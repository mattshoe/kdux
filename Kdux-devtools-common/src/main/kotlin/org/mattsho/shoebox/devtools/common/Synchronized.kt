package org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class Synchronized<T>(
    private var value: T
) {
    private val mutex = Mutex()

    suspend fun update(mutator: suspend (T) -> Unit) {
        mutex.withLock {
            mutator(value)
        }
    }

    suspend fun <R> access(accessor: suspend (T) -> R): R {
        return mutex.withLock {
            accessor(value)
        }
    }

    suspend fun set(value: T) = mutex.withLock { this.value = value }
}