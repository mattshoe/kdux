# Throttle Enhancer

The `ThrottleEnhancer` is a powerful tool that limits the rate at which actions are dispatched to a store in a Redux-like state management system. This enhancer ensures that actions are dispatched at most once per specified interval, preventing the store from being overwhelmed by rapid-fire actions and minimizing unnecessary state updates.

## Overview

### What is the `ThrottleEnhancer`?

The `ThrottleEnhancer` is an enhancer that regulates the dispatch rate of actions. It does not drop any actions; instead, if actions are dispatched too quickly, they are queued up and dispatched one at a time, with each dispatch separated by the specified interval. This is particularly useful in scenarios where actions are triggered frequently, such as user input events, network polling, or other high-frequency events.

### Why Use the `ThrottleEnhancer`?

In scenarios where actions may be dispatched rapidly, such as frequent user interactions or real-time data streams, uncontrolled dispatching can lead to performance bottlenecks and excessive state updates. The `ThrottleEnhancer` addresses this by:

- **Rate Limiting**: Ensuring that actions are dispatched at most once per specified interval.
- **Action Queuing**: Queuing up actions that occur too quickly and dispatching them in sequence, preserving the order of execution.
- **State Management Optimization**: Reducing unnecessary state updates, which can improve overall application performance and responsiveness.

## How It Works

The `ThrottleEnhancer` works by wrapping the store’s `dispatch` function with a mechanism that tracks the time of the last dispatched action. When a new action is dispatched, the enhancer checks whether enough time has passed since the last dispatch:

- If the interval has passed, the action is dispatched immediately.
- If the interval has not passed, the action is delayed until the remaining time in the interval has elapsed.

This ensures that actions are dispatched no more than once per specified interval, with any excess actions being processed sequentially.

### Key Features

- **Non-Blocking Dispatch**: Actions are not dropped but queued up and dispatched at the correct time, ensuring that no action is lost.
- **Sequential Dispatching**: When multiple actions are queued, they are processed in the order they were dispatched, maintaining the correct sequence of operations.
- **Concurrency Handling**: The enhancer uses a mutex to ensure that actions are queued and dispatched in the correct order, even when multiple coroutines attempt to dispatch actions simultaneously.

## Example Usage

To use the `ThrottleEnhancer`, integrate it into your store during the creation process. Below is an example:

```kotlin
val store = store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    throttle(500.milliseconds)
}
```