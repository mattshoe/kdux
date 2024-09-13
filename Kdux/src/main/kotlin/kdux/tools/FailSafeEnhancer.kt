package kdux.tools

import kotlinx.coroutines.flow.Flow
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store

/**
 * The `FailSafeEnhancer` provides a mechanism to handle errors during dispatch.
 *
 * If an exception occurs while processing an action, the [onError] function is invoked, allowing you to handle the error
 * and optionally retry the same action or dispatch a different action.
 *
 * This enhancer is useful in scenarios where you want to prevent the store from crashing due to unexpected errors
 * and instead recover gracefully or provide fallback logic.
 *
 * @param State The type representing the state managed by the store.
 * @param Action The type representing the actions that can be dispatched to the store.
 * @param onError A suspend function that is invoked when an error occurs during action processing.
 *                It receives the current state, the action that caused the error, the error itself,
 *                and a callback function to dispatch a new action. It returns an optional action to retry
 *                or a different action, or `null` if no further action is needed.
 */
class FailSafeEnhancer<State: Any, Action: Any>(
    private val onError: suspend (
        state: State,
        action: Action,
        error: Throwable,
        dispatch: suspend (Action) -> Unit
    ) -> Unit
): Enhancer<State, Action> {
    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {

            override val name: String
                get() = store.name
            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) {
                try {
                    store.dispatch(action)
                } catch (ex: Throwable) {
                    onError(currentState, action, ex) { newAction ->
                        store.dispatch(newAction)
                    }
                }
            }
        }
    }
}