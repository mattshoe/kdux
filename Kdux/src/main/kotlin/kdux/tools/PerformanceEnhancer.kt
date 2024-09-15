package kdux.tools

import kotlinx.coroutines.flow.Flow
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * An enhancer that measures and logs the performance of each action dispatched to the store.
 * Specifically, it measures the total time taken from when the action is first dispatched
 * to the time that the dispatch function completes. This includes all middleware, enhancers,
 * the reducer.
 *
 * This enhancer is useful for monitoring the performance of your state management system,
 * helping you identify slow actions and optimize them if necessary.
 *
 * @param log A suspendable function that takes a `PerformanceData` object and logs it.
 *            The logging function is called every time an action is dispatched, with
 *            the duration of the dispatch process.
 */
open class PerformanceEnhancer<State: Any, Action: Any>(
    private val log: suspend (PerformanceData<Action>) -> Unit
): Enhancer<State, Action> {
    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {

            override val name: String
                get() = store.name
            override val currentState: State
                get() = store.currentState
            override val state: Flow<State>
                get() = store.state

            override suspend fun dispatch(action: Action) {
                measureTime {
                    store.dispatch(action)
                }.also {
                    log(
                        PerformanceData(store.name, action, it)
                    )
                }
            }
        }
    }
}

/**
 * A data class that holds performance data related to the dispatch process.
 * It contains the name of the store and the duration it took to dispatch
 * and process the action.
 *
 * @param storeName The name of the store where the action was dispatched.
 * @param action The action that was dispatched
 * @param duration The time duration it took to dispatch and process the action.
 */
data class PerformanceData<Action: Any> internal constructor(
    val storeName: String,
    val action: Action,
    val duration: Duration
)