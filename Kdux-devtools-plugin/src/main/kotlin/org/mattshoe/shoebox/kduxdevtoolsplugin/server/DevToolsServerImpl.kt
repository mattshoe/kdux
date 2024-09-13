package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mattsho.shoebox.devtools.common.*
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Command
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.CommandPayload
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.ServerMessage
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Synchronized
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private data class Session(
    val id: UUID,
    val storeName: String,
    val socket: WebSocketSession
)

typealias RequestId = String
typealias SessionId = String
typealias StoreName = String


class DevToolsServerImpl : DevToolsServer {
    private var coroutineScope = buildCoroutineScope()
    private val commandStream = MutableSharedFlow<Command>()
    private val _registrationStream = MutableSharedFlow<Registration>(
        replay = 0,
        extraBufferCapacity = 10000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val _dispatchRequestStream = MutableSharedFlow<DispatchRequest>(
        replay = 0,
        extraBufferCapacity = 10000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val _dispatchResultStream = MutableSharedFlow<DispatchResult>(
        replay = 0,
        extraBufferCapacity = 1000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val isStarted = AtomicBoolean(false)
    private val server: ApplicationEngine = buildServer()
    private val storeMap = Synchronized(mutableMapOf<StoreName, WebSocketSession>())
    private val sessionMap = Synchronized(mutableMapOf<SessionId, Registration>())
    private val requestMap = Synchronized(mutableMapOf<StoreName, RequestId>())
    private var storeUnderDebug: String? = null

    override val dispatchRequestStream = _dispatchRequestStream.asSharedFlow()
    override val dispatchResultStream = _dispatchResultStream.asSharedFlow()
    override val registrationStream = _registrationStream.asSharedFlow()

    init {
        start()
        commandStream
            .onEach { command ->
                println("Command Issued --> $command")
                println("Requests --> \n\t${requestMap.access { map -> map.keys.joinToString("\n\t") { "$it: ${map[it]}" } }}")
                val requestId = requestMap.access {
                    it.remove(command.storeName)
                }
                if (requestId != null) {
                    println("Request ID --> $requestId")
                    val session = storeMap.access {
                        it[command.storeName]
                    }
                    println("Session --> $session")
                    session?.sendMessage(
                        requestId,
                        Json.encodeToString(command.payload)
                    )
                } else {
                    println("No Request id! Sending message")
                    storeMap.access {
                        it[command.storeName]
                    }?.sendMessage(
                        null,
                        Json.encodeToString(command.payload)
                    )
                }
            }
            .catch {
                println(it)
            }
            .launchIn(coroutineScope)
    }

    override fun send(command: Command) {
        coroutineScope.launch {
            commandStream.emit(command)
        }
    }

    override fun debug(storeName: String?) {
        storeUnderDebug = storeName
    }

    override fun start() {
        if (!isStarted.getAndSet(true)) {
            server.start()
        }
    }

    override fun stop() {
        coroutineScope.cancel()
        server.stop()
        coroutineScope = buildCoroutineScope()
        isStarted.set(false)
    }

    private fun buildServer(): ApplicationEngine {
        return embeddedServer(
            factory = Netty,
            host = "localhost",
            port = 9001
        ) {
            install(WebSockets)

            routing {
                webSocket("/debug") {
                    val sessionId = UUID.randomUUID()
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> handleTextFrame(frame, sessionId)
                                is Frame.Close -> handleFrameClose(frame, sessionId)
                                else -> Unit
                            }
                        }
                    } catch (ex: Throwable) {
                        this.send(Frame.Close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, ex.message ?: "UNKNOWN")))
                    }
                }
            }
        }
    }

    private suspend fun WebSocketSession.handleTextFrame(frame: Frame.Text, sessionId: UUID) {
        try {
            val text = frame.readText()
            val serverRequest = Json.decodeFromString<ServerRequest>(text)

            when (serverRequest.type) {
                ServerRequest.Type.REGISTRATION -> register(serverRequest, sessionId)
                ServerRequest.Type.DISPATCH_REQUEST -> dispatchRequest(serverRequest, sessionId)
                ServerRequest.Type.DISPATCH_RESULT -> dispatchResult(serverRequest, sessionId)
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    private suspend fun WebSocketSession.register(request: ServerRequest, sessionId: UUID) {
        val registration = Json.decodeFromString<Registration>(request.data) // Receive the incoming request as a Kotlin data class

        storeMap.update {
            it[registration.storeName] = this
        }
        sessionMap.update {
            it[sessionId.toString()] = registration
        }
        _registrationStream.emit(registration)
    }

    private suspend fun WebSocketSession.dispatchRequest(serverRequest: ServerRequest, sessionId: UUID) {
        val dispatchRequest = Json.decodeFromString<DispatchRequest>(serverRequest.data)
        log("Dispatch Request -> $dispatchRequest")
        if (storeUnderDebug != null && storeUnderDebug == dispatchRequest.storeName) {
            requestMap.update {
                it[dispatchRequest.storeName] = dispatchRequest.dispatchId
            }
            _dispatchRequestStream.emit(dispatchRequest)
        } else {
            sendMessage(
                id = dispatchRequest.dispatchId,
                text = Json.encodeToString(CommandPayload("continue"))
            )
        }
    }

    private suspend fun WebSocketSession.dispatchResult(serverRequest: ServerRequest, sessionId: UUID) {
        val dispatchResult = Json.decodeFromString<DispatchResult>(serverRequest.data)
        _dispatchResultStream.emit(dispatchResult)
    }

    private suspend fun handleFrameClose(frame: Frame.Close, sessionId: UUID) {
        val registration = sessionMap.access {
            it.remove(sessionId.toString())
        }
        storeMap.update {
            it.remove(registration?.storeName)
        }
    }

    private suspend fun WebSocketSession.sendMessage(id: String?, text: String) {
        this.send(
            Frame.Text(
                Json.encodeToString(
                    ServerMessage(
                        id = id,
                        data = text
                    )
                )
            )
        )
    }

    private fun buildCoroutineScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

fun log(msg: Any?) {
//    println(msg.toString())
}