## Testing

Kdux is designed with testability in mind, offering robust tools to ensure that your state management logic behaves as
expected. Whether you're testing reducers, middleware, or the entire store, Kdux supports integration-style testing,
which allows you to verify the complete flow of state changes in your application.

### Best Practices for Testing with Kdux

- **Don't Neglect Unit Tests:** Unit testing Reducers and Middleware is essential to ensure your code works the way you
  intend it to.
- **Make Use of Integration Tests:** Since Kdux is designed to handle state management in a holistic way, it’s
  beneficial to incorporate integration tests that cover the entire flow, from action dispatching to state updates.
- **Coroutine Testing Tools:** Given that Kdux uses coroutines and flows, utilize tools like runTest and Turbine to
  effectively manage coroutine execution and flow testing.

### Testing Reducers

Reducers are pure functions that take the current state and an action, then return a new state. This predictability
makes reducers easy to test in isolation.

**Example:**

```kotlin
class CounterReducerTest {

    private val reducer = CounterReducer()

    @Test
    fun `WHEN Increment action is dispatched THEN state count is incremented`() {
        val initialState = CounterState(count = 0)
        val newState = reducer.reduce(initialState, CounterAction.Increment)
        assertThat(newState.count).isEqualTo(1)
    }

    @Test
    fun `WHEN Decrement action is dispatched THEN state count is decremented`() {
        val initialState = CounterState(count = 1)
        val newState = reducer.reduce(initialState, CounterAction.Decrement)
        assertThat(newState.count).isEqualTo(0)
    }
}
```

### Testing Middleware

Middleware can modify actions, handle side effects, or even block actions from reaching the reducer. When testing
middleware, focus on verifying that the correct actions are intercepted and any side effects (like logging or API calls)
are performed.

**Example:**

```kotlin
class LoggingMiddlewareTest {

    private val middleware = LoggingMiddleware()

    @Test
    fun `WHEN action is dispatched THEN it is logged correctly`() = runTest {
        val store = TestStore(initialState = CounterState(count = 0))
        middleware.apply(store, CounterAction.Increment) { action ->
            store.dispatch(action)
        }

        assertThat(middleware.log).containsExactly("Logging: Increment")
    }
}
```

### Testing the Store

Testing the store involves verifying that state transitions occur correctly when actions are dispatched and that
middleware is applied as expected. Kdux makes it straightforward to test the full integration of your store.

**Example:**

```kotlin
class StoreTest {

    @Test
    fun `WHEN action is dispatched THEN state is updated`() = runTest {
        val store = store(
            initialState = CounterState(count = 0),
            reducer = CounterReducer()
        )

        store.dispatch(CounterAction.Increment)

        assertThat(store.state.first().count).isEqualTo(1)
    }

    @Test
    fun `WHEN multiple actions are dispatched THEN final state is correct`() = runTest {
        val store = store(
            initialState = CounterState(count = 0),
            reducer = CounterReducer()
        )

        store.dispatch(CounterAction.Increment)
        store.dispatch(CounterAction.Decrement)

        assertThat(store.state.first().count).isEqualTo(0)
    }
}
```

#### Testing with Coroutines and Flows

Kdux leverages Kotlin coroutines and flows for state management. When testing these asynchronous components, tools like
Turbine make it easy to verify the behavior of your flows.

**Example:**

```kotlin
class StoreFlowTest {

    @Test
    fun `WHEN action is dispatched THEN state flow emits correct values`() = runTest {
        val store = store(
            initialState = CounterState(count = 0),
            reducer = CounterReducer()
        )

        store.state.test {
            // initial value
            assdertThat(awaitItem().count).isEqualto(0)
            store.dispatch(CounterAction.Increment)
            assertThat(awaitItem().count).isEqualTo(1)

            store.dispatch(CounterAction.Decrement)
            assertThat(awaitItem().count).isEqualTo(0)
        }
    }
}
```

### Testing Enhancers

Enhancers modify or extend the behavior of the store by wrapping it and adding new functionality, such as batching
actions or logging performance metrics. When testing enhancers, the goal is to ensure that the store behaves as expected
with the added functionality.

**Example:**

```kotlin
class ActionBatchingEnhancerTest {

    @Test
    fun `WHEN actions are batched THEN state is updated after all actions are processed`() = runTest {
        val store = store(
            initialState = CounterState(count = 0),
            reducer = CounterReducer()
        ) {
            enhancers(ActionBatchingEnhancer())
        }

        // Start batching actions
        store.startBatching()

        // Dispatch multiple actions
        store.dispatch(CounterAction.Increment)
        store.dispatch(CounterAction.Increment)
        store.dispatch(CounterAction.Decrement)

        // End batching and apply all actions
        store.endBatching()

        // Check final state after batching
        assertThat(store.state.first().count).isEqualTo(1)
    }

    @Test
    fun `WHEN no actions are batched THEN state is updated after each action`() = runTest {
        val store = store(
            initialState = CounterState(count = 0),
            reducer = CounterReducer()
        ) {
            enhancers(ActionBatchingEnhancer())
        }

        // Dispatch actions without batching
        store.dispatch(CounterAction.Increment)
        assertThat(store.state.first().count).isEqualTo(1)

        store.dispatch(CounterAction.Decrement)
        assertThat(store.state.first().count).isEqualTo(0)
    }
}
```

### Summary

Testing with Kdux is designed to be straightforward and robust, ensuring that your state management logic is reliable,
predictable, and easy to validate. By following these guidelines, you can confidently write tests that cover all aspects
of your state management, from individual reducers to the entire application flow.