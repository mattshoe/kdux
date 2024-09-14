package org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class ServerRequest(
    val responseCorrelationId: String? = null,
    val type: Type,
    val data: String
) {
    @Serializable
    enum class Type {
        REGISTRATION,
        CURRENT_STATE,
        DISPATCH_REQUEST,
        DISPATCH_RESULT
    }
}