package kdux.tools

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * The `ThrottleEnhancer` is an [Enhancer] that limits the rate at which actions are dispatched.
 * It ensures that actions are only dispatched at most once per specified [interval].
 *
 * Dispatches are **_not dropped_** if they happen too quickly, they are simply queued up and `dispatch` will suspend
 * until the appropriate amount of time has passed. If multiple coroutines attempt to dispatch at once,
 * then they are queued up and executed in the order they were dispatched, at the rate of 1 queued dispatch
 * per [interval].
 *
 * This is useful in scenarios where actions might be dispatched rapidly (e.g., user input events)
 * and you want to avoid overwhelming the store or causing unnecessary state updates.
 *
 * The enhancer works by delaying subsequent actions if they occur before the defined interval
 * has passed since the last dispatched action.
 *
 * @param State The type representing the state managed by the store.
 * @param Action The type representing the actions that can be dispatched to the store.
 * @param interval The minimum time interval between consecutive action dispatches. If an action
 * is dispatched before this interval has passed since the last action, it will be delayed.
 *
 * Example usage:
 * ```kotlin
 * val store = store(
 *     initialState = MyState(),
 *     reducer = MyReducer()
 * ) {
 *     add(ThrottleEnhancer(500.milliseconds))
 * }
 * ```
 */
open class ThrottleEnhancer<State: Any, Action: Any>(
    private val interval: Duration
): Enhancer<State, Action> {

    init {
        require(interval > Duration.ZERO) {
            "Throttle interval must be greater than zero."
        }
    }

    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {

            private val timeSource = TimeSource.Monotonic
            private val now: TimeSource.Monotonic.ValueTimeMark get() = timeSource.markNow()
            private var lastDispatch = now.minus(interval)
            private val elapsedTime: Duration get() = now.minus(lastDispatch)
            private val timeToWait: Duration get() = interval.minus(elapsedTime)
            private val mutex = Mutex()

            override val name: String
                get() = store.name
            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) {
                mutex.withLock {
                    if (timeToWait > Duration.ZERO) {
                        delay(timeToWait)
                    }
                    lastDispatch = now
                }

                store.dispatch(action)
            }
        }
    }
}