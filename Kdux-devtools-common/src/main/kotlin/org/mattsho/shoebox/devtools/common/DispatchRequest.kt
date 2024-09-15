package org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class DispatchRequest(
    val dispatchId: String,
    val storeName: String,
    val currentState: State,
    val action: Action,
    val timestamp: String
)

