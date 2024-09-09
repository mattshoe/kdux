package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mattsho.shoebox.devtools.common.*
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Command
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

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
        extraBufferCapacity = 10000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val isStarted = AtomicBoolean(false)
    private val server: ApplicationEngine = buildServer()
    private val sessionMap = mutableMapOf<String, WebSocketSession>()
    private val sessionMutex = Mutex()

    override val dispatchRequestStream = _dispatchRequestStream.asSharedFlow()
    override val dispatchResultStream = _dispatchResultStream.asSharedFlow()
    override val registrationStream = _registrationStream.asSharedFlow()

    init {
        coroutineScope.launch {
            repeat(30) {
                delay(500)

                _dispatchRequestStream.emit(
                    DispatchRequest(
                        UUID.randomUUID().toString(),
                        "testStore",
                        State("derp", "$it"),
                        Action("foo", "$it"),
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    )
                )

                _registrationStream.emit(
                    Registration(
                        "Store #${it}"
                    )
                )
            }
        }
        start()

        commandStream
            .onEach { command ->
                sessionMutex.withLock {
                    sessionMap[command.storeName]?.send(
                        Json.encodeToString(command.payload)
                    )
                }
            }.launchIn(coroutineScope)
    }

    override fun send(command: Command) {
        coroutineScope.launch {
            commandStream.emit(command)
        }
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
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
            }

            routing {

                webSocket("/debug") {

                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            try {
                                val debugRequest = Json.decodeFromString<DebugRequest>(text)
                                when (debugRequest.type) {
                                    DebugRequest.Type.REGISTRATION -> register(debugRequest.data)
                                    DebugRequest.Type.DISPATCH_REQUEST -> dispatchRequest(debugRequest.data)
                                    DebugRequest.Type.DISPATCH_RESULT -> dispatchResult(debugRequest.data)
                                }
                            } catch (e: Exception) {
                                println("Error: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun WebSocketSession.register(data: String) {
        val request = Json.decodeFromString<Registration>(data) // Receive the incoming request as a Kotlin data class
        sessionMutex.withLock {
            sessionMap[request.storeName] = this
        }
        _registrationStream.emit(request)
    }

    private suspend fun  WebSocketSession.dispatchRequest(data: String) {
        // Deserialize the message
        val kduxDispatchRequest = Json.decodeFromString<DispatchRequest>(data)
        println("Received Store Data: ${kduxDispatchRequest.storeName}")

        _dispatchRequestStream.emit(kduxDispatchRequest)
    }

    private suspend fun  WebSocketSession.dispatchResult(data: String) {
        // Deserialize the message
        val kduxDispatchResult = Json.decodeFromString<DispatchResult>(data)
        println("Received Dispatch Result: ${kduxDispatchResult}")

        _dispatchResultStream.emit(kduxDispatchResult)
    }

    private fun buildCoroutineScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}