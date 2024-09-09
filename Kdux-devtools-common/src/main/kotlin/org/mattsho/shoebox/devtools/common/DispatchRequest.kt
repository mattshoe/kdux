package org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class State(
    val name: String,
    val json: String
)

@Serializable
data class Action(
    val name: String,
    val json: String
)

@Serializable
data class DispatchRequest(
    val dispatchId: String,
    val storeName: String,
    val currentState: State,
    val action: Action,
    val timestamp: String
)

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

@Serializable
data class DispatchOverride(
    val action: Action
)

@Serializable
data class DebugRequest(
    val type: Type,
    val data: String
) {
    @Serializable
    enum class Type {
        REGISTRATION,
        DISPATCH_RESULT,
        DISPATCH_REQUEST
    }
}
