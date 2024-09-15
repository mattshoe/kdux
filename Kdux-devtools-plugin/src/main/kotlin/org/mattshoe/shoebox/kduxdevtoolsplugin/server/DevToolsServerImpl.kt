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
import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattsho.shoebox.devtools.common.DispatchResult
import org.mattsho.shoebox.devtools.common.ServerRequest
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.*
import java.util.*

typealias RequestId = String
typealias SessionId = String
typealias StoreName = String

class DevToolsServerImpl : DevToolsServer {
    private var coroutineScope = buildCoroutineScope()
    private val userCommandStream = MutableSharedFlow<UserCommand>()
    private val _registrationStream = MutableSharedFlow<RegistrationChange>()
    private val _dispatchResultStream = MutableSharedFlow<DispatchResult>()
    private val server: ApplicationEngine = buildServer()
    private val storeMap = Synchronized(mutableMapOf<StoreName, WebSocketSession>())
    private val sessionMap = Synchronized(mutableMapOf<SessionId, Registration>())
    private val requestMap = Synchronized(mutableMapOf<StoreName, RequestId>())
    private val _debugState = MutableStateFlow<DebugState>(DebugState.NotDebugging)
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    private val _currentStateMap = Synchronized(mutableMapOf<StoreName, CurrentState>())

    override val dispatchResultStream = _dispatchResultStream.asSharedFlow()
    override val registrationStream = _registrationStream.asSharedFlow()
    override val serverState = _serverState.asStateFlow()
    override val debugState = _debugState.asStateFlow()

    init {
        start()
        userCommandStream
            .onEach { command ->
                try {
                    when (command) {
                        is UserCommand.Continue,
                        is UserCommand.Pause,
                        is UserCommand.NextDispatch -> {
                            val requestId = requestMap.access {
                                it.remove(command.storeName)
                            }
                            println("")
                            if (requestId != null) {
                                val session = storeMap.access {
                                    it[command.storeName]
                                }
                                println("Sending Command --> $command")
                                session?.sendMessage(
                                    requestId,
                                    Json.encodeToString(command.payload)
                                ) ?: println("Dropped Command for request!")
                            } else {
                                storeMap.access {
                                    it[command.storeName]
                                }?.sendMessage(
                                    null,
                                    Json.encodeToString(command.payload)
                                ) ?: println("Dropped command!!")
                            }
                        }
                        else -> {
                            storeMap.access {
                                it[command.storeName]
                            }?.sendMessage(
                                null,
                                Json.encodeToString(command.payload)
                            )
                        }

                    }
                } catch (e: Throwable) {
                    println("Error processing command --> $e")
                }
            }
            .catch {
                println(it)
            }
            .launchIn(coroutineScope)
    }

    override fun execute(intent: ServerIntent) {
        coroutineScope.launch {
            when (intent) {
                is ServerIntent.StartServer -> start()
                is ServerIntent.StopServer -> stop()
                is ServerIntent.Command -> userCommandStream.emit(intent.command)
                is ServerIntent.StartDebugging -> _debugState.emit(
                    DebugState.ActivelyDebugging(
                        intent.storeName,
                        _currentStateMap.access { it[intent.storeName] }
                    )
                )
                is ServerIntent.PauseDebugging -> {
                    _debugState.emit(
                        DebugState.DebuggingPaused(
                            intent.storeName,
                            _currentStateMap.access { it[intent.storeName] }
                        )
                    )
                    userCommandStream.emit(UserCommand.Pause(intent.storeName))
                }
                is ServerIntent.StopDebugging -> {
                    println("_debugState.emit(DebugState.NotDebugging)")
                    userCommandStream.emit(UserCommand.Continue(intent.storeName))
                    _debugState.emit(DebugState.NotDebugging)
                }
            }
        }
    }

    private fun start() {
        if (_serverState.value != ServerState.Started) {
            server.start()
        }
    }

