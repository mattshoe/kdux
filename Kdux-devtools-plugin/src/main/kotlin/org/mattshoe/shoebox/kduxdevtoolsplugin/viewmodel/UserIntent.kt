package org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel

sealed interface UserIntent {
    data class StopDebugging(val storeName: String): UserIntent
    data class StartDebugging(val storeName: String): UserIntent
    data class PauseDebugging(val storeName: String): UserIntent
    data class StepOver(val storeName: String): UserIntent
    data class StepBack(val storeName: String): UserIntent
    data class ReplayDispatch(val storeName: String, val dispatchId: String): UserIntent
    data class DispatchOverride(val storeName: String, val text: String): UserIntent
    data object ClearLogs: UserIntent
}