package org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class Action(
    val name: String,
    val json: String
)