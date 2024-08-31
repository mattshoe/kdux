package org.mattshoe.shoebox.kdux

/**
 * An interface that defines an `Enhancer`, which modifies or extends the behavior of a [Store].
 *
 * Enhancers are used to add new capabilities or to fundamentally change how a store operates by wrapping
 * the store and modifying its internal behavior. Enhancers can intercept actions before they reach the reducer,
 * batch multiple actions together, track performance metrics, add additional methods to the store itself, or any
 * other modification that may be necessary.
 *
 * An Enhancer is just a "wrapper" around a given [Store] that changes its functionality.
 *
 * ## Enhancers vs. Middleware
 *
 * While middleware is used to intercept and manipulate actions as they are dispatched (typically on a per-action basis),
 * an `Enhancer` operates at a higher level by modifying the store itself. An enhancer can change how the store's `dispatch`
 * and `state` properties behave globally, introducing new logic that affects the entire lifecycle of action processing and state management.
 *
 * Middleware handles actions one at a time and passes them through a chain, while an enhancer can modify how the store as a whole handles
 * multiple actions, alters the flow of state management, or extend the store with new methods and behaviors that can't be achieved by
 * middleware alone.
 *
 * Enhancers are typically used when you need to add complex behavior to a store that cannot be easily achieved with middleware.
 * Some examples include:
 * * **Action Batching**: Collect multiple actions into a batch and only update the state once after all actions have been processed.
 * * **Time-Travel Debugging**: Add the ability to move backward and forward through different states by replaying actions.
 * * **State Persistence**: Automatically save and restore the state of the store to and from local storage or a remote server.
 * * **Enhanced Performance Metrics**: Measure and log the time it takes for each action to be processed by the store, including
 *   how long reducers take to execute.
 *
 * Enhancers allow you to apply cross-cutting concerns that are beyond the scope of what middleware can achieve, and can
 * fundamentally change how the store behaves.
 *
 * ## How Enhancers Work
 *
 * An enhancer takes an existing store and returns a new store with modified or extended behavior. The enhancer wraps the
 * store, potentially modifying the `dispatch` function to intercept actions, changing how the state is accessed, or adding
 * new capabilities to the store, such as batching, persistence, or other cross-cutting concerns.
 *
 * Multiple enhancers can be chained together to apply layers of behavior modification to a store. Each enhancer wraps the
 * store created by the previous enhancer, allowing you to compose complex behavior in a modular and reusable way.
 *
 * ## Example
 *
 * A common example of an enhancer is an `ActionBatchingEnhancer`, which allows multiple actions to be dispatched in a
 * batch and processed together, rather than individually. This can be useful in scenarios where many actions need to be
 * processed at once, but you only want to update the state once after all actions have been processed.
 *
 * ```
 *  class ActionBatchingEnhancer<State: Any, Action: Any>: Enhancer<State, Action> {
 *      override fun enhance(store: Stare<State>, Action>): Store<State, Action> {
 *          return object : Store<State, Action> {
 *              private val batchedActions = mutableListOf<Action>()
 *              private var isBatching = false
 *
 *              override val state: StateFlow<State> = store.state
 *
 *              override suspend fun dispatch(action: Action) {
 *                  if (isBatching) {
 *                      batchedActions.add(action)
 *                  } else {
 *                      store.dispatch(action)
 *                  }
 *              }
 *
 *              suspend fun startBatching() {
 *                  isBatching = true
 *              }
 *
 *              suspend fun endBatching() {
 *                  isBatching = false
 *                  for (action in batchedActions) {
 *                      store.dispatch(action)
 *                  }
 *                  batchedActions.clear()
 *              }
 *          }
 *      }
 *  }
 * ```
 *
 * In this example, the `ActionBatchingEnhancer` modifies the store to enable batching. When `startBatching()` is called,
 * actions are collected but not immediately processed. Once `endBatching()` is called, all the batched actions are dispatched
 * at once, allowing for a single state update after all actions have been processed.
 *
 * ## Enhancer Lifecycle
 * 1. **Store Creation**: The enhancer is applied when the store is created (usually via a builder or factory). The enhancer wraps the store with its extended functionality.
 * 2. **Action Dispatch**: The enhanced store processes actions according to the logic provided by the enhancer. This could involve batching actions, modifying the dispatch process, or adding additional steps before or after an action is processed.
 * 3. **State Observation**: The enhanced store may modify how the state is accessed or introduce new behavior when the state changes. This allows for features like automatic persistence or complex caching strategies.
 * 4. **Extended Capabilities**: The enhancer may introduce new methods or capabilities to the store, such as time-travel debugging, batching, persistence, etc.
 *
 * @param State The type of state that the store manages. This represents the application's state or a subset of the application's state.
 * @param Action The type of actions that the store handles. Actions represent events or commands that describe something that happened or should happen in the application.
 */

interface Enhancer<State : Any, Action : Any> {
    /**
     * Enhances the behavior of a given [Store] by wrapping it with additional functionality. The enhanced
     * store can modify how actions are dispatched, how state is accessed, and add new capabilities to
     * the store's API.
     *
     * The returned store should behave like the original store but may have additional features, such as
     * batching, logging, state persistence, or other cross-cutting concerns.
     *
     * @param store The original [Store] to be enhanced. This store will be wrapped by the enhancer, adding new
     * behavior and processing actions. The enhancer wraps this store with additional functionality.
     * @return A NEW [Store] that extends or modifies the behavior of the original store. This store may
     * include new methods, modified dispatch behavior, or other enhancements that enhance the
     * store's functionality.
     *
     * ## Notes for Implementation:
     * * The enhanced store typically wraps the original store's `dispatch` and `state` properties, adding
     * custom logic where needed. This can include collecting metrics, batching actions, or altering the
     * way actions are processed.
     * * Multiple enhancers can be chained together to compose complex behavior across the store.
     */
    fun enhance(store: Store<State, Action>): Store<State, Action>
}






