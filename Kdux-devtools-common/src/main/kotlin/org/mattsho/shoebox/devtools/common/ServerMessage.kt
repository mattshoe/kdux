package org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class ServerMessage(
    val responseCorrelationId: String?,
    val data: String
)
