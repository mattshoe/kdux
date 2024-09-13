package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mattsho.shoebox.devtools.common.*
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.UserCommand
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Command
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.ServerMessage
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Synchronized
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

typealias RequestId = String
typealias SessionId = String
typealias StoreName = String

data class RegistrationChange(
    val value: Registration,
    val removed: Boolean = false
)

class DevToolsServerImpl : DevToolsServer {
    private var coroutineScope = buildCoroutineScope()
    private val userCommandStream = MutableSharedFlow<UserCommand>()
    private val _registrationStream = MutableSharedFlow<RegistrationChange>()
    private val _dispatchRequestStream = MutableSharedFlow<DispatchRequest>()
    private val _dispatchResultStream = MutableSharedFlow<DispatchResult>()
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
        userCommandStream
            .onEach { command ->
                val requestId = requestMap.access {
                    it.remove(command.storeName)
                }
                if (requestId != null) {
                    val session = storeMap.access {
                        it[command.storeName]
                    }
                    session?.sendMessage(
                        requestId,
                        Json.encodeToString(command.payload)
                    )
                } else {
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

    override fun send(userCommand: UserCommand) {
        coroutineScope.launch {
            userCommandStream.emit(userCommand)
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
                    } finally {
                        closeReason.await()
                        val registration = sessionMap.access { map ->
                            map.remove(sessionId.toString())
                        }
                        registration?.let { reg ->
                            storeMap.access { it.remove(reg.storeName) }
                            _registrationStream.emit(
                                RegistrationChange(
                                    value = registration,
                                    removed = true
                                )
                            )
                        }
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
        _registrationStream.emit(
            RegistrationChange(registration, false)
        )
    }

    private suspend fun WebSocketSession.dispatchRequest(serverRequest: ServerRequest, sessionId: UUID) {
        val dispatchRequest = Json.decodeFromString<DispatchRequest>(serverRequest.data)
        if (storeUnderDebug != null && storeUnderDebug == dispatchRequest.storeName) {
            requestMap.update {
                it[dispatchRequest.storeName] = dispatchRequest.dispatchId
            }
            _dispatchRequestStream.emit(dispatchRequest)
        } else {
            sendMessage(
                id = dispatchRequest.dispatchId,
                text = Json.encodeToString(Command("continue"))
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
                        responseCorrelationId = id,
                        data = text
                    )
                )
            )
        )
    }

    private fun buildCoroutineScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
