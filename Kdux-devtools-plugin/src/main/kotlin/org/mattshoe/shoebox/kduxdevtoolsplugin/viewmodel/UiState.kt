package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.CurrentState

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