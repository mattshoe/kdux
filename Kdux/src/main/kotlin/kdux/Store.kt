package org.mattshoe.shoebox.kdux

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the core of a Redux-like state management system. A `Store` holds a state,
 * allows state transitions by dispatching actions, and provides a way to observe state changes over time.
 *
 * The store is responsible for managing the current state, processing actions that
 * represent events or intentions in the system, and applying the appropriate state updates by passing
 * the action to a [Reducer]. The store exposes the state as a [StateFlow], which allows reactive components
 * to observe and react to state changes.
 *
 * ## Purpose of the Store
 *
 * The store acts as the **single source of truth** for a particular state. It encapsulates the entire state within a
 * centralized container. By using the store, the state becomes:
 *
 * **Predictable**: State transitions only occur in response to actions dispatched through the store, ensuring
 * that all state changes are intentional and controlled.
 *
 * **Observable**: The store exposes the state as a [StateFlow], allowing reactive components
 * to observe and react to state changes in a consistent and efficient manner.
 *
 * **Centralized**: All state transitions are handled by a single entity (the store), which makes it easier to
 * reason about the application's behavior, debug issues, and track state changes over time.
 *
 * ## Key Responsibilities
 *
 * **Hold State**: The store maintains the current state of the application. This state is immutable and can only
 * be updated by dispatching actions through the store.
 *
 * **Dispatch Actions**: The store accepts actions, which represent events or commands in the application. These
 * actions are processed by middleware (if present) and then sent to the reducer, which determines how the state
 * should change in response to the action.
 *
 * **Observe State Changes**: The store exposes the current state as a [StateFlow]. External components can collect
 * this flow to observe state changes and automatically update themselves when the state changes.
 *
 * ## Store Lifecycle
 *
 * The lifecycle of the store revolves around the continuous cycle of actions being dispatched and state being updated:
 *
 * 1. **Initial State**: When the store is created, it initializes with an initial state. This guarantees that any observers
 *    will ALWAYS have a value to react to, and that the store always has a state at any given time.
 *
 * 2. **Dispatch Actions**: Actions are dispatched by various parts of the application (e.g., user interactions,
 *    network responses, external events). These actions are passed through middleware (if any) and then sent to the reducer.
 *
 * 3. **Reducer Processing**: The reducer processes the action and produces a new state based on the current state
 *    and the dispatched action.
 *
 * 4. **State Update**: Once the reducer returns the new state, the store updates its internal state and emits the new
 *    state to any collectors of the [StateFlow].
 *
 * 5. **State Observation**: Reactive components that are collecting the [StateFlow] receive the new state and update
 *    accordingly.
 *
 * ## Example Use Case
 *
 * Imagine a simple counter application where the state represents the current count. The store would hold the count state
 * and allow components to dispatch actions to increment or decrement the count:
 *
 * ```kotlin
 * data class CounterState(val count: Int)
 *
 * sealed class CounterAction {
 *     object Increment : CounterAction()
 *     object Decrement : CounterAction()
 * }
 *
 * class LoggingMiddleware : Middleware<CounterState, CounterAction> {
 *     val log = mutableListOf<String>()
 *     override suspend fun apply(store: Store<CounterState, CounterAction>, action: CounterAction, next: suspend (CounterAction) -> Unit) {
 *         log.add("Logging: $action")
 *         next(action)
 *     }
 * }
 *
 * class CounterReducer : Reducer<CounterState, CounterAction> {
 *     override suspend fun reduce(state: CounterState, action: CounterAction): CounterState {
 *         return when (action) {
 *             is CounterAction.Increment -> state.copy(count = state.count + 1)
 *             is CounterAction.Decrement -> state.copy(count = state.count - 1)
 *         }
 *     }
 * }
 *
 * // Create the store with the initial state and the reducer
 * class CounterStore : Store<CounterState, CounterAction> {
 *     by StoreBuilder(
 *         initialState = CounterState(0),
 *         reducer = CounterReducer()
 *     )
 *         .add(LoggingMiddleware())
 *         .build()
 * }
 *
 * val store = CounterStore()
 *
 * // Dispatch an action to increment the count
 * store.dispatch(CounterAction.Increment)
 *
 * // Observe the state and print it
 * store.state.collect { state ->
 *     println("Current count: ${state.count}")
 * }
 * ```
 *
 * In this example, the store holds the current count as part of the `CounterState`. Actions like `Increment` and `Decrement`
 * are dispatched to the store, processed by the reducer, and the state is updated. Any components observing the store's
 * state would automatically receive the new state and update accordingly.
 *
 * ## Immutability and State Updates
 *
 * The state held by the store is immutable. This means that the state is not modified directly; instead, when an action is
 * dispatched, a new state object is created by the reducer and returned to the store. The store then replaces the old state
 * with the new state and emits it to any observers. This ensures that state transitions are predictable and traceable.
 *
 * ## Observing State Changes
 *
 * The state is exposed as a [StateFlow], which is a reactive data stream that components can collect. When the state changes,
 * the flow automatically emits the new state to all collectors. This makes the store an ideal tool for building reactive
 * user interfaces where components automatically update themselves in response to state changes.
 *
 * ## Store and Middleware
 *
 * Middleware can be used to intercept actions as they are dispatched to the store. Middleware can modify actions, handle side
 * effects (such as network requests), or block actions from reaching the reducer. The store ensures that middleware is processed
 * in the correct order before the reducer handles the action and updates the state.
 *
 * ## Conclusion
 *
 * The `Store` interface serves as the central hub for managing application state, processing actions, and broadcasting state
 * updates. It provides a predictable and observable system for handling state transitions, making it easier to reason about
 * the application's behavior and to build reactive components that automatically update in response to state changes.
 *
 * @param State The type of state that the store manages. This represents the application state or a specific portion of the
 *    application's state. It must be a non-nullable type (`Any`).
 * @param Action The type of actions that the store processes. Actions represent events or commands that describe something
 *    that happened or should happen in the application. It must be a non-nullable type (`Any`).
 */
interface Store<State : Any, Action : Any> {
    /**
     * The stream providing the current state of the store, exposed as a [StateFlow]. This flow is reactive, meaning that it can be
     * collected by external components to observe state changes over time. Every time the state is updated as a
     * result of an action being processed, the new state is emitted to all collectors of the flow.
     *
     * @return A [StateFlow] containing the current state of the store. This flow will emit the new state whenever
     *    it changes, allowing components to reactively update themselves in response to state changes.
     */
    val state: Flow<State>

    /**
     * The current state of the store. This property provides direct access to the most recent state
     * managed by the store without needing to collect the [state] flow. It represents the latest
     * snapshot of the application's state.
     *
     * This is particularly useful when you need to access the state synchronously, without waiting for
     * flow emissions. However, unlike the [state] flow, `currentState` does not notify observers of state changes.
     *
     * @return The most recent state of type [State] managed by the store.
     */
    val currentState: State

    /**
     * Dispatches an action to the store. The action represents an event or command that should trigger a state
     * transition within the store. When an action is dispatched, it is first passed through any middleware (if present),
     * and then sent to the reducer. The reducer processes the action and returns a new state, which the store then
     * updates and emits to all observers.
     *
     * @param action The action to be dispatched. This action represents an event or intent within the application that
     *    should cause a state transition.
     * @throws Exception If there is an error while processing the action or updating the state.
     */
    suspend fun dispatch(action: Action)

    companion object {
        fun <State : Any, Action : Any> Builder(
            initialState: State,
            reducer: Reducer<State, Action>
        ): StoreBuilder<State, Action> {
            return StoreBuilder(initialState, reducer)
        }
    }
}