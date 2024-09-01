# Batch Enhancer

The `BatchEnhancer` is a useful tool designed to batch actions over a specified time duration before dispatching them all at once. This enhancer is particularly effective in scenarios where you want to minimize the number of state updates, thereby improving performance by reducing the frequency of dispatches.

## Overview

### What is the `BatchEnhancer`?

The `BatchEnhancer` is an enhancer that accumulates actions in a batch over a specified time period (`batchDuration`). Once the elapsed time since the start of the batch exceeds this duration, all accumulated actions are dispatched together during the next dispatch call. This batching mechanism helps in controlling the flow of actions and reducing the overhead of frequent state updates.

### Why Use the `BatchEnhancer`?

In applications where actions are frequently dispatched, updating the state after each action can lead to performance issues, especially if the state changes trigger expensive operations such as re-rendering UI components or saving data. The `BatchEnhancer` provides several key benefits:

- **Performance Optimization**: By batching actions together, you can reduce the number of state updates and improve overall performance.
- **Controlled Dispatching**: Accumulating actions over a defined duration allows you to manage when state updates occur, providing better control over your application's behavior.
- **Reduced Overhead**: Fewer state updates mean less overhead in managing state changes, particularly in complex applications with heavy processing on state updates.

## How It Works

The `BatchEnhancer` works by wrapping the store's `dispatch` function. When an action is dispatched, it is added to a batch rather than being immediately processed. The enhancer tracks the time elapsed since the start of the batch, and if this elapsed time exceeds the specified `batchDuration`, all actions in the batch are dispatched together on the next call to `dispatch`.

### Key Features

- **Time-Based Batching**: Actions are accumulated until the specified `batchDuration` has passed.
- **Non-Blocking Operation**: The enhancer uses a mutex to ensure thread safety while accumulating actions, without blocking other operations.
- **Flexible Duration**: The duration for batching actions can be easily customized, making the enhancer adaptable to various application needs.

### How to Use the `BatchEnhancer`

To use the `BatchEnhancer`, integrate it into your store during the creation process. You can specify the `batchDuration` to control how long actions are accumulated before they are dispatched.

Example usage:

```kotlin
val myStore = store(
        initialState = MyState(),
        reducer = MyReducer()
    ) {
        batched(5.seconds)
    }
}
```