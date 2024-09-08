package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class DevToolsServerImpl : DevToolsServer {
    private var coroutineScope = buildCoroutineScope()
    private val commandStream = MutableSharedFlow<Command>()
    private val _dataStream = MutableSharedFlow<StoreData>()
    private val closeCompletable = CompletableDeferred<Unit>()
    private val isStarted = AtomicBoolean(false)

    override val data = _dataStream.asSharedFlow()

    override fun send(command: Command) {
        coroutineScope.launch {
            commandStream.emit(command)
        }
    }

    override fun start() {
        if (!isStarted.getAndSet(true)) {
            embeddedServer(
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
                                    val storeData = Json.decodeFromString<StoreData>(text)
                                    println("Received Store Data: ${storeData.name}")

                                    _dataStream.emit(storeData)

                                    // Echo the message back to the client (optional)
                                    send(Frame.Text("Server received: ${storeData.name}"))
                                } catch (e: Exception) {
                                    println("Error: ${e.message}")
                                }
                            }
                        }

                        commandStream
                            .onEach {
                                sendSerialized(it)
                            }.launchIn(coroutineScope)

                        closeCompletable.await()
                        close(CloseReason(CloseReason.Codes.NORMAL, "All done"))
                    }
                }
            }.start(wait = false)
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