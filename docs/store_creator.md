# StoreCreator in Kdux

The `StoreCreator` interface in Kdux serves as a factory for creating instances of a `Store`. This interface abstracts
the creation logic for a `Store`, allowing for custom store initialization, such as applying enhancers, middleware, or
any other custom logic required during store instantiation.

## Purpose

The `StoreCreator` interface is typically used in scenarios where the store creation process involves more than just
calling a constructor. For example, it is useful when creating a store with predefined enhancers, middleware, or any
other custom setup that the application might require.

## Key Concepts

- **Factory Interface**: `StoreCreator` acts as a factory, meaning it encapsulates the logic needed to create a fully
  initialized `Store` instance.
- **Customization**: This interface allows for a high degree of customization during the store creation process. You can
  use it to apply middleware, enhancers, or any other initialization logic needed for your specific application.

## Method Summary

### `createStore()`

- **Description**: Creates and returns a new instance of a `Store`. The store manages the application's state and
  processes actions to update the state using middleware and a reducer.
- **Implementation Notes**: Implementations of this function should handle the full instantiation of the store,
  including setting up initial state, applying middleware, and any other initialization logic required for the specific
  application.
- **Returns**: A new instance of a `Store` that is ready to manage state and handle actions.

## Example Usage

```kotlin
// Define the state and actions
data class AppState(val count: Int = 0)

sealed class AppAction {
    object Increment : AppAction()
    object Decrement : AppAction()
}

// Create a simple reducer
class AppReducer : Reducer<AppState, AppAction> {
    override suspend fun reduce(state: AppState, action: AppAction): AppState {
        return when (action) {
            is AppAction.Increment -> state.copy(count = state.count + 1)
            is AppAction.Decrement -> state.copy(count = state.count - 1)
        }
    }
}

// Implement the StoreCreator interface
class CustomStoreCreator : StoreCreator<AppState, AppAction> {
    override fun createStore(): Store<AppState, AppAction> {
        return store(
            initialState = AppState(),
            reducer = AppReducer()
        ) {
            // Add any middleware or enhancers here if needed
            // Example: add(LoggingMiddleware())
        }
    }
}

// Usage
fun main() {
    // Create the store using the custom store creator
    val storeCreator = CustomStoreCreator()
    val store = storeCreator.createStore()

    // Dispatch actions
    store.dispatch(AppAction.Increment)
    store.dispatch(AppAction.Decrement)

    // Observe the state
    store.state.collect { state ->
        println("Current count: ${state.count}")
    }
}
```