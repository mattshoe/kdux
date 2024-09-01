package kdux.tools

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store

/**
 * An enhancer that logs every action dispatched to the store. The logging is handled
 * asynchronously, allowing you to log actions without blocking the dispatch process.
 *
 * This enhancer is useful for debugging or monitoring purposes, providing insight into
 * the actions being dispatched and allowing you to track how the state changes over time.
 *
 * @param log A suspendable function that takes an `Action` as a parameter and logs it.
 *            The logging function is called every time an action is dispatched.
 */
open class LoggingEnhancer<State: Any, Action: Any>(
    private val log: suspend (Action) -> Unit
): Enhancer<State, Action> {

    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {
            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) = coroutineScope {
                launch {
                    log(action)
                }
                store.dispatch(action)
            }
        }
    }
}