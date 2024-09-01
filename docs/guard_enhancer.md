# GuardEnhancer

## Overview

The `GuardEnhancer` provides conditional control over which actions can be dispatched in your Kdux store. By implementing a custom authorization function, you can ensure that only authorized actions are allowed to modify the store's state. This is useful for enforcing security, permissions, or other rules that dictate which actions can take effect.

## How It Works

The `GuardEnhancer` intercepts actions before they are dispatched to the store. It evaluates each action using a provided `isAuthorized` function. If the function returns `true`, the action is dispatched as usual. If the function returns `false`, the action is blocked and does not reach the store.

### Parameters

- **State**: The type representing the state managed by the store.
- **Action**: The type representing the actions that can be dispatched to the store.
- **isAuthorized**: A suspendable function that takes an `Action` as a parameter and returns `true` if the action should be dispatched, or `false` if it should be blocked.

## Usage

### Adding the GuardEnhancer to a Store

You can add the `GuardEnhancer` to your store using the `guard` DSL function:

```kotlin
val store = kdux.store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    guard { action -> 
        // Define your authorization logic here
        userIsLoggedIn() && isAllowed(action)
    }
}
```