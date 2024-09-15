package org.mattshoe.shoebox.kduxdevtoolsplugin.server

sealed interface ServerState {
    data object Started: ServerState
    data object Stopped: ServerState
}