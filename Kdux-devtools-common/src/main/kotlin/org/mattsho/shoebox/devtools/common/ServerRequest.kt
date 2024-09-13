package org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class ServerRequest(
    val id: String? = null,
    val type: Type,
    val data: String
) {
    @Serializable
    enum class Type {
        REGISTRATION,
        DISPATCH_REQUEST,
        DISPATCH_RESULT
    }
}