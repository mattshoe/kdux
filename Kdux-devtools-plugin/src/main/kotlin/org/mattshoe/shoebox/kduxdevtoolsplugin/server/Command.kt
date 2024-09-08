package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import kotlinx.serialization.Serializable

sealed interface Command {
    val payload: CommandPayload

    data object Next: Command {
        override val payload = CommandPayload("next")
    }
    data object Previous: Command {
        override val payload = CommandPayload("previous")
    }
    data class Replay(private val action: Action): Command {
        override val payload = CommandPayload("replay", action)
    }
}

@Serializable
data class CommandPayload(
    val command: String,
    val action: Action? = null
)
