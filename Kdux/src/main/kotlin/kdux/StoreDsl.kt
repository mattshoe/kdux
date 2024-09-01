package kdux

import kdux.dsl.StoreDslMenu
import kdux.tools.*
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Middleware
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store

/**
 * Creates and configures a [Store] using a DSL.
 *
 * This function provides a DSL for configuring a [Store] with an initial state, a reducer,
 * and optional middleware, enhancers, or a custom store creator.
 *
 * @param State The type representing the state managed by the store.
 * @param Action The type representing the actions that can be dispatched to the store.
 * @param initialState The initial state of the store.
 * @param reducer The reducer that handles actions and updates the state.
 * @param configuration A lambda function used to configure the store, such as adding middleware, enhancers, or a custom store creator.
 * @return A fully configured [Store] instance.
 */
fun <State: Any, Action: Any> store(
    initialState: State,
    reducer: Reducer<State, Action>,
    configuration: StoreDslMenu<State, Action>.() -> Unit = { }
): Store<State, Action> {
    return StoreDslMenu(initialState, reducer)
        .apply(configuration)
        .builder
        .apply {
            KduxGlobal.loggers.forEach {
                add(LoggingEnhancer(it))
            }
            KduxGlobal.performanceMonitors.forEach {
                add(PerformanceEnhancer(it))
            }
        }
        .build()
}


/**
 * A DSL utility that creates a [Middleware] from a given function. This allows you to define
 * middleware logic inline without needing to create a separate class.
 *
 * The middleware function intercepts actions as they are dispatched to the store, allowing you to
 * perform side effects, modify the action, or block the action from reaching the reducer.
 *
 * @param function A suspend function that takes three parameters:
 * - [store]: The [Store] instance managing the state and actions.
 * - [action]: The action being dispatched.
 * - [next]: A suspend function representing the next middleware or reducer in the chain. Calling `next(action)` passes the action to the next stage.
 *
 * @return A [Middleware] instance that applies the provided function to intercept and process actions.
 */
fun <State: Any, Action: Any> middleware(
    function: suspend (
        store: Store<State, Action>,
        action: Action,
        next: suspend (Action
        ) -> Unit
    ) -> Unit
): Middleware<State, Action> {
    return object : Middleware<State, Action> {
        override suspend fun apply(
            store: Store<State, Action>,
            action: Action,
            next: suspend (Action) -> Unit
        ) {
            function.invoke(store, action, next)
        }
    }
}

/**
 * A DSL utility that creates an [Enhancer] from a given function. This allows you to define
 * enhancer logic inline without needing to create a separate class.
 *
 * The enhancer function modifies the behavior of the store by wrapping it with additional functionality.
 * This can include altering how actions are dispatched, adding new methods, or modifying how the state is accessed.
 *
 * @param function A function that takes a [Store] as a parameter and returns a modified [Store].
 *
 * @return An [Enhancer] instance that applies the provided function to modify or extend the store's behavior.
 */
fun <State: Any, Action: Any> enhancer(
    function: (store: Store<State, Action>) -> Store<State, Action>
): Enhancer<State, Action> {
    return object : Enhancer<State, Action> {
        override fun enhance(store: Store<State, Action>): Store<State, Action> {
            return function.invoke(store)
        }
    }
}