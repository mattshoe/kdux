package com.example.kdux.android

import android.os.Parcel
import android.os.Parcelable
import kdux.dsl.StoreDslMenu

inline fun <reified State : Parcelable, Action : Any> StoreDslMenu<State, Action>.persistAsParcelable(
    key: String,
    noinline onError: (State?, Throwable) -> Unit = { _, _ -> }
) {
    persist(
        key,
        serializer = { state, outputStream ->
            val parcel = Parcel.obtain()
            try {
                parcel.writeParcelable(state, 0)
                val bytes = parcel.marshall()
                outputStream.write(bytes)
            } catch (e: Throwable) {
                onError(state, e)
            } finally {
                parcel.recycle()
            }
        },
        deserializer = {
            val bytes = it.readBytes()
            val parcel = Parcel.obtain()
            try {
                parcel.unmarshall(bytes, 0, bytes.size)
                parcel.setDataPosition(0)
                parcel.readParcelable<State>(State::class.java.classLoader)
            } catch (e: Throwable) {
                onError(null, e)
                throw e
            } finally {
                parcel.recycle()
            } ?: throw IllegalStateException("Parcelable could not be deserialized: ${State::class.simpleName}")
        },
        onError
    )
}