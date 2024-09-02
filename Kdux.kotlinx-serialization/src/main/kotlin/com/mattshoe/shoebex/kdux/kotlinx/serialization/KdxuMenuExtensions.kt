package com.mattshoe.shoebex.kdux.kotlinx.serialization

import kdux.dsl.StoreDslMenu
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

val charSet = Charsets.UTF_8

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