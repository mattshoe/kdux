package kdux.tools

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store
import kotlin.time.Duration

/**
 * The `TimeoutEnhancer` enforces a time limit on the dispatching of actions. If the action is not
 * processed within the specified [timeout] duration, the dispatch is canceled, and a `TimeoutCancellationException`
 * is thrown.
 *
 * This enhancer is useful in scenarios where you want to ensure that certain actions are processed within a specific
 * time frame, preventing long-running operations from blocking the store.
 *
 * @param State The type representing the state managed by the store.
 * @param Action The type representing the actions that can be dispatched to the store.
 * @param timeout The maximum duration allowed for processing an action. If the action is not processed
 * within this duration, the dispatch is canceled.
 */
class TimeoutEnhancer<State: Any, Action: Any>(
    private val timeout: Duration
): Enhancer<State, Action> {

    init {
        require(timeout > Duration.ZERO) {
            "Timeout must be greater than zero."
        }
    }

    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {
            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) {
                withTimeout(timeout) {
                    store.dispatch(action)
                }
            }
        }
    }
}