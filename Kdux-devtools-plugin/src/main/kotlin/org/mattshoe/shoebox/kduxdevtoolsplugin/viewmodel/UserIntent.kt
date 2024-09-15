package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

import org.mattsho.shoebox.devtools.common.DispatchResult

sealed interface UserIntent {
    data class StopDebugging(val storeName: String): UserIntent
    data class StartDebugging(val storeName: String): UserIntent
    data class PauseDebugging(val storeName: String): UserIntent
    data class StepOver(val storeName: String): UserIntent
    data class StepBack(val storeName: String): UserIntent
    data class RestoreState(val dispatch: DispatchResult): UserIntent
    data class ReplayAction(val dispatch: DispatchResult): UserIntent
    data class ReplayDispatch(val dispatch: DispatchResult): UserIntent
    data class DispatchOverride(val storeName: String, val text: String): UserIntent
    data object ClearLogs: UserIntent
}