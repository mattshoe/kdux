# Debounce Enhancer

The `DebounceEnhancer` is a powerful tool designed to limit the rate at which actions are dispatched to your store in a Redux-like state management system. By integrating this enhancer, you can prevent rapid successive actions from overwhelming your application, ensuring that actions are only processed if a specified amount of time has passed since the last dispatch.

## Overview

### What is the `DebounceEnhancer`?

The `DebounceEnhancer` is an enhancer that debounces actions based on a specified time duration. This means that if actions are dispatched more frequently than the debounce duration, only the first action in the burst will be processed. This is particularly useful in scenarios where you want to avoid excessive state updates in response to rapid user inputs or other frequent events.

### Why Use the `DebounceEnhancer`?

In applications where actions can be triggered rapidly (such as from user inputs, network events, or timers), immediate processing of every action can lead to performance bottlenecks or undesired behaviors like excessive re-renders. The `DebounceEnhancer` provides several key benefits:

- **Performance Optimization**: By debouncing actions, you can reduce the frequency of state updates, improving overall performance.
- **Controlled Rate of Dispatch**: Ensures that actions are processed at a controlled rate, preventing rapid sequences of actions from overwhelming the system.
- **Enhanced User Experience**: Prevents unnecessary processing in response to rapid inputs, leading to smoother and more predictable application behavior.

## How It Works

The `DebounceEnhancer` works by wrapping the store's `dispatch` function. When an action is dispatched, the enhancer checks the time elapsed since the last dispatched action. If the elapsed time exceeds the specified debounce duration, the action is dispatched to the store. Otherwise, the action is ignored.

### Key Features

- **Time-Based Debouncing**: Actions are only dispatched if a specified amount of time has passed since the last action was processed.
- **Thread-Safe Operation**: The enhancer uses a mutex to ensure that the debouncing logic is thread-safe in concurrent environments.
- **Flexible Duration**: The debounce duration can be customized to suit the needs of your application, providing fine control over the rate of action dispatches.

### How to Use the `DebounceEnhancer`

To use the `DebounceEnhancer`, integrate it into your store during the creation process. You can specify the debounce `duration` to control the minimum time interval between dispatched actions.

Example usage:

```kotlin
fun createStore(): Store<MyState, MyAction> {
    return store(
        initialState = MyState(),
        reducer = MyReducer()
    ) {
        enhancers(
            DebounceEnhancer(Duration.seconds(1))
        )
    }
}
```