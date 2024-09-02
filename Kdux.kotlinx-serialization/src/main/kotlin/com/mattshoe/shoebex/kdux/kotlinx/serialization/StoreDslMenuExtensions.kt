package com.mattshoe.shoebex.kdux.kotlinx.serialization

import kdux.dsl.StoreDslMenu
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

val charSet = Charsets.UTF_8

/**
 * Adds a [PersistenceEnhancer] to the store that uses `kotlinx-serialization` to serialize State objects, enabling automatic persistence and restoration of the store's state.
 * This function is designed to make it easy to persist and restore the state of your store across application restarts
 * or other lifecycle events.
 *
 * This function enables automatic state persistence for the store, leveraging Kotlinx Serialization to serialize
 * the state and store it in a file. The state is restored from the file when the store is initialized.
 *
 * ### Important Details:
 *
 * The [key] parameter is used to determine the filename or unique identifier for the stored state. You must ensure that the
 * key is unique if multiple stores are being persisted, to avoid conflicts. The [key] can be used to associate user-data to avoid
 * exposing the wrong user's data to another, by appending a user-id or otherwise to the key.
 *
 * @param State The type representing the state managed by the store. The state must be serializable by Kotlinx Serialization.
 * @param Action The type representing the actions that can be dispatched to the store.
 * @param key A unique identifier for the persisted state, used to determine the filename or key under which the state is stored.
 * @param onError A lambda function invoked if an error occurs during serialization or deserialization. Defaults to a no-op.
 */

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified State : Any, Action : Any> StoreDslMenu<State, Action>.persistWithKotlinxSerialization(
    key: String,
    noinline onError: (State?, Throwable) -> Unit = { _, _ -> }
) {
    persist(
        key,
        serializer = { state, outputStream ->
            outputStream.write(
                Json.encodeToString(state).toByteArray(charSet)
            )
        },
        deserializer = {
            Json.decodeFromStream(it)
        },
        onError
    )
}




@OptIn(ExperimentalSerializationApi::class)
fun <State : Any, Action : Any> StoreDslMenu<State, Action>.persist(
    key: String,
    onError: (State?, Throwable) -> Unit
) {
    persist(
        key,
        serializer = { state, outputStream ->
            TODO()
        },
        deserializer = {
            TODO()
        },
        onError
    )
}