package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattsho.shoebox.devtools.common.DispatchResult
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.*
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.CurrentState
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.UserCommand
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.TimeStamper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DispatchLog(
    val result: DispatchResult
)

interface ViewModel<State: Any, UserIntent: Any> {
    val state: Flow<State>
    fun handleIntent(intent: UserIntent)
    fun dispose()
}

sealed interface UiState {
    data object DebuggingStopped: UiState
    data class Debugging(
        val storeName: String,
        val currentState: CurrentState? = null,
        val dispatchRequest: DispatchRequest? = null
    ): UiState
    data class DebuggingPaused(
        val storeName: String,
        val currentState: CurrentState? = null
    ): UiState
}

sealed interface UserIntent {
    data class StopDebugging(val storeName: String): UserIntent
    data class StartDebugging(val storeName: String): UserIntent
    data class PauseDebugging(val storeName: String): UserIntent
    data class StepOver(val storeName: String): UserIntent
    data class StepBack(val storeName: String): UserIntent
    data class ReplayDispatch(val storeName: String, val dispatchId: String): UserIntent
    data class DispatchOverride(val storeName: String, val text: String): UserIntent
}

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
                println("VM received ServerDebugState --> $serverDebugState")
                when (serverDebugState) {
                    is DebugState.ActivelyDebugging -> _uiState.emit(
                        UiState.Debugging(
                            serverDebugState.storeName,
                            serverDebugState.currentState,
                            serverDebugState.dispatchRequest?.copy(
                                currentState = serverDebugState.dispatchRequest.currentState.copy(
                                    json = prettyPrintJson(serverDebugState.dispatchRequest.currentState.json)
                                ),
                                action = serverDebugState.dispatchRequest.action.copy(
                                    json = prettyPrintJson(serverDebugState.dispatchRequest.action.json)
                                )
                            )
                        )
                    )
                    is DebugState.DebuggingPaused -> _uiState.emit(
                        UiState.DebuggingPaused(
                            serverDebugState.storeName,
                            serverDebugState.currentState
                        )
                    )
                    is DebugState.NotDebugging -> _uiState.emit(
                        UiState.DebuggingStopped
                    )
                }
            }.launchIn(coroutineScope)

        state.onEach {
            println("Emitting State --> $it")
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
                            dispatchLog.sortedBy {
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
                        _registeredStores
                    }
                }
            }.launchIn(coroutineScope)
    }

    override fun handleIntent(intent: UserIntent) {
        coroutineScope.launch {
            println("Handling Intent --> $intent")
            when (intent) {
                is UserIntent.StartDebugging -> {
                    server.execute(ServerIntent.StartDebugging(intent.storeName))
                    _uiState.update {
                        UiState.Debugging(intent.storeName)
                    }
                }
                is UserIntent.StopDebugging -> {
                    server.execute(ServerIntent.StopDebugging)
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
                            UserCommand.Continue(intent.storeName)
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
                is UserIntent.ReplayDispatch -> {
                    server.execute(
                        ServerIntent.Command(
                            UserCommand.PreviousDispatch(intent.storeName)
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

    private fun convertToHumanReadable(isoTimestamp: String): String {
        return try {
            val instant = Instant.parse(isoTimestamp)
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault()) // Adjusts to system's local timezone
            formatter.format(instant)
        } catch (ex: Throwable) {
            println(ex)
            "--"
        }
    }

}