package kdux.tools

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.TimeSource

private val LONG_TIME_FROM_NOW = 10.days

/**
 * An enhancer that debounces dispatched actions based on a specified time duration.
 * This means that actions will only be dispatched if a specified amount of time has passed
 * since the last dispatched action. If actions are dispatched more frequently than the
 * debounce duration, only the first action in the burst will be dispatched.
 *
 * In other words, if you try to dispatch more than one action in a given [duration], then
 * all actions besides the first will be DROPPED. They will not be queued to execute later.
 *
 * This enhancer is useful in scenarios where you want to limit the rate at which actions
 * are processed, such as preventing excessive updates in response to rapid user input.
 *
 * @param duration The debounce duration. Actions will only be dispatched if this amount
 *                 of time has passed since the last dispatched action.
 *
 * @throws IllegalArgumentException if `duration` is less than or equal to zero.
 */
open class DebounceEnhancer<State: Any, Action: Any>(
    private val duration: Duration
): Enhancer<State, Action> {

    init {
        require(duration > Duration.ZERO) {
            "Debounce duration must be greater than zero."
        }
    }

    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {
            private val timeSource = TimeSource.Monotonic
            private val now: TimeSource.Monotonic.ValueTimeMark get() = timeSource.markNow()
            private var debounceStart = now.minus(LONG_TIME_FROM_NOW)
            private val elapsedTimeSinceLastDispatch: Duration get() = now.minus(debounceStart)
            private val debounceMutex = Mutex()

            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) {
                var actionToDispatch: Action? = null

                debounceMutex.withLock {
                    if (elapsedTimeSinceLastDispatch > duration) {
                        actionToDispatch = action
                        debounceStart = now
                    }
                }

                actionToDispatch?.let {
                    store.dispatch(it)
                }
            }
        }
    }
}