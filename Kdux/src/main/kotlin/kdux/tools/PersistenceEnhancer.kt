package kdux.tools

import kdux.KduxMenu
import kdux.caching.CacheUtility
import kdux.log.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The `PersistenceEnhancer` is an [Enhancer] designed to automatically persist and restore the state of a store.
 * This enhancer ensures that the state of the store is saved to a persistent storage medium and restored upon
 * initialization, providing durability across application restarts.
 *
 * ### Important Details:
 *
 * The [key] parameter is used to determine the filename or unique identifier for the stored state. You must ensure that the
 * key is unique if multiple stores are being persisted, to avoid conflicts. The [key] can be used to associate user-data to avoid
 * exposing the wrong user's data to another, by appending a user-id or otherwise to the key.
 *
 * @param State The type representing the state managed by the store. It must be serializable by the provided
 *     serializer.
 * @param Action The type representing the actions that can be dispatched to the store.
 * @param key A unique identifier for the persisted state. This is used to determine the filename or key under which the
 *     state is stored.
 * @param serializer A suspend function that serializes the state into the provided [OutputStream]. It must write the
 *     state in a format that can later be deserialized.
 * @param deserializer A function that deserializes the state from the provided [InputStream]. It must return a state
 *     object that matches the type of the store's state.
 * @param fileProvider A function that provides the [File] object where the state will be stored. Defaults to creating a
 *     `File` based on the key.
 * @param outputStreamProvider A function that provides the [OutputStream] for writing the state. Defaults to creating a
 *     `FileOutputStream` that overwrites the file if it exists.
 */
class PersistenceEnhancer<State : Any, Action : Any>(
    private val key: String,
    private val serializer: suspend (state: State, outputStream: OutputStream) -> Unit,
    private val deserializer: (inputStream: InputStream) -> State,
    private val onError: (state: State?, error: Throwable) -> Unit = { s, e -> Logger.get().e("Error while processing $s", e) },
    private val fileProvider: (String) -> File = { File(it) },
    private val inputStreamProvider: (File) -> InputStream = { it.inputStream() },
    private val outputStreamProvider: (String) -> OutputStream = { FileOutputStream(it, false) },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : Enhancer<State, Action> {

    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {
            private val mutex = Mutex()
            private lateinit var flow: Flow<State>
            private val initializationCompleted = CompletableDeferred<Unit>()
            private lateinit var _currentState: () -> State
            private val hasInitialValueBeenOverwritten = AtomicBoolean(false)
            private val mergedFlow = channelFlow {
                initializationCompleted.await()

                if (hasInitialValueBeenOverwritten.get()) {
                    store.state
                        .onEach {
                            send(it)
                        }.launchIn(this)
                } else {
                    merge(
                        store.state.drop(1),
                        MutableStateFlow(_currentState())
                    ).onEach {
                        send(it)
                    }.launchIn(this)
                }
            }

            init {
                try {
                    val cache = fileProvider(persistentCacheLocation)

                    if (cache.exists()) {
                        val inputStream = inputStreamProvider(cache)
                        val initialValue = deserializer(inputStream)
                        _currentState = { initialValue }
                    } else {
                        flow = store.state
                        _currentState = { store.currentState }
                    }
                } catch (ex: Throwable) {
                    flow = store.state
                    _currentState = {
                        store.currentState
                    }
                    onError(null, ex)
                } finally {
                    initializationCompleted.complete(Unit)
                }
            }

            override val state: Flow<State>
                get() = mergedFlow

            override val currentState: State
                get() = runBlocking {
                    initializationCompleted
                    _currentState()
                }

            override suspend fun dispatch(action: Action) {
                store.dispatch(action)

                hasInitialValueBeenOverwritten.set(true)

                mutex.withLock {
                    initializationCompleted.await()
                    val currentState = _currentState()
                    withContext(dispatcher) {
                        try {
                            outputStreamProvider(persistentCacheLocation).use {
                                serializer(currentState, it)
                            }
                        } catch (ex: Throwable) {
                            onError(currentState, ex)
                        }
                    }
                }
            }

            private val persistentCacheLocation: String
                get() = CacheUtility.cacheLocation(key)
        }
    }

}