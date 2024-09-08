package org.mattshoe.shoebox.kdux.gson

import com.google.gson.Gson
import kdux.dsl.StoreDslMenu

val _gsonFallback by lazy { Gson() }
val _charSet = Charsets.UTF_8

inline fun <reified State : Any, Action : Any> StoreDslMenu<State, Action>.persistWithGson(
    key: String,
    gson: Gson = _gsonFallback,
    noinline onError: (State?, Throwable) -> Unit = { _, _ -> }
) {
    persist(
        key,
        serializer = { state, outputStream ->
            outputStream.write(
                gson.toJson(state).toByteArray(_charSet)
            )
        },
        deserializer = {
            gson.fromJson(it.reader(_charSet), State::class.java)
        },
        onError
    )
}