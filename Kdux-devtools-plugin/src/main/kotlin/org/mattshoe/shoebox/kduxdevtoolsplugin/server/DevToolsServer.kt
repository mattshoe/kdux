package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import kotlinx.coroutines.flow.Flow

interface DevToolsServer {
    val data: Flow<DispatchRequest>
    fun send(command: Command)
    fun start()
    fun stop()
}

