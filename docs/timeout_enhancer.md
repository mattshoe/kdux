## Timeout Enhancer

The `TimeoutEnhancer` enforces a time limit on the dispatching of actions within a Kdux store. If an action is not
processed within the specified `timeout` duration, the dispatch is canceled, and a `TimeoutCancellationException` is
thrown.

### Overview

### What is the `TimeoutEnhancer`?

The `TimeoutEnhancer` is an enhancer that wraps the dispatch process of a Kdux store with a timeout mechanism. It
ensures that any action dispatched to the store is processed within a given time frame. If the processing of an action
takes longer than the specified timeout, the operation is canceled.

### Why Use the `TimeoutEnhancer`?

In certain scenarios, you may want to enforce strict time limits on how long an action can take to process. This can be
particularly useful for:

- **Preventing Long-Running Operations**: Ensure that no single action can block the store indefinitely, potentially
  leading to a better-performing application.
- **Enforcing Timeouts on Critical Actions**: For actions where timing is crucial, this enhancer ensures they are
  completed within a specific duration or are canceled.

### How It Works

The `TimeoutEnhancer` works by wrapping the store’s `dispatch` function in a `withTimeout` block provided by Kotlin’s
coroutines library. When an action is dispatched, the enhancer checks whether it can be processed within the
specified `timeout`. If the action processing exceeds this duration, the operation is canceled, and
a `TimeoutCancellationException` is thrown.

### Example Usage

```kotlin
val store = kdux.store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    timeout(500.milliseconds)
}
```