package org.mattshoe.shoebox.kdux

/**
 * A factory interface for creating instances of a [Store]. This interface abstracts the creation
 * logic for a [Store], allowing for custom store initialization, such as applying enhancers,
 * middleware, or any other custom logic required during store instantiation.
 *
 * This interface is typically used in scenarios where the store creation process involves more than
 * just calling a constructor, for example, when creating a store with predefined enhancers or
 * middleware.
 *
 * @param State The type of state that the created store will manage. It must be a non-nullable type (`Any`).
 * @param Action The type of actions that the created store will handle. It must be a non-nullable type (`Any`).
 */
interface StoreCreator<State : Any, Action : Any> {

    /**
     * Creates and returns a new instance of a [Store]. The store manages the application's state and
     * processes actions to update the state using middleware and a reducer.
     *
     * Implementations of this function should handle the full instantiation of the store, including
     * setting up initial state, applying middleware, and any other initialization logic required
     * for the specific application.
     *
     * @return A new instance of a [Store] that is ready to manage state and handle actions.
     */
    fun createStore(): Store<State, Action>
}