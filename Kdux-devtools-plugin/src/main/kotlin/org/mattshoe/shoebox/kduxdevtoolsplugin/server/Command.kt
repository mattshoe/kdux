package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import kotlinx.serialization.Serializable

@Serializable
data class Command(val command: String)