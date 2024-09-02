## Reducer

The `Reducer` is a core component in a Redux-like architecture, responsible for processing actions and determining how
the application's state should change in response to those actions.

### Key Characteristics

- **Pure Function**: Reducers are pure functions, meaning they do not produce side effects. Given the same state and
  action, they will always return the same new state without modifying the original state.
- **Immutability**: Reducers treat the state as immutable. Instead of modifying the existing state, they return a new
  instance with the necessary changes.
- **Action-Driven**: Reducers are driven by actions that represent events or intents within the application. Each action
  contains logic describing what happened, and the reducer determines how the state should change in response.
- **Synchronous or Asynchronous**: While typically synchronous, reducers can be `suspend` functions, allowing them to
  handle asynchronous operations before returning the new state.

### How Reducers Work

1. **Receive Action**: The reducer receives an action dispatched by the store after it has passed through any
   middleware.
2. **Process State**: The reducer examines the current state and the dispatched action to determine how the state should
   change.
3. **Return New State**: The reducer creates a new state object based on the logic associated with the action and
   returns it. The store then updates its state to the new state returned by the reducer.

### Example Reducer

Consider a simple counter example where the state is an integer representing the current count. The reducer handles two
types of actions: increment and decrement:

```kotlin
data class CounterState(val count: Int)

sealed class CounterAction {
    object Increment : CounterAction()
    object Decrement : CounterAction()
}

class CounterReducer : Reducer<CounterState, CounterAction> {
    override suspend fun reduce(state: CounterState, action: CounterAction): CounterState {
        return when (action) {
            is CounterAction.Increment -> state.copy(count = state.count + 1)
            is CounterAction.Decrement -> state.copy(count = state.count - 1)
        }
    }
}
```

In this example, the reducer handles two types of actions (Increment and Decrement) and updates the state accordingly.
If the Increment action is received, the state is updated by incrementing the count. If the Decrement action is
received, the state is updated by decrementing the count. The reducer always returns a new state without modifying the
original state.

### Principles of Reducer Design

- **Immutability**: The reducer must never modify the original state directly. Instead, it should return a new state
  object with the necessary updates. In Kotlin, this is typically achieved by using `data class` copy methods, which
  allow you to create modified copies of an immutable object.
- **Action-Driven Logic**: The reducer is entirely action-driven. Each action should represent an event or intent, and
  the reducer's logic should handle that action to produce a new state. Reducers can handle multiple types of actions,
  and you can use Kotlin's `when` expression to branch based on the type of action received.
- **Deterministic**: Given the same inputs (state and action), a reducer must always produce the same output. This
  predictability makes the application easier to test and reason about.
- **No Side Effects**: Reducers should not trigger side effects like network requests or database writes. These types of
  operations are handled in middleware. The reducer's job is solely to compute the next state.

### Reducer Lifecycle in the Store

The reducer is a critical part of the store's lifecycle. When an action is dispatched to the store:

1. **Action Dispatch**: The store receives the dispatched action.
2. **Middleware Processing**: The action passes through the middleware chain, which may modify or intercept the action.
3. **Reducer Invocation**: The store invokes the reducer with the current state and the processed action.
4. **New State Calculation**: The reducer processes the action and returns a new state based on the action's type and
   payload.
5. **State Update**: The store updates its state to the new state returned by the reducer.
6. **State Notification**: The store emits the new state to all subscribers, allowing the application to react to the
   state change.

### Example Use Case

In a shopping cart application, the state might represent the items in the cart, and the reducer might handle actions
like adding an item, removing an item, or clearing the cart. The reducer would take the current cart state and the
dispatched action (e.g., `AddItemAction`) and return a new state that includes the updated cart contents.

```kotlin
data class CartState(val items: List<Item>)

sealed class CartAction {
    data class AddItem(val item: Item) : CartAction()
    data class RemoveItem(val itemId: String) : CartAction()
    object ClearCart : CartAction()
}

class CartReducer : Reducer<CartState, CartAction> {
    override suspend fun reduce(state: CartState, action: CartAction): CartState {
        return when (action) {
            is CartAction.AddItem -> state.copy(items = state.items + action.item)
            is CartAction.RemoveItem -> state.copy(items = state.items.filter { it.id != action.itemId })
            is CartAction.ClearCart -> state.copy(items = emptyList())
        }
    }
}
```

### Conclusion

The Reducer interface plays a central role in determining how the state evolves over time in response to actions. It is
the pure function that handles state transitions, ensuring that the state is updated predictably and immutably. Reducers
are designed to be simple, deterministic, and free of side effects, ensuring that the state management system remains
consistent, testable, and predictable.
