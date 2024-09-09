package org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

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