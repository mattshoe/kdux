package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.gradle.internal.impldep.org.h2.engine.User
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.DebugState
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.DevToolsServer
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.DevToolsServerImpl
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.ServerIntent
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.TimeStamper
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.UserCommand
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalSerializationApi::class)
class DevToolsViewModel(
    private val server: DevToolsServer = DevToolsServerImpl()
): ViewModel<UiState, UserIntent> {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow<UiState>(UiState.DebuggingStopped)
    private val _dispatchStream = MutableStateFlow<List<DispatchLog>>(emptyList())
    private val dispatchLog = mutableListOf<DispatchLog>()
    private val dispatchLogMutex = Mutex()
    private val _registeredStores = mutableListOf<Registration>()
    private val _registrationStream = MutableStateFlow(emptyList<Registration>())
    private val registrationMutex = Mutex()
    private val prettyJson = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }


    override val state = _uiState.asStateFlow()
    val registrationStream: Flow<List<Registration>> = _registrationStream.asStateFlow()
    val dispatchLogStream = _dispatchStream.asStateFlow()

    init {
        server.debugState
            .onEach { serverDebugState ->
                try {
                    when (serverDebugState) {
                        is DebugState.ActivelyDebugging ->  {
                            val prettyState = if (serverDebugState.currentState != null) {
                                serverDebugState.currentState.copy(
                                    state = serverDebugState.currentState.state.copy(
                                        json = prettyPrintJson(serverDebugState.currentState.state.json)
                                    )
                                )
                            } else null
                            _uiState.emit(
                                UiState.Debugging(
                                    serverDebugState.storeName,
                                    prettyState,
                                    serverDebugState.dispatchRequest?.copy(
                                        action = serverDebugState.dispatchRequest.action.copy(
                                            json = prettyPrintJson(serverDebugState.dispatchRequest.action.json)
                                        )
                                    )
                                )
                            )
                        }
                        is DebugState.DebuggingPaused -> {
                            val prettyState = if (serverDebugState.currentState != null) {
                                serverDebugState.currentState.copy(
                                    state = serverDebugState.currentState.state.copy(
                                        json = prettyPrintJson(serverDebugState.currentState.state.json)
                                    )
                                )
                            } else null
                            _uiState.emit(
                                UiState.DebuggingPaused(
                                    serverDebugState.storeName,
                                    prettyState
                                )
                            )
                        }
                        is DebugState.NotDebugging -> _uiState.emit(
                            UiState.DebuggingStopped
                        )
                    }
                } catch (e: Throwable) {
                    println("Error processing ServerDebugState! --> $e")
                }
            }
            .catch {
                println("Server debug state collection killed :( --> $it ")
            }.launchIn(coroutineScope)

        server.dispatchResultStream
            .onEach {
                dispatchLogMutex.withLock {
                    dispatchLog.add(
                        0,
                        DispatchLog(
                            it.copy(
                                request = it.request.copy(
                                    action = it.request.action.copy(
                                        json = prettyPrintJson(it.action.json)
                                    ),
                                    currentState = it.request.currentState.copy(
                                        json = prettyPrintJson(it.request.currentState.json)
                                    )
                                ),
                                action = it.action.copy(
                                    json = prettyPrintJson(it.action.json)
                                ),
                                newState = it.newState.copy(
                                    json = prettyPrintJson(it.newState.json)
                                ),
                                timestamp = TimeStamper.pretty(it.timestamp)
                            )
                        )
                    )
                    _dispatchStream.update {
                        try {
                            dispatchLog.toList().sortedBy {
                                it.result.timestamp
                            }.reversed().take(500)
                        } catch (e: Throwable) {
                            println(e)
                            emptyList()
                        }
                    }
                }
            }.launchIn(coroutineScope)

        server.registrationStream
            .onEach { registrationChange ->
                registrationMutex.withLock {
                    if (registrationChange.removed) {
                        _registeredStores.remove(registrationChange.value)
                    } else {
                        _registeredStores.add(registrationChange.value)
                    }
                    _registrationStream.update {
                        _registeredStores.toList()
                    }
                }
            }.launchIn(coroutineScope)
    }

    override fun handleIntent(intent: UserIntent) {
        coroutineScope.launch {
            when (intent) {
                is UserIntent.StartDebugging -> {
                    server.execute(ServerIntent.StartDebugging(intent.storeName))
                    _uiState.update {
                        UiState.Debugging(intent.storeName)
                    }
                }
                is UserIntent.StopDebugging -> {
                    println("server.execute(ServerIntent.StopDebugging)")
                    server.execute(ServerIntent.StopDebugging(intent.storeName))
                    _uiState.update { UiState.DebuggingStopped }
                }
                is UserIntent.PauseDebugging -> {
                    server.execute(ServerIntent.PauseDebugging(intent.storeName))
                    _uiState.update {
                        UiState.DebuggingPaused(intent.storeName)
                    }
                }
                is UserIntent.StepOver -> {
                    server.execute(
                        ServerIntent.Command(
                            UserCommand.NextDispatch(intent.storeName)
                        )
                    )
                    with (_uiState.value as? UiState.Debugging) {
                        this?.let {
                            _uiState.update {
                                copy(dispatchRequest = null)
                            }
                        }
                    }
                }
                is UserIntent.StepBack -> {
                    server.execute(
                        ServerIntent.Command(
                            UserCommand.PreviousDispatch(intent.storeName)
                        )
                    )
                }
                is UserIntent.RestoreState -> {
                    server.execute(
                        ServerIntent.Command(
                            UserCommand.RestoreState(intent.dispatch)
                        )
                    )
                }
                is UserIntent.ReplayAction -> {
                    server.execute(
                        ServerIntent.Command(
                            UserCommand.ReplayAction(intent.dispatch)
                        )
                    )
                }
                is UserIntent.ReplayDispatch -> {
                    server.execute(
                        ServerIntent.Command(
                            UserCommand.ReplayDispatch(intent.dispatch)
                        )
                    )
                }
                is UserIntent.DispatchOverride -> {
                    server.execute(
                        ServerIntent.Command(
                            UserCommand.DispatchOverride(intent.storeName, Json.decodeFromString(intent.text))
                        )
                    )
                }
                is UserIntent.ClearLogs -> {
                    dispatchLogMutex.withLock {
                        dispatchLog.clear()
                        _dispatchStream.emit(dispatchLog)
                    }
                }
            }
        }
    }

    override fun dispose() {
        coroutineScope.cancel()
    }

    private fun prettyPrintJson(jsonText: String): String {
        val jsonObject = prettyJson.parseToJsonElement(jsonText) as JsonObject
        return prettyJson.encodeToString(JsonObject.serializer(), jsonObject)
    }

}