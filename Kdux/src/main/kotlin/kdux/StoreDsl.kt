package org.mattshoe.shoebox.kdux

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
        .build()
}


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
     * Adds middleware to the store configuration.
     *
     * Middleware can intercept actions before they reach the reducer, enabling tasks such as logging, side effects, or modifying actions.
     *
     * @param middleware Vararg of middleware to be added to the store.
     */
    fun middleware(vararg middleware: Middleware<State, Action>) {
        builder.add(*middleware)
    }

    /**
     * Adds enhancers to the store configuration.
     *
     * Enhancers can modify or extend the store's behavior, such as adding functionality or altering how the store processes actions.
     *
     * @param enhancers Vararg of enhancers to be added to the store.
     */
    fun enhancers(vararg enhancers: Enhancer<State, Action>) {
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
}