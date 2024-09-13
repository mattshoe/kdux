package org.mattshoe.shoebox.devtools.server

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mattsho.shoebox.devtools.common.ServerRequest
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Command
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.ServerMessage
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration

internal class ClientDebugSessionImpl(
    id: String,
    httpClient: HttpClient,
    private val coroutineScope: CoroutineScope
): ClientDebugSession {
    private lateinit var socket: WebSocketSession
    private val socketInitialized = CompletableDeferred<Boolean>()
    private val _adHocCommands = MutableSharedFlow<Command>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val responseMap = mutableMapOf<String, CompletableDeferred<Command>>()
    private val responseMutex = Mutex()

    override val adHocCommands = _adHocCommands.asSharedFlow()

    init {
        coroutineScope.launch {
            try {
                socket = httpClient.webSocketSession(
                    HttpMethod.Get,
                    host = "localhost",
                    port = 9001,
                    path = "/debug"
                ) {
                    timeout {
                        connectTimeoutMillis = 3000
                        socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                        requestTimeoutMillis = 5000
                    }
                }
            } catch (ex: Throwable) {
                println(ex)
                socketInitialized.complete(false)
                return@launch
            }

            socket.outgoing.send(
                Frame.Text(
                    Json.encodeToString(
                        ServerRequest(
                            type = ServerRequest.Type.REGISTRATION,
                            data = Json.encodeToString(Registration(id))
                        )
                    )
                )
            )

            socket.incoming.consumeAsFlow()
                .filter {
                    it is Frame.Text
                }
                .map {
                    it as Frame.Text
                }
                .onEach {
                    try {
                        val serverMessage = Json.decodeFromString<ServerMessage>(it.readText())
                        val command = Json.decodeFromString<Command>(serverMessage.data)
                        if (serverMessage.id != null) {
                            val completable = responseMutex.withLock {
                                responseMap[serverMessage.id]
                            }
                            if (completable == null) {
                                _adHocCommands.emit(command)
                            } else {
                                completable.complete(command)
                            }
                        } else {
                            _adHocCommands.emit(command)
                        }
                    } catch (e: Throwable) {
                        println("Response failure --> $it")
                    }
                }
                .catch {
                    println("Response failure --> $it")
                }
                .launchIn(this)

            socketInitialized.complete(true)
        }
    }

    override suspend fun send(serverRequest: ServerRequest) {
        if (socketInitialized.await()) {
            socket.send(
                Frame.Text(
                    Json.encodeToString(serverRequest)
                )
            )
        }
    }

    override suspend fun awaitResponse(serverRequest: ServerRequest): Command {
        val completableDeferred = CompletableDeferred<Command>()

        serverRequest.id?.let {
            responseMutex.withLock {
                responseMap[it] = completableDeferred
            }
        }
        send(serverRequest)

        return if (socketInitialized.await()) {
            completableDeferred.await()
        } else {
            Command("continue")
        }
    }

    override suspend fun closeSession() {
        if (socketInitialized.await()) {
            socket.send(
                Frame.Close(
                    CloseReason(CloseReason.Codes.NORMAL, "Kdux DevTools debug session ended.")
                )
            )
        }
    }

}
