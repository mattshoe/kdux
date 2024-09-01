package org.mattshoe.shoebox.kdux

/**
 * A builder class for constructing instances of a [Store] in a Redux-like architecture. The `StoreBuilder`
 * allows for the configuration of middleware, enhancers, and a custom store creation function, enabling
 * flexible and customizable store initialization.
 *
 * This class follows the standard builder pattern, allowing for the chaining of methods to add middleware,
 * enhancers, and to define a custom store creation process. Once configured, the `build()` function can be
 * called to create the fully constructed [Store].
 *
 * @param State The type of state that the store will manage.
 * @param Action The type of actions that the store will handle.
 * @param initialState The initial state that the store will start with.
 * @param reducer The reducer that will handle state transitions based on dispatched actions.
 */
internal class StoreBuilder<State: Any, Action: Any>(
    private val initialState: State,
    private val reducer: Reducer<State, Action>
) {
    private val middlewares = mutableListOf<Middleware<State, Action>>()
    private val enhancers = mutableListOf<Enhancer<State, Action>>()
    private var storeCreatorLambda: (() -> Store<State, Action>)? = null
    private var storeCreator: StoreCreator<State, Action>? = null
    private var storeName: String? = null

    fun storeName(value: String) {
        storeName = value
    }

    /**
     * Adds a middleware to the builder, allowing it to be included in the store's processing pipeline.
     * This function can be chained.
     *
     * @param middleware The middleware to be added to the builder.
     * @return The current [StoreBuilder] instance, allowing for method chaining.
     */
    operator fun plus(middleware: Middleware<State, Action>): StoreBuilder<State, Action> {
        middlewares.add(middleware)
        return this
    }

    /**
     * Adds one or more middleware to the builder. This function can be chained.
     *
     * @param middlewares Vararg parameter for adding multiple middleware at once.
     * @return The current [StoreBuilder] instance, allowing for method chaining.
     */
    fun add(vararg middlewares: Middleware<State, Action>): StoreBuilder<State, Action> {
        this.middlewares.addAll(middlewares)
        return this
    }

    /**
     * Adds an enhancer to the builder, allowing it to modify or extend the behavior of the store.
     * This function can be chained.
     *
     * @param enhancer The enhancer to be added to the builder.
     * @return The current [StoreBuilder] instance, allowing for method chaining.
     */
    operator fun plus(enhancer: Enhancer<State, Action>): StoreBuilder<State, Action> {
        enhancers.add(enhancer)
        return this
    }

    /**
     * Adds one or more enhancers to the builder. This function can be chained.
     *
     * @param enhancers Vararg parameter for adding multiple enhancers at once.
     * @return The current [StoreBuilder] instance, allowing for method chaining.
     */
    fun add(vararg enhancers: Enhancer<State, Action>): StoreBuilder<State, Action> {
        this.enhancers.addAll(enhancers)
        return this
    }

    /**
     * Sets a custom store creation function. This allows for creating a custom store instance rather than
     * relying on the default store creation logic.
     *
     * @param creator A lambda function that returns a custom [Store] instance.
     * @return The current [StoreBuilder] instance, allowing for method chaining.
     */
    fun store(creator: () -> Store<State, Action>): StoreBuilder<State, Action> {
        this.storeCreatorLambda = creator
        return this
    }

    /**
     * Sets a custom store creation function using a [StoreCreator]. This allows for creating a custom store
     * instance using a predefined [StoreCreator] interface implementation, rather than relying on a lambda function.
     *
     * @param creator A [StoreCreator] instance that defines the custom store creation logic.
     * @return The current [StoreBuilder] instance, allowing for method chaining.
     */
    fun storeCreator(creator: StoreCreator<State, Action>): StoreBuilder<State, Action> {
        this.storeCreator = creator
        return this
    }

    /**
     * Builds and returns the fully configured [Store] instance. This method applies all the configured
     * middleware and enhancers in the order they were added.
     *
     * @return A fully constructed and enhanced [Store] instance.
     */
    fun build(): Store<State, Action> {
        var store = storeCreator?.createStore()
            ?: this.storeCreatorLambda?.invoke()
            ?: DefaultStore(
                storeName,
                initialState,
                reducer,
                middlewares
            )

        // Apply all enhancers in the order they were created.
        enhancers.forEach { enhancer ->
            store = enhancer.enhance(store)
        }

        return store
    }
}