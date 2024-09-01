package kdux.dsl

import kdux.tools.*
import org.mattshoe.shoebox.kdux.*
import org.mattshoe.shoebox.kdux.StoreBuilder
import kotlin.time.Duration

/**
 * A DSL menu for configuring a [Store] with middleware, enhancers, and custom store creators.
 *
 * @param State The type representing the state managed by the store.
 * @param Action The type representing the actions that can be dispatched to the store.
 * @property builder The internal builder used to construct the store.
 * @constructor Creates a [StoreDslMenu] with the specified initial state and reducer.
 * @param initialState The initial state of the store.
 * @param reducer The reducer that handles actions and updates the state.
 */
class StoreDslMenu<State: Any, Action: Any>(
    initialState: State,
    reducer: Reducer<State, Action>
) {
    internal val builder = StoreBuilder(
        initialState,
        reducer
    )

    /**
     * Give this store a name. This is useful for debugging or reporting purposes.
     *
     * For example, the global performance monitors will see this value. It may also come in handy during debugging
     * sessions.
     */
    fun name(value: String) {
        builder.storeName(value)
    }

    /**
     * Adds middleware to the store configuration.
     *
     * Middleware can intercept actions before they reach the reducer, enabling tasks such as logging, side effects, or modifying actions.
     *
     * @param middleware Vararg of middleware to be added to the store.
     */
    fun add(vararg middleware: Middleware<State, Action>) {
        builder.add(*middleware)
    }

    /**
     * Adds enhancers to the store configuration.
     *
     * Enhancers can modify or extend the store's behavior, such as adding functionality or altering how the store processes actions.
     *
     * @param enhancers Vararg of enhancers to be added to the store.
     */
    fun add(vararg enhancers: Enhancer<State, Action>) {
        builder.add(*enhancers)
    }

    /**
     * Sets a custom store creator for the store.
     *
     * The store creator is responsible for creating the final store instance, allowing customization of how the store is built.
     *
     * @param creator The custom store creator to be used for building the store.
     */
    fun creator(creator: StoreCreator<State, Action>) {
        builder.storeCreator(creator)
    }

    /**
     * Adds a [BufferEnhancer] to the store, which accumulates a specified number of actions
     * in a buffer before dispatching them all at once. This can help in reducing the number
     * of state updates and improving performance by batching actions together.
     *
     * @param size The size of the buffer. Once the buffer is full, all actions in the buffer
     *             are dispatched to the store at once. Must be greater than zero.
     */
    fun buffer(size: Int) {
        builder.add(
            BufferEnhancer(size)
        )
    }

    /**
     * Adds a [BatchEnhancer] to the store, which accumulates actions over a specified duration
     * before dispatching them all at once. This can help in reducing the frequency of state
     * updates by batching actions together based on time intervals.
     *
     * **Please Note:** Batches will not be dispatched immediately. They will only be dispatched at the
     * next call to dispatch once the duration is exceeded.
     *
     * @param duration The duration for which actions are accumulated before being dispatched.
     *                 Once this duration is exceeded, all actions in the batch are dispatched
     *                 to the store at once.
     */
    fun batched(duration: Duration) {
        builder.add(BatchEnhancer(duration))
    }

    /**
     * Adds a [LoggingEnhancer] to the store, which logs every action that is dispatched. This
     * can be useful for debugging or monitoring purposes to track how actions are flowing
     * through the store.
     *
     * @param logger A function that takes an `Action` and logs it. The function is called
     *               every time an action is dispatched to the store.
     */
    fun log(logger: (Action) -> Unit) {
        builder.add(LoggingEnhancer(logger))
    }

    /**
     * Adds a [PerformanceEnhancer] to the store, which monitors the performance of action
     * dispatches by measuring the time it takes to process each action. This can be useful
     * for identifying slow actions and optimizing the performance of the store.
     *
     * @param monitor A function that takes `PerformanceData`, which contains the store name
     *                and the duration of the action dispatch, and logs or processes this data
     *                as needed.
     */
    fun monitorPerformance(monitor: (PerformanceData<Action>) -> Unit) {
        builder.add(PerformanceEnhancer(monitor))
    }

    /**
     * Adds a [DebounceEnhancer] to the store's builder, which debounces actions based on the specified duration.
     * This means that actions will only be dispatched if the specified amount of time has passed since the last dispatched action.
     * If actions are dispatched more frequently than the debounce duration, only the first action in the burst will be processed.
     *
     * This function is useful in scenarios where you want to limit the rate at which actions are processed,
     * such as preventing excessive updates in response to rapid user input.
     *
     * @param duration The debounce duration. Actions will only be dispatched if this amount of time
     *                 has passed since the last dispatched action. The duration must be greater than zero.
     *
     * @throws IllegalArgumentException if `duration` is less than or equal to zero.
     */
    fun debounce(duration: Duration) {
        builder.add(DebounceEnhancer(duration))
    }

    /**
     * Adds a `GuardEnhancer` to the store, which blocks actions that fail the [isAuthorized] check.
     *
     * @param isAuthorized A suspend function that returns `true` if the action should be dispatched, or `false` if it should be blocked.
     */
    fun guard(isAuthorized: suspend (Action) -> Boolean) {
        builder.add(GuardEnhancer(isAuthorized))
    }
}