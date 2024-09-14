package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import kotlinx.coroutines.flow.Flow
import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattsho.shoebox.devtools.common.DispatchResult
import org.mattsho.shoebox.devtools.common.State
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.UserCommand
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration

interface DevToolsServer {
    val dispatchRequestStream: Flow<DispatchRequest>
    val dispatchResultStream: Flow<DispatchResult>
    val registrationStream: Flow<RegistrationChange>
    val currentStateStream: Flow<State>
    fun send(userCommand: UserCommand)
    fun debug(storeName: String?)
    fun start()
    fun stop()
}

