package org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class Command(
    val command: String,
    val payload: String? = null
)