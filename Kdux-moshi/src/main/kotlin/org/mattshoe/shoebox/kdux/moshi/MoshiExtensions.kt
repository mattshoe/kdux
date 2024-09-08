package org.mattshoe.shoebox.kdux.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kdux.dsl.StoreDslMenu

/**
 * DO NOT USE.
 */
val _kduxInternalDefaultMoshiNotForPublicUse = Moshi
    .Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

/**
 * Persists the store's state using Moshi for serialization.
 *
 * This function leverages the Moshi library to serialize and deserialize the state of the store.
 * It uses the provided Moshi instance or falls back to a default instance. The serialized data is saved
 * under a unique [key], which you must ensure is unique across stores to avoid data conflicts.
 *
 * @param State The type of the state being persisted. This class should be annotated or otherwise compatible with Moshi's serialization.
 * @param Action The type of actions being dispatched to the store.
 * @param key A unique identifier for the persisted state, typically used as a filename or key under which the state is stored.
 * @param moshi The Moshi instance used for serialization and deserialization. If not provided, a fallback Moshi instance will be used.
 * @param onError A callback to handle any errors that occur during serialization or deserialization. It receives the current state (if available) and the thrown error.
 */
inline fun <reified State : Any, Action : Any> StoreDslMenu<State, Action>.persistWithMoshi(
    key: String,
    moshi: Moshi = _kduxInternalDefaultMoshiNotForPublicUse,
    noinline onError: (State?, Throwable) -> Unit = { _, _ -> }
) {
    val adapter = moshi.adapter(State::class.java)
    persist(
        key,
        serializer = { state, outputStream ->
            outputStream.write(
                adapter.toJson(state).toByteArray()
            )
        },
        deserializer = {
            adapter.fromJson(it.readAllBytes().decodeToString())!!
        },
        onError
    )
}