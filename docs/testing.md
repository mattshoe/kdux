## Testing

### Best Practices for Testing with Kdux

- **Don't Neglect Unit Tests:** Unit testing Reducers and Middleware is essential to ensure your code works the way you
  intend it to.
- **Make Use of Integration Tests:** Since Kdux is designed to handle state management in a holistic way, it’s
  beneficial to incorporate integration tests that cover the entire flow, from action dispatching to state updates.
- **Coroutine Testing Tools:** Given that Kdux uses coroutines and flows, utilize tools like [runTest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-test.html)
  and [Turbine](https://github.com/cashapp/turbine) to effectively manage coroutine execution and flow testing.

### Testing Reducers

Reducers are pure functions that take the current state and an action, then return a new state. This predictability
makes reducers easy to test in isolation.

**Example:**

```kotlin
class CounterReducerTest {

    private val reducer = CounterReducer()

    @Test
    fun `WHEN Increment action is dispatched THEN state count is incremented`() = runTest {
        val initialState = CounterState(count = 0)
        val newState = reducer.reduce(initialState, CounterAction.Increment)
        assertThat(newState.count).isEqualTo(1)
    }

    @Test
    fun `WHEN Decrement action is dispatched THEN state count is decremented`() = runTest {
        val initialState = CounterState(count = 1)
        val newState = reducer.reduce(initialState, CounterAction.Decrement)
        assertThat(newState.count).isEqualTo(0)
    }
}
```

### Testing Middleware

Middleware can modify actions, handle side effects, even block actions or dispatch new ones. When testing
middleware, focus on verifying that the correct actions are intercepted and any side effects (like logging or API calls)
are performed.

**Example:**

```kotlin
class LoggingMiddlewareTest {

    private val middleware = LoggingMiddleware()

    @Test
    fun `WHEN action is dispatched THEN it is logged correctly`() = runTest {
        val store: Store<counterState, CounterAction> = mockk()
        var nextWasInvoked = false
        middleware.apply(store, CounterAction.Increment) { action ->
            nextWasInvoked = true
        }

        assertThat(middleware.log).containsExactly("Logging: Increment")
        assertThat(nextWasInvoked).isTrue()
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
      
        store.test {
            // initial state
            assertThat(awaitItem()).isEqualTo(0)
          
            store.dispatch(CounterAction.Increment)
  
            assertThat(awaitItem().count).isEqualTo(1)
        }

    }

    @Test
    fun `WHEN multiple actions are dispatched THEN final state is correct`() = runTest {
        val store = store(
            initialState = CounterState(count = 0),
            reducer = CounterReducer()
        )
      
        store.test {
          // initial state
          assertThat(awaitItem()).isEqualTo(0)

          store.dispatch(CounterAction.Increment)
          assertThat(awaitItem().count).isEqualTo(1)
          
          store.dispatch(CounterAction.Decrement)
          assertThat(awaitItem().count).isEqualTo(0)
            
        }
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
class BufferEnhancerTest {

    @Test
    fun `WHEN actions are buffered THEN state is updated after all buffer is filled`() = runTest {
        val store = store(
            initialState = CounterState(count = 0),
            reducer = CounterReducer()
        ) {
            buffer(size = 3)
        }

        store.test {
            // Initial state
            assertThat(awaitItem().count).isEqualTo(0)

            // Dispatch multiple actions
            store.dispatch(CounterAction.Increment)
            expectNoEvents()
            store.dispatch(CounterAction.Increment)
            expectNoEvents()
            store.dispatch(CounterAction.Decrement)
    
            // Check final state after batching
            assertThat(awaitItem().count).isEqualTo(1)
            
        }
    }
}
```
