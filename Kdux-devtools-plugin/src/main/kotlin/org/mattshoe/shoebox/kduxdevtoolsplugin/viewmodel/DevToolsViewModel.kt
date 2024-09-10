package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattsho.shoebox.devtools.common.DispatchResult
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.*
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Command
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DispatchLog(
    val request: DispatchRequest,
    val result: DispatchResult?
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
    private val _debugStream = MutableSharedFlow<DispatchRequest>(replay = 1)
    private val registrationMutex = Mutex()


    override val state = _state.asStateFlow()
    val dispatchStream: Flow<List<DispatchLog>> = _dispatchStream.asSharedFlow()
    val registrationStream: Flow<List<Registration>> = _registrationStream.asStateFlow()
    val debugStream: Flow<DispatchRequest?> = _debugStream.asSharedFlow()

    init {
        server.dispatchRequestStream
            .onEach {
                dispatchLogMutex.withLock {
                    dispatchLog.add(
                        0,
                        DispatchLog(it.copy(timestamp = convertToHumanReadable(it.timestamp)), null)
                    )
                    _dispatchStream.update {
                        dispatchLog.toList()
                    }
                }
            }.launchIn(coroutineScope)

        server.registrationStream
            .onEach { store ->
                registrationMutex.withLock {
                    _registeredStores.add(store)
                    _registrationStream.update {
                        _registeredStores.toList()
                    }
                }
            }.launchIn()
    }

    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.StartDebugging -> {
                server.start()
                _state.update {
                    State.Debugging(intent.storeName)
                }
            }
            is UserIntent.StopDebugging -> {
                server.send(
                    Command.Continue(intent.storeName)
                )
                _state.update { State.Stopped }
            }
            is UserIntent.PauseDebugging -> {
                server.send(Command.Pause(intent.storeName))
                _state.update {
                    State.Paused(intent.storeName)
                }
            }
            is UserIntent.NextDispatch -> {
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

    private fun convertToHumanReadable(isoTimestamp: String): String {
        val instant = Instant.parse(isoTimestamp)
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault()) // Adjusts to system's local timezone
        return formatter.format(instant)
    }

}