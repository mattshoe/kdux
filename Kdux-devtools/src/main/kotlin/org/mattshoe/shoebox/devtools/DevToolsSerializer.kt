package org.mattshoe.shoebox.devtools

interface DevToolsSerializer<State: Any, Action: Any> {
    fun serializeAction(action: Action): String
    fun serializeState(state: State): String
    fun deserializeAction(action: org.mattsho.shoebox.devtools.common.Action): Action
    fun deserializeState(state: org.mattsho.shoebox.devtools.common.State): State
}