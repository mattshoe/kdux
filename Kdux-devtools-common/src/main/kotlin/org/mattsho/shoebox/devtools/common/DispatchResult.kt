package org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class DispatchResult(
    val dispatchId: String,
    val request: DispatchRequest,
    val storeName: String,
    val action: Action,
    val previousState: State,
    val newState: State,
    val timestamp: String
)