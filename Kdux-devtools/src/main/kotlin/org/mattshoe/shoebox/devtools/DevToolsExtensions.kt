package org.mattshoe.shoebox.devtools

import kdux.dsl.StoreDslMenu

inline fun <reified State : Any, Action : Any> StoreDslMenu<State, Action>.devtools(
    noinline actionSerializer: suspend (Action) -> String,
    noinline actionDeserializer: suspend (org.mattsho.shoebox.devtools.common.Action) -> Action,
    noinline stateSerializer: suspend (State) -> String,
    noinline stateDeserializer: suspend (org.mattsho.shoebox.devtools.common.State) -> State
) {
    add(
        DevToolsEnhancer(
            actionSerializer,
            actionDeserializer,
            stateSerializer,
            stateDeserializer
        )
    )
}

inline fun <reified State : Any, Action : Any> StoreDslMenu<State, Action>.devtools(
    serializer: DevToolsSerializer<State, Action>
) {
    add(
        DevToolsEnhancer(
            serializer::serializeAction,
            serializer::deserializeAction,
            serializer::serializeState,
            serializer::deserializeState
        )
    )
}