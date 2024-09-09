package org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common

import kotlinx.serialization.Serializable

sealed interface Command {
    val storeName: String
    val payload: CommandPayload

    data class Continue(
        override val storeName: String,
        override val payload: CommandPayload = CommandPayload("continue")
    ): Command
    data class Pause(
        override val storeName: String
    ): Command {
        override val payload = CommandPayload("pause")
    }
    data class NextDispatch(
        override val storeName: String
    ): Command {
        override val payload = CommandPayload("next")
    }
    data class PreviousDispatch(
        override val storeName: String
    ): Command {
        override val payload = CommandPayload("previous")
    }
    data class ReplayDispatch(
        override val storeName: String,
        private val dispatchId: String
    ): Command {
        override val payload = CommandPayload("replay", dispatchId)
    }
}

@Serializable
data class CommandPayload(
    val command: String,
    val data: String? = null
)
