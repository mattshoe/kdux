package org.mattshoe.shoebox.devtools.server

import kotlinx.coroutines.flow.Flow
import org.mattsho.shoebox.devtools.common.ServerRequest
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Command

internal interface ClientDebugSession {
    val adHocCommands: Flow<Command>

    suspend fun send(serverRequest: ServerRequest)
    suspend fun awaitResponse(serverRequest: ServerRequest): Command
    suspend fun closeSession()
}