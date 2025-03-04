package kdux

import kdux.dsl.StoreDslMenu
import kdux.tools.*
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Middleware
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store
import kotlin.reflect.KClass

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
            if (KduxMenu.globalErrorHandlers.isNotEmpty()) {
                add(
                    FailSafeEnhancer { state, action, error, dispatch ->
                        KduxMenu.globalErrorHandlers.forEach { onError ->
                            onError(state, action, error)
                        }
                    }
                )
            }
            if (KduxMenu.loggers.isNotEmpty()) {
                add(
                    LoggingEnhancer { action ->
                        KduxMenu.loggers.forEach { log ->
                            log(action)
                        }
                    }
                )
            }
            if (KduxMenu.performanceMonitors.isNotEmpty()) {
                add(
                    PerformanceEnhancer { data ->
                        KduxMenu.performanceMonitors.forEach { monitor ->
                            monitor(data)
                        }
                    }
                )
            }
            if (KduxMenu.globalGuards.isNotEmpty()) {
                add(
                    GuardEnhancer { action ->
                        KduxMenu.globalGuards.all { it(action) }
                    }
                )
            }
        }
        .build()
}

/**
 * A DSL utility that creates a [Reducer] from a given function. This allows you to define reducer logic
 * without needing to create a separate class.
 *
 * The reducer function is responsible for taking the current state and an action, and then returning a new
 * state based on the action. Reducers should be pure functions, meaning they do not cause side effects
 * and always produce the same output given the same inputs.
 *
 * @param State The type representing the state managed by the reducer.
 * @param Action The type representing the actions that can be dispatched to the store.
 * @param function A suspend function that takes the current state and an action, and returns the new state.
 *
 * @return A [Reducer] instance that applies the provided function to reduce the state.
 */
fun <State: Any, Action: Any> reducer(
    function: suspend (state: State, action: Action) -> State
): Reducer<State, Action> {
    return object : Reducer<State, Action> {
        override suspend fun reduce(state: State, action: Action): State {
            return function(state, action)
        }
    }
}

/**
 * A DSL utility that creates a new [Reducer] that will execute the given [reducers] sequentially, passing the updated
 * state from one to the next. Each reducer processes the given action in order.
 *
 * Keep in mind that ALL of the given [reducers] will be executed for every single dispatch. These will execute in
 * exactly the order passed into the method, so order does matter here.
 *
 * @param reducers The reducers to be applied in sequence.
 * @return A single reducer that applies all provided reducers in order.
 */
fun <State: Any, Action: Any> chainReducers(
    vararg reducers: Reducer<State, Action>
): Reducer<State, Action> {
    return object : Reducer<State, Action> {
        override suspend fun reduce(state: State, action: Action): State {
            return reducers.fold(state) { accState, reducer ->
                reducer.reduce(accState, action)
            }
        }
    }
}

/**
 * A DSL utility that creates a new [Reducer] that will select only one reducer to execute, based on the action type.
 * If no matching reducer is found, it uses the [fallback] reducer.
 *
 * Keep in mind that only ONE reducer will ever execute for a given action.
 *
 * @param assigned A list of action-type-to-reducer mappings.
 * @param fallback The reducer to use if no matching reducer is found.
 * @return A reducer that delegates to the appropriate reducer based on action type.
 */
fun <State: Any, Action: Any> assignReducers(
    vararg assigned: Pair<KClass<out Action>, Reducer<State, Action>>,
    fallback: Reducer<State, Action>
): Reducer<State, Action> {
    return object : Reducer<State, Action> {
        override suspend fun reduce(state: State, action: Action): State {
            val reducer: Reducer<State, Action> = assigned.firstOrNull { it.first == action::class }?.second ?: fallback
            return reducer.reduce(state, action)
        }
    }
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