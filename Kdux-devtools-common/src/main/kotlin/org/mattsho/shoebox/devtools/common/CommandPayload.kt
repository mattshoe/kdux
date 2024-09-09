package org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class CommandPayload(
    val command: String,
    val data: String? = null
)