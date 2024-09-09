package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class DevToolsServerImpl : DevToolsServer {
    private var coroutineScope = buildCoroutineScope()
    private val commandStream = MutableSharedFlow<Command>()
    private val _dataStream = MutableSharedFlow<DispatchRequest>(
        replay = 0,
        extraBufferCapacity = 10000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val closeCompletable = CompletableDeferred<Unit>()
    private val isStarted = AtomicBoolean(false)
    private val exceptionHandler = CoroutineExceptionHandler { _, error ->
        println("KDUX DEVTOOLS:ERROR --> \n\t${error.stackTrace.joinToString("\n\t") { it.toString() }}")
    }

    override val data = _dataStream.asSharedFlow()

    init {
        coroutineScope.launch {
            repeat(30) {
                delay(500)

                _dataStream.emit(
                    DispatchRequest(
                        UUID.randomUUID().toString(),
                        "testStore",
                        State("derp", "$it"),
                        Action("foo", "$it"),
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    )
                )
            }
        }
    }

    override fun send(command: Command) {
        coroutineScope.launch {
            commandStream.emit(command)
        }
    }

    override fun start() {
        coroutineScope.launch(Dispatchers.IO + exceptionHandler) {
            if (!isStarted.getAndSet(true)) {
                val connection = embeddedServer(
                    factory = Netty,
                    host = "localhost",
                    port = 9001
                ) {
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(15)
                    }

                    routing {
                        webSocket("/store") {
                            send("WebSocket Server Started. Ready to receive messages.")
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    try {
                                        // Deserialize the message
                                        val kduxDispatchRequest = Json.decodeFromString<DispatchRequest>(text)
                                        println("Received Store Data: ${kduxDispatchRequest.storeName}")

                                        _dataStream.emit(kduxDispatchRequest)

                                        // Echo the message back to the client (optional)
                                        send(Frame.Text("Server received: ${kduxDispatchRequest.storeName}"))
                                    } catch (e: Exception) {
                                        println("Error: ${e.message}")
                                    }
                                }
                            }

                            commandStream
                                .onEach {
                                    sendSerialized(it.payload)
                                }.launchIn(coroutineScope)

                            closeCompletable.await()
                            close(CloseReason(CloseReason.Codes.NORMAL, "All done"))
                        }
                    }
                }.apply {
                    start(wait = false)
                }
                closeCompletable.await()
                connection.stop(0L)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
        closeCompletable.complete(Unit)
        coroutineScope = buildCoroutineScope()
        isStarted.set(false)
    }

    private fun buildCoroutineScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}