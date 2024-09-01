package kdux.tools

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * An enhancer that batches actions based on a specified time duration. Actions are accumulated
 * in a batch, and when the elapsed time since the start of the batch exceeds the specified
 * `batchDuration`, all actions in the batch are dispatched to the store at once.
 *
 * This enhancer is useful in scenarios where you want to delay processing actions and
 * dispatch them together to minimize state updates or improve performance.
 *
 * Note that batched actions will not be dispatched until the next dispatch call after the
 * [batchDuration] expires.
 *
 * @param batchDuration The duration for which actions are accumulated before being dispatched.
 *                      Once this duration is exceeded, the batch of actions is dispatched at the
 *                      next call to dispatch.
 */
class BatchEnhancer<State: Any, Action: Any>(
    private val batchDuration: Duration
): Enhancer<State, Action> {

    init {
        require(batchDuration > Duration.ZERO) {
            "Batch duration must be greater than zero."
        }
    }

    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {

            private val timeSource = TimeSource.Monotonic
            private val now: TimeSource.Monotonic.ValueTimeMark get() = timeSource.markNow()
            private var batchStart = now
            private val elapsedTime: Duration get() = now.minus(batchStart)
            private val batch = mutableListOf<Action>()
            private val batchMutex = Mutex()

            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) {
                val actionsToDispatch = mutableListOf<Action>()
                batchMutex.withLock {
                    batch.add(action)

                    if (elapsedTime > batchDuration) {
                        actionsToDispatch.addAll(batch)
                        batchStart = now
                    }
                }

                actionsToDispatch.forEach {
                    store.dispatch(it)
                }
            }
        }
    }
}