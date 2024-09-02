## FailSafeEnhancer

The `FailSafeEnhancer` provides a mechanism to handle errors during the dispatch process. By integrating this enhancer into your store, you can prevent your application from crashing due to unexpected exceptions and instead recover gracefully by providing fallback logic or even retry/recovery mechanisms.

### Overview

### What is the `FailSafeEnhancer`?

The `FailSafeEnhancer` intercepts errors that occur during the processing of actions. When an exception is thrown while an action is being dispatched, the enhancer catches the exception and invokes your `onError` function. This function allows you to handle the error, potentially retry the action, or even dispatch a different action to recover from the error.

### Why Use the `FailSafeEnhancer`?

In complex applications, errors can occur unexpectedly during state transitions. The `FailSafeEnhancer` ensures that these errors do not cause your application to crash or enter an inconsistent state. Instead, it gives you the flexibility to:

- **Retry Actions**: Attempt to re-dispatch the action that caused the error.
- **Fallback Actions**: Dispatch a different action to handle the error or recover gracefully.
- **Log and Monitor**: Capture error details and monitor issues in your application's state management.

### How It Works

The `FailSafeEnhancer` works by wrapping the store's `dispatch` function. When an action is dispatched and an error occurs during its processing, the enhancer invokes the `onError` function. This function receives:

- **Current State**: The state of the store when the error occurred.
- **Action**: The action that caused the error.
- **Error**: The exception that was thrown.
- **Dispatch Callback**: A callback function that allows you to dispatch a new action.

You can use this information to decide how to handle the error, whether by retrying the same action, dispatching a different action, or logging the error for further analysis.

### Example Usage

```kotlin
val store = kdux.store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    onError { state, action, error, dispatch ->
        println("Error occurred: $error")

        if (action is ImportantAction) {
            dispatch(action)
        }
    }
}
```