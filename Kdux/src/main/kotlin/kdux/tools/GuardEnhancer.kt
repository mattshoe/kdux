package kdux.tools

import kotlinx.coroutines.flow.Flow
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store

/**
 * The `GuardEnhancer` blocks actions that fail the [isAuthorized] check. Before dispatching an action,
 * it runs the [isAuthorized] function. If the function returns `true`, the action is dispatched;
 * otherwise, it is blocked.
 *
 * @param State The type representing the state managed by the store.
 * @param Action The type representing the actions that can be dispatched to the store.
 * @param isAuthorized A suspend function that returns `true` if the action should be dispatched,
 * or `false` if it should be blocked.
 */
open class GuardEnhancer<State: Any, Action: Any>(
    private val isAuthorized: suspend (Action) -> Boolean
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
                if (isAuthorized(action)) {
                    store.dispatch(action)
                }
            }
        }
    }
}