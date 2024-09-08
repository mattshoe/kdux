package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import kotlinx.coroutines.flow.Flow

interface DevToolsServer {
    val data: Flow<StoreData>
    fun send(command: Command)
    fun start()
    fun stop()
}

