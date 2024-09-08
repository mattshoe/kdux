package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.DevToolsServer
import org.mattshoe.shoebox.kduxdevtoolsplugin.server.DevToolsServerImpl

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
    private val _state = MutableStateFlow<State>(State.Stopped)
    override val state = _state.asStateFlow()

    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.StartDebugging -> {
//                server.start()
                _state.update { State.Debugging(emptyList()) }
            }
            is UserIntent.StopDebugging -> {
//                server.stop()
                _state.update { State.Stopped }
            }
        }
    }

}