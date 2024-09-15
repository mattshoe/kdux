package org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable
import org.mattsho.shoebox.devtools.common.State

@Serializable
data class CurrentState(
    val storeName: String,
    val state: State
)