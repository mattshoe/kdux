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
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.CommandPayload
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.ServerMessage
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration

internal class KtorDebugSession(
    id: String,
    httpClient: HttpClient,
    private val coroutineScope: CoroutineScope
): DebugSession {
    private lateinit var socket: WebSocketSession
    private val socketInitialized = CompletableDeferred<Boolean>()
    private val _adHocCommands = MutableSharedFlow<CommandPayload>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val responseMap = mutableMapOf<String, CompletableDeferred<CommandPayload>>()
    private val responseMutex = Mutex()

    override val adHocCommands = _adHocCommands.asSharedFlow()

    init {
        coroutineScope.launch {
            log("Initializing socket")
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

            log("Sending registration: $id")
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
            log("Consuming responses")
            socket.incoming.consumeAsFlow()
                .filter {
                    log("Response Received --> $it")
                    it is Frame.Text
                }
                .map {
                    it as Frame.Text
                }
                .onEach {
                    val serverMessage = Json.decodeFromString<ServerMessage>(it.readText())
                    log("Message Parsed --> $serverMessage")
                    val command = Json.decodeFromString<CommandPayload>(serverMessage.data)
                    if (serverMessage.id != null) {
                        log("Message is a response!")
                        val completable = responseMutex.withLock {
                            responseMap[serverMessage.id]
                        }
                        if (completable == null) {
                            log("Emitting AdHoc command --> $command")
                            _adHocCommands.emit(command)
                        } else {
                            log("Completing Response --> $command")
                            log("Completable --> $completable")
                            completable.complete(command)
                            log("Response completed")
                        }
                    } else {
                        log("Emitting AdHoc command -> $command")
                        _adHocCommands.emit(command)
                    }
                }
                .catch {
                    log("Response failure --> $it")
                }
                .launchIn(this)
            log("Completing Initialization")
            socketInitialized.complete(true)
            log("Completed Initialization")
        }
    }

    override suspend fun send(serverRequest: ServerRequest) {
        log("Sending ServerRequest! --> $serverRequest")
        if (socketInitialized.await()) {
            socket.send(
                Frame.Text(
                    Json.encodeToString(serverRequest)
                )
            )
        }
    }

    override suspend fun awaitResponse(serverRequest: ServerRequest): CommandPayload {
        log("awaitResponse(serverRequest = $serverRequest)")
        val completableDeferred = CompletableDeferred<CommandPayload>()

        serverRequest.id?.let {
            responseMutex.withLock {
                responseMap[it] = completableDeferred
            }
        }
        send(serverRequest)

        return if (socketInitialized.await()) {
            completableDeferred.await()
        } else {
            CommandPayload("continue")
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

fun log(msg: Any?) {
//    println(msg.toString())
}