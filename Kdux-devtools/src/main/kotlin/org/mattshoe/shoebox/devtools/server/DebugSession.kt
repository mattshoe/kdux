package org.mattshoe.shoebox.devtools.server

import kotlinx.coroutines.flow.Flow
import org.mattsho.shoebox.devtools.common.ServerRequest
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.CommandPayload

internal interface DebugSession {
    val adHocCommands: Flow<CommandPayload>

    suspend fun send(serverRequest: ServerRequest)
    suspend fun awaitResponse(serverRequest: ServerRequest): CommandPayload
    suspend fun closeSession()
}