package org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mattsho.shoebox.devtools.common.Action
import org.mattsho.shoebox.devtools.common.DispatchResult

sealed interface UserCommand {
    val storeName: String
    val payload: Command

    data class Continue(
        override val storeName: String,
        override val payload: Command = Command("continue")
    ): UserCommand

    data class Pause(
        override val storeName: String
    ): UserCommand {
        override val payload = Command("pause")
    }

    data class NextDispatch(
        override val storeName: String
    ): UserCommand {
        override val payload = Command("next")
    }

    data class PreviousDispatch(
        override val storeName: String
    ): UserCommand {
        override val payload = Command("previous")
    }

    data class RestoreState(
        val dispatch: DispatchResult,
        override val storeName: String = dispatch.storeName,
        override val payload: Command = Command(
            "restoreState",
            payload = Json.encodeToString(dispatch)
        )
    ): UserCommand

    data class ReplayAction(
        val dispatch: DispatchResult,
        override val storeName: String = dispatch.storeName,
        override val payload: Command = Command(
            "replayAction",
            payload = Json.encodeToString(dispatch)
        )
    ): UserCommand

    data class ReplayDispatch(
        val dispatch: DispatchResult,
        override val storeName: String = dispatch.storeName,
        override val payload: Command = Command(
            "replayDispatch",
            payload = Json.encodeToString(dispatch)
        )
    ): UserCommand

    data class DispatchOverride(
        override val storeName: String,
        val action: Action
    ): UserCommand {
        override val payload = Command("override", Json.encodeToString(action))
    }
}

