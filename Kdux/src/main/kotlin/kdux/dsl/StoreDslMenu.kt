package kdux.dsl

import kdux.tools.*
import org.mattshoe.shoebox.kdux.*
import org.mattshoe.shoebox.kdux.StoreBuilder
import java.io.InputStream
import java.io.OutputStream
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

    /**
     * Adds a [ThrottleEnhancer] to the store, which limits the rate at which actions are dispatched.
     *
     * Actions are dispatched at most once per specified [interval]. If actions are dispatched more frequently,
     * they are queued and dispatched sequentially, with each dispatch delayed by the [interval].
     *
     * This function is useful in scenarios where you want to prevent actions from being dispatched too frequently,
     * such as limiting the rate of updates in response to rapid user input or other high-frequency events.
     *
     * @param interval The minimum time interval between consecutive action dispatches. If an action is dispatched
     *                 before this interval has passed since the last action, it will be delayed until the interval has elapsed.
     *
     * @throws IllegalArgumentException if `interval` is less than or equal to zero.
     */
    fun throttle(interval: Duration) {
        builder.add(ThrottleEnhancer(interval))
    }

    /**
     * Adds a [FailSafeEnhancer] to the store, which handles errors that occur during dispatch.
     *
     * If an error occurs while processing an action, the provided [onError] function is invoked.
     * This function can be used to perform error handling, logging, or dispatching recovery actions.
     *
     * This function guarantees that any exceptions thrown during dispatch are caught.
     *
     * @param onError A suspend function that is called when an error occurs during action dispatching.
     * It receives the current state, the action that caused the error, the error itself, and a dispatch function that can be used to
     * dispatch a new action to the store as a recovery mechanism.
     *
     * Example usage:
     * ```kotlin
     * store(...) {
     *     onError { state, action, error, dispatch ->
     *         // Log the error and dispatch a recovery action if necessary
     *         println("Error occurred: ${error.message}")
     *         if (action is ImportantAction) {
     *             dispatch(RecoveryAction)
     *         }
     *     }
     * }
     * ```
     */
    fun onError(
        onError: suspend (
            state: State,
            action: Action,
            error: Throwable,
            dispatch: suspend (Action) -> Unit
        ) -> Unit
    ) {
        builder.add(FailSafeEnhancer(onError))
    }

    /**
     * Adds a `TimeoutEnhancer` to the store's builder, which enforces a time limit on the dispatching of actions.
     * If an action is not processed within the specified [value] duration, the dispatch is canceled, and a `TimeoutCancellationException` is thrown.
     *
     * This is useful for ensuring that actions are processed within a reasonable time frame, avoiding long-running operations from blocking the store.
     *
     * @param value The maximum duration allowed for processing an action. If the action is not processed
     * within this duration, the dispatch is canceled. The duration must be greater than zero.
     *
     * @throws IllegalArgumentException if the `value` is less than or equal to zero.
     */
    fun timeout(value: Duration) {
        builder.add(TimeoutEnhancer(value))
    }

    /**
     * Adds a [PersistenceEnhancer] to the store, enabling automatic persistence and restoration of the store's state.
     * This function is designed to make it easy to persist and restore the state of your store across application restarts
     * or other lifecycle events.
     *
     * The state is serialized and stored using the provided [serializer], and restored using the [deserializer]. The state
     * is saved to a file identified by the specified [key]. The [key] should be unique for each store to avoid conflicts.
     *
     * ### Important Details:
     *
     * The [key] parameter is used to determine the filename or unique identifier for the stored state. You must ensure that the
     * key is unique if multiple stores are being persisted, to avoid conflicts. The [key] can be used to associate user-data to avoid
     * exposing the wrong user's data to another, by appending a user-id or otherwise to the key.
     *
     * @param key A unique identifier for the persisted state. This is used to determine the filename or key under which the
     *     state is stored.
     * @param serializer A suspend function that serializes the state into the provided [OutputStream]. It must write the
     *     state in a format that can later be deserialized.
     * @param deserializer A function that deserializes the state from the provided [InputStream]. It must return a state
     *     object that matches the type of the store's state.
     * @param onError A function used to notify you when an uncaught error occurs during serialization or deserialization.
     */
    fun persist(
        key: String,
        serializer: suspend (state: State, outputStream: OutputStream) -> Unit,
        deserializer: (inputStream: InputStream) -> State,
        onError: (state: State?, error: Throwable) -> Unit = { _, _ -> }
    ) {
        builder.add(
            PersistenceEnhancer(
                key,
                serializer,
                deserializer,
                onError
            )
        )
    }
}