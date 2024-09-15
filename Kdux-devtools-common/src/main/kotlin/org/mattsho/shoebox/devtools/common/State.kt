package org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class State(
    val name: String,
    val json: String
)