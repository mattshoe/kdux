package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.DevToolsServer
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.DevToolsServerImpl
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.DispatchRequest
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.DispatchResult
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
    data class Debugging(val data: List<String>): State
}

sealed interface UserIntent {
    data class StartDebugging(val storeName: String): UserIntent
    data object StopDebugging: UserIntent
}

class DevToolsViewModel(
    private val server: DevToolsServer = DevToolsServerImpl()
): ViewModel<State, UserIntent> {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<State>(State.Stopped)
    private val dispatchLog = mutableListOf<DispatchLog>()
    private val _dispatchStream = MutableStateFlow<List<DispatchLog>>(emptyList())
    private val dispatchLogMutex = Mutex()
    private val exceptionHandler = CoroutineExceptionHandler { _, error ->
        println(error)
    }


    override val state = _state.asStateFlow()
    val dispatchStream: Flow<List<DispatchLog>> = _dispatchStream.asSharedFlow()

    init {
        server.data
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
    }

    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.StartDebugging -> {
                server.start()
                _state.update { State.Debugging(emptyList()) }
            }
            is UserIntent.StopDebugging -> {
                server.stop()
                _state.update { State.Stopped }
            }
        }
    }

    private fun convertToHumanReadable(isoTimestamp: String): String {
        val instant = Instant.parse(isoTimestamp)
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault()) // Adjusts to system's local timezone
        return formatter.format(instant)
    }

}