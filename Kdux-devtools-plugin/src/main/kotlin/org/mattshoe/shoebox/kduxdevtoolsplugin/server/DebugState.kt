package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.CurrentState

sealed interface DebugState {
    data class ActivelyDebugging(
        val storeName: String,
        val currentState: CurrentState? = null,
        val dispatchRequest: DispatchRequest? = null
    ): DebugState

    data class DebuggingPaused(
        val storeName: String,
        val currentState: CurrentState? = null
    ): DebugState

    data object NotDebugging: DebugState
}