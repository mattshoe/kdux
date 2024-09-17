package org.mattshoe.shoebox.devtools

import kdux.dsl.StoreDslMenu
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Defaults

object KduxHost {
    val ANDROID_EMULATOR = "10.0.2.2"
}

/**
 * DSL extension that enhances a Kdux `Store` by integrating it with an external server-based
 * debugging tool. It enables remote control of the store's state and actions through a WebSocket
 * connection, allowing users to track, replay, and override state transitions and dispatched actions
 * in real time.
 *
 * ### Features:
 *
 * - **Serialization/Deserialization**: Accepts custom serializer and deserializer functions for both
 *   actions and state. These are necessary for converting actions and state into a transmittable format
 *   (such as JSON) and reconstructing them from received data.
 * - **DevTools Integration**: The `DevToolsEnhancer` is automatically added to the store, allowing real-time
 *   debugging and manipulation of the store from a remote server.
 * - **Flexible Serialization**: The function is generic, supporting any `State` and `Action` types as long
 *   as they can be serialized and deserialized using the provided functions.
 *
 * @param actionSerializer A suspend function that serializes an `Action` to a `String` for transmission
 *   to the WebSocket server.
 * @param actionDeserializer A suspend function that deserializes a `String` into an `Action` from data
 *   received over the WebSocket.
 * @param stateSerializer A suspend function that serializes the `State` to a `String` for transmission
 *   to the WebSocket server.
 * @param stateDeserializer A suspend function that deserializes a `String` into a `State` from data
 *   received over the WebSocket.
 * @param State The type representing the store's state.
 * @param Action The type representing actions that can be dispatched to the store.
 */
inline fun <reified State : Any, Action : Any> StoreDslMenu<State, Action>.devtools(
    host: String = Defaults.HOST,
    port: Int = Defaults.PORT,
    noinline actionSerializer: suspend (Action) -> String,
    noinline actionDeserializer: suspend (org.mattsho.shoebox.devtools.common.Action) -> Action,
    noinline stateSerializer: suspend (State) -> String,
    noinline stateDeserializer: suspend (org.mattsho.shoebox.devtools.common.State) -> State
) {
    add(
        DevToolsEnhancer(
            host,
            port,
            actionSerializer,
            actionDeserializer,
            stateSerializer,
            stateDeserializer
        )
    )
}

/**
 * DSL extension that enhances a Kdux `Store` by integrating it with an external server-based
 * debugging tool using a `DevToolsSerializer` for state and action serialization.
 *
 * This extension simplifies the process of integrating debugging tools by accepting a single
 * `DevToolsSerializer` object that encapsulates all serialization and deserialization logic
 * for both the state and actions. The `DevToolsEnhancer` is then added to the store, allowing
 * for real-time debugging and control over the store's state transitions and actions.
 *
 * ### Features:
 *
 * - **Simplified Serializer Interface**: This extension accepts a `DevToolsSerializer` object,
 *   which encapsulates both action and state serialization logic. This makes it easier to manage
 *   the serialization/deserialization process in a single place.
 * - **DevTools Integration**: The `DevToolsEnhancer` is automatically added to the store, enabling
 *   real-time debugging and manipulation from an external server over a WebSocket connection.
 * - **Flexible Serialization**: Supports any `State` and `Action` types as long as they can be
 *   serialized and deserialized using the provided `DevToolsSerializer` functions.
 *
 * @param serializer A `DevToolsSerializer` object that provides the serialization and deserialization
 *   logic for both the state and actions.
 * @param State The type representing the store's state.
 * @param Action The type representing actions that can be dispatched to the store.
 */
inline fun <reified State : Any, Action : Any> StoreDslMenu<State, Action>.devtools(
    host: String = Defaults.HOST,
    port: Int = Defaults.PORT,
    serializer: DevToolsSerializer<State, Action>
) {
    add(
        DevToolsEnhancer(
            host,
            port,
            serializer::serializeAction,
            serializer::deserializeAction,
            serializer::serializeState,
            serializer::deserializeState
        )
    )
}