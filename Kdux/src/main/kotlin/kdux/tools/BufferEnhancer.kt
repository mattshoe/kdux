package kdux.tools

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store

/**
 * An enhancer that buffers dispatched actions until a specified buffer size is reached.
 * Once the buffer is full, all buffered actions are dispatched to the store at once.
 *
 * This enhancer is useful in scenarios where you want to delay processing of actions
 * and batch them together to minimize state updates or improve performance.
 *
 * When buffer is flushed, actions are dispatched in the order they entered the buffer.
 *
 * @param bufferSize The size of the buffer that determines when the buffered actions
 *                   should be dispatched. Must be greater than zero.
 *
 * @throws IllegalArgumentException if `bufferSize` is less than or equal to zero.
 */
open class BufferEnhancer<State: Any, Action: Any>(
    private val bufferSize: Int
): Enhancer<State, Action> {
    init {
        require(bufferSize > 0) {
            "Buffer size must be greater than zero."
        }
    }

    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {
            private val bufferMutex = Mutex()
            private val buffer = mutableListOf<Action>()
            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) {
                val actionsToDispatch = mutableListOf<Action>()
                bufferMutex.withLock {
                    buffer.add(action)

                    if (buffer.size >= bufferSize) {
                        actionsToDispatch.addAll(buffer)
                        buffer.clear()
                    }
                }
                actionsToDispatch.forEach {
                    store.dispatch(it)
                }
            }
        }
    }
}