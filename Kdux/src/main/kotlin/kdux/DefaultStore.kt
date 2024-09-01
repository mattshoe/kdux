package org.mattshoe.shoebox.kdux

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Default implementation of the [Store] interface that manages the state and handles
 * dispatched actions by passing them through the middleware chain and then reducing them into a new state.
 * The store updates its state flow, allowing observers to track state changes over time.
 *
 * @param State The type of state that this store holds. It must be a non-nullable type (`Any`).
 * @param Action The type of actions that can be dispatched to the store. It must be a non-nullable type (`Any`).
 * @property initialState The initial state of the store when it is created.
 * @property reducer The reducer function that determines how the state should change when an action is dispatched.
 * @property middlewares A list of middlewares that intercept and process the actions before they reach the reducer.
 */
internal class DefaultStore<State: Any, Action: Any>(
    customName: String? = null,
    private val initialState: State,
    private val reducer: Reducer<State, Action>,
    private val middlewares: List<Middleware<State, Action>> = emptyList()
) : Store<State, Action> {
    private val _state = MutableStateFlow(initialState)

    override val name = customName ?: super.toString()

    override val state: Flow<State> = _state.asStateFlow()
    override val currentState: State
        get() = _state.value

    override suspend fun dispatch(action: Action) {
        processMiddleware(0, action)
    }

    override fun toString(): String {
        return name
    }

    /**
     * Recursively processes the middleware chain. Each middleware can intercept, modify, or block
     * the action before it is passed to the next middleware or the reducer.
     *
     * This function is called recursively to process each middleware in sequence. Once all middleware
     * has been processed, the action is passed to the reducer to update the state.
     *
     * @param index The current index in the middleware list. Used to determine which middleware to apply next.
     * @param action The action currently being processed by the middleware chain.
     */
    private suspend fun processMiddleware(index: Int, action: Action) {
        if (index < middlewares.size) {
            val nextMiddleware = middlewares[index]
            nextMiddleware.apply(this, action) { nextAction ->
                processMiddleware(index + 1, nextAction)
            }
        } else {
            // No more middleware, reduce the action and update the state
            reduceAndUpdate(action)
        }
    }

    /**
     * Reduces the current state based on the dispatched action using the provided reducer function.
     * The reducer is responsible for determining how the state should change based on the given action.
     *
     * This method updates the internal state and notifies any observers of the new state.
     *
     * @param action The action to be reduced. This action determines how the state will be updated.
     */
    private suspend fun reduceAndUpdate(action: Action) {
        _state.update {
            reducer.reduce(_state.value, action)
        }
    }
}

private data class DispatchEvent<Action: Any>(
    val action: Action,
    val completableDeferred: CompletableDeferred<DispatchResult>
)

data class DispatchResult(
    val error: Throwable?
)