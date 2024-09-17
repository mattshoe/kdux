package org.mattshoe.shoebox.devtools.server

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Defaults

internal object ServerClient {
    private val ktorClient = HttpClient {
        install(WebSockets)
        install(HttpTimeout)
    }
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startSession(
        id: String,
        host: String,
        port: Int
    ): ClientDebugSession {
        return ClientDebugSessionImpl(
            id,
            host,
            port,
            ktorClient,
            coroutineScope
        )
    }
}


