package org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

@Serializable
data class DispatchOverride(
    val action: Action
)