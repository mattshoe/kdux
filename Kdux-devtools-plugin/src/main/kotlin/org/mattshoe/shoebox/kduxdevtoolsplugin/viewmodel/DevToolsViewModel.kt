package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

import com.android.tools.idea.wizard.template.impl.activities.viewModelActivity.src.app_package.viewModelKt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattsho.shoebox.devtools.common.DispatchResult
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.*
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Command
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
}

sealed interface State {
    data object Stopped: State
    data class Debugging(val storeName: String): State
    data class Paused(val storeName: String): State
}

sealed interface UserIntent {
    data class StopDebugging(val storeName: String): UserIntent
    data class StartDebugging(val storeName: String): UserIntent
    data class PauseDebugging(val storeName: String): UserIntent
    data class NextDispatch(val storeName: String): UserIntent
    data class PreviousDispatch(val storeName: String): UserIntent
    data class ReplayDispatch(val storeName: String, val dispatchId: String): UserIntent
    data class DispatchOverride(val storeName: String, val text: String): UserIntent
}

@OptIn(ExperimentalSerializationApi::class)
class DevToolsViewModel(
    private val server: DevToolsServer = DevToolsServerImpl()
): ViewModel<State, UserIntent> {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<State>(State.Stopped)
    private val _dispatchStream = MutableStateFlow<List<DispatchLog>>(emptyList())
    private val dispatchLog = mutableListOf<DispatchLog>()
    private val dispatchLogMutex = Mutex()
    private val _registeredStores = mutableListOf<Registration>()
    private val _registrationStream = MutableStateFlow(emptyList<Registration>())
    private val _debugStream = MutableSharedFlow<DispatchRequest?>(replay = 1)
    private val registrationMutex = Mutex()
    private val prettyJson = Json {
        prettyPrint = true
        prettyPrintIndent = "    "
    }


    override val state = _state.asStateFlow()
    val dispatchStream: Flow<List<DispatchLog>> = _dispatchStream.asSharedFlow()
    val registrationStream: Flow<List<Registration>> = _registrationStream.asStateFlow()
    val debugStream: Flow<DispatchRequest?> = _debugStream.asSharedFlow()

    init {
        server.dispatchRequestStream
            .onEach {
                println("Dispatch Request! --> $it")
                _debugStream.emit(
                    it.copy(
                        currentState = it.currentState.copy(
                            json = prettyPrintJson(it.currentState.json)
                        ),
                        action = it.action.copy(
                            json = prettyPrintJson(it.action.json)
                        )
                    ).also { copy ->
                        println("Copied Request --> $copy")
                    }
                )
            }.launchIn(coroutineScope)

        server.dispatchResultStream
            .onEach {
                println("Dispatch Log --> $it")
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
                        dispatchLog.toList().sortedBy {
                            it.result.timestamp
                        }.reversed()
                    }
                }
            }.launchIn(coroutineScope)

        server.registrationStream
            .onEach { store ->
                println("Registration --> $store")
                registrationMutex.withLock {
                    _registeredStores.add(store)
                    _registrationStream.update {
                        _registeredStores.toList()
                    }
                }
            }.launchIn(coroutineScope)
    }

    override fun handleIntent(intent: UserIntent) {
        coroutineScope.launch {
            _debugStream.emit(null)
            when (intent) {
                is UserIntent.StartDebugging -> {
                    server.debug(intent.storeName)
                    _state.update {
                        State.Debugging(intent.storeName)
                    }
                }
                is UserIntent.StopDebugging -> {
                    server.send(
                        Command.Continue(intent.storeName)
                    )
                    server.debug(null)
                    _state.update { State.Stopped }
                }
                is UserIntent.PauseDebugging -> {
                    server.send(Command.Pause(intent.storeName))
                    _state.update {
                        State.Paused(intent.storeName)
                    }
                }
                is UserIntent.NextDispatch -> {
                    println("Sending NextDispatch")
                    server.send(Command.NextDispatch(intent.storeName))
                }
                is UserIntent.PreviousDispatch -> {
                    server.send(Command.PreviousDispatch(intent.storeName))
                }
                is UserIntent.ReplayDispatch -> {
                    server.send(Command.ReplayDispatch(intent.storeName, intent.dispatchId))
                }
                is UserIntent.DispatchOverride -> {
                    server.send(
                        Command.DispatchOverride(intent.storeName, Json.decodeFromString(intent.text))
                    )
                }
            }
        }
    }

    fun prettyPrintJson(jsonText: String): String {
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