package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import kotlinx.coroutines.flow.Flow
import org.mattsho.shoebox.devtools.common.DispatchResult

interface DevToolsServer {
    val serverState: Flow<ServerState>
    val dispatchResultStream: Flow<DispatchResult>
    val registrationStream: Flow<RegistrationChange>
    val debugState: Flow<DebugState>
    fun execute(intent: ServerIntent)
}

