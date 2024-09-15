package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.UserCommand

sealed interface ServerIntent {
    data class Command(val command: UserCommand): ServerIntent
    data class StartDebugging(val storeName: String): ServerIntent
    data class PauseDebugging(val storeName: String): ServerIntent
    data class StopDebugging(val storeName: String): ServerIntent
    data object StopServer: ServerIntent
    data object StartServer: ServerIntent
}