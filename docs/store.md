# Store in Kdux

The `Store` interface represents the core of a Redux-like state management system in Kdux. A `Store` holds the state,
allows state transitions by dispatching actions, and provides a way to observe state changes over time.

## Purpose of the Store

The store acts as the **single source of truth** for a particular state. It encapsulates the entire state within a
centralized container. By using the store, the state becomes:

- **Predictable**: State transitions only occur in response to actions dispatched through the store, ensuring that all
  state changes are intentional and controlled.
- **Observable**: The store exposes the state as a `StateFlow`, allowing reactive components to observe and react to
  state changes in a consistent and efficient manner.
- **Centralized**: All state transitions are handled by a single entity (the store), which makes it easier to reason
  about the application's behavior, debug issues, and track state changes over time.

## Key Responsibilities

- **Hold State**: The store maintains the current state of the application. This state is immutable and can only be
  updated by dispatching actions through the store.
- **Dispatch Actions**: The store accepts actions, which represent events or commands in the application. These actions
  are processed by middleware (if present) and then sent to the reducer, which determines how the state should change in
  response to the action.
- **Observe State Changes**: The store exposes the current state as a `StateFlow`. External components can collect this
  flow to observe state changes and automatically update themselves when the state changes.

## Store Lifecycle

The lifecycle of the store revolves around the continuous cycle of actions being dispatched and state being updated:

1. **Initial State**: When the store is created, it initializes with an initial state. This guarantees that any
   observers will ALWAYS have a value to react to, and that the store always has a state at any given time.
2. **Dispatch Actions**: Actions are dispatched by various parts of the application (e.g., user interactions, network
   responses, external events). These actions are passed through middleware (if any) and then sent to the reducer.
3. **Reducer Processing**: The reducer processes the action and produces a new state based on the current state and the
   dispatched action.
4. **State Update**: Once the reducer returns the new state, the store updates its internal state and emits the new
   state to any collectors of the `StateFlow`.
5. **State Observation**: Reactive components that are collecting the `StateFlow` receive the new state and update
   accordingly.

## Example Use Case

Imagine a simple counter application where the state represents the current count. The store would hold the count state
and allow components to dispatch actions to increment or decrement the count:

```kotlin
data class CounterState(val count: Int)

sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
}

class LoggingMiddleware : Middleware<CounterState, CounterAction> {
    val log = mutableListOf<String>()
    
    override suspend fun apply(
        store: Store<CounterState, CounterAction>, 
        action: CounterAction, 
        next: suspend (CounterAction) -> Unit
    ) {
        log.add("Logging: $action")
        next(action)
    }
}

class CounterReducer : Reducer<CounterState, CounterAction> {
    override suspend fun reduce(state: CounterState, action: CounterAction): CounterState {
        return when (action) {
            is CounterAction.Increment -> state.copy(count = state.count + 1)
            is CounterAction.Decrement -> state.copy(count = state.count - 1)
        }
    }
}

// store variable with DSL
val counterStore = kdux.store(
    initialState = CounterState(),
    reducer = CounterReducer()
) {
    // add(PerformanceMonitorMiddleware())
    // add(DevToolsEnhancer())
}

// Store class by delegation
class CounterStore(
    initialState: CounterState = CounterState(0),
    reducer: Reducer<CounterState, CounterAction> = CounterReducer()
): Store<CounterState, CounterAction>
by kdux.store(
    initialState,
    reducer,
    {
        add(LoggingMiddleware())
    }
)

// Dispatch an action to increment the count
store.dispatch(CounterAction.Increment)

// Observe the state and print it
store.state.collect { state ->
    println("Current count: ${state.count}")
}
```

In this example, the store holds the current count as part of the `CounterState`. Actions like `Increment` and `Decrement`
are dispatched to the store, processed by the reducer, and the state is updated. Any components observing the store's
state would automatically receive the new state and update accordingly.

## Immutability and State Updates

The state held by the store is immutable. This means that the state is not modified directly; instead, when an action is
dispatched, a new state object is created by the reducer and returned to the store. The store then replaces the old
state with the new state and emits it to any observers. This ensures that state transitions are predictable and
traceable.

## Observing State Changes

The state is exposed as a `StateFlow`, which is a reactive data stream that components can collect. When the state
changes, the flow automatically emits the new state to all collectors. This makes the store an ideal tool for building
reactive user interfaces where components automatically update themselves in response to state changes.

## Store and Middleware

Middleware can be used to intercept actions as they are dispatched to the store. Middleware can modify actions, handle
side effects (such as network requests), or block actions from reaching the reducer. The store ensures that middleware
is processed in the correct order before the reducer handles the action and updates the state.

## Conclusion

The `Store` interface serves as the central hub for managing application state, processing actions, and broadcasting
state updates. It provides a predictable and observable system for handling state transitions, making it easier to
reason about the application's behavior and to build reactive components that automatically update in response to state
changes.