    private fun stop() {
        coroutineScope.cancel()
        server.stop()
        coroutineScope = buildCoroutineScope()
        _serverState.update { ServerState.Stopped }
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
                                else -> throw UnsupportedOperationException("DevTools Debug Server only supports Text Frames.")
                            }
                        }
                    } catch (ex: Throwable) {
                        this.send(Frame.Close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, ex.message ?: "UNKNOWN")))
                    } finally {
                        val closeReason = closeReason.await()
                        println("Close Reason received --> $closeReason")
                        val registration = sessionMap.access { map ->
                            map.remove(sessionId.toString())
                        }
                        println("Closing registration for --> $registration")
                        registration?.let { reg ->
                            val session = storeMap.access { it.remove(reg.storeName) }
                            println("Session cleared --> $session")
                            println("Emitting Registration Removal")
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
                ServerRequest.Type.CURRENT_STATE -> currentState(serverRequest, sessionId)
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    private suspend fun WebSocketSession.register(request: ServerRequest, sessionId: UUID) {
        val registration = Json.decodeFromString<Registration>(request.data)

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
        val currentDebugState = _debugState.value
        when (currentDebugState) {
            is DebugState.NotDebugging,
            is DebugState.DebuggingPaused -> {
                println("Ignoring dispatch request --> $currentDebugState")
                sendMessage(
                    id = dispatchRequest.dispatchId,
                    text = Json.encodeToString(Command("continue"))
                )
            }

            is DebugState.ActivelyDebugging -> {
                val storeUnderDebug = currentDebugState.storeName
                if (storeUnderDebug == dispatchRequest.storeName) {
                    requestMap.update {
                        it[dispatchRequest.storeName] = dispatchRequest.dispatchId
                    }
                    _debugState.emit(
                        DebugState.ActivelyDebugging(
                            storeName = dispatchRequest.storeName,
                            currentState = _currentStateMap.access { it[dispatchRequest.storeName] },
                            dispatchRequest = dispatchRequest
                        )
                    )
                } else {
                    println("Wrong store, ignoring dispatch request")
                    sendMessage(
                        id = dispatchRequest.dispatchId,
                        text = Json.encodeToString(Command("continue"))
                    )
                }
            }
        }

    }

    private suspend fun WebSocketSession.dispatchResult(serverRequest: ServerRequest, sessionId: UUID) {
        val dispatchResult = Json.decodeFromString<DispatchResult>(serverRequest.data)
        _dispatchResultStream.emit(dispatchResult)
    }

    private suspend fun currentState(serverRequest: ServerRequest, sessionId: UUID) {
        println("Received Current State Report --> $serverRequest")
        val currentState = Json.decodeFromString<CurrentState>(serverRequest.data)
        _currentStateMap.access {
            it[currentState.storeName] = currentState
        }
        val currentDebugState = _debugState.value
        if (currentDebugState.storeUnderDebug == currentState.storeName) {
            _debugState.update {
                currentDebugState.updateCurrentState(currentState)
            }
        }
    }

    private suspend fun WebSocketSession.sendMessage(id: String?, text: String) {
        try {
            val message = Json.encodeToString(
                ServerMessage(
                    responseCorrelationId = id,
                    data = text
                )
            )
            this.send(Frame.Text(message))
        } catch (e: Throwable) {
            println(e)
        }
    }

    private fun buildCoroutineScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun DebugState.updateCurrentState(currentState: CurrentState?): DebugState {
        return if (this is DebugState.ActivelyDebugging) {
            copy(currentState = currentState)
        } else if (this is DebugState.DebuggingPaused) {
            copy(currentState = currentState)
        } else this
    }

    private val DebugState.storeUnderDebug: String?
        get() = when (this) {
            is DebugState.NotDebugging -> null
            is DebugState.DebuggingPaused -> this.storeName
            is DebugState.ActivelyDebugging ->  this.storeName
        }
}
