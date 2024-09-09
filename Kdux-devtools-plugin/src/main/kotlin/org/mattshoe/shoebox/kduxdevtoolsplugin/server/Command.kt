package org.mattshoe.shoebox.kduxdevtoolsplugin.server

import kotlinx.serialization.Serializable

sealed interface Command {
    val payload: CommandPayload

    data object Pause: Command {
        override val payload = CommandPayload("pause")
    }
    data object NextDispatch: Command {
        override val payload = CommandPayload("next")
    }
    data object PreviousDispatch: Command {
        override val payload = CommandPayload("previous")
    }
    data class ReplayDispatch(private val dispatchId: String): Command {
        override val payload = CommandPayload("replay", dispatchId)
    }
}

@Serializable
data class CommandPayload(
    val command: String,
    val data: String? = null
)
