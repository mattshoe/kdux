# Buffer Enhancer

The `BufferEnhancer` is a tool designed to accumulate dispatched actions in a buffer until a specified buffer size is
reached. Once the buffer is full, all buffered actions are dispatched to the store at once. This approach is
particularly effective in scenarios where you want to reduce the frequency of state updates and process actions in
batches, thereby improving performance.

## Overview

### What is the `BufferEnhancer`?

The `BufferEnhancer` is an enhancer that buffers actions as they are dispatched to the store. Actions are accumulated in
a buffer, and when the number of buffered actions reaches the specified `bufferSize`, all actions in the buffer are
dispatched together. This ensures that state updates are minimized, which can be beneficial in performance-critical
applications.

### Why Use the `BufferEnhancer`?

In applications where frequent actions are dispatched, immediate state updates can lead to performance bottlenecks,
especially if each state change triggers expensive operations. The `BufferEnhancer` offers several key benefits:

- **Performance Optimization**: By buffering actions and dispatching them in batches, the enhancer reduces the number of
  state updates, which can significantly improve performance.
- **Order Preservation**: Actions are dispatched in the same order they entered the buffer, ensuring that the sequence
  of operations remains consistent and predictable.
- **Reduced Overhead**: By batching actions, the overhead associated with frequent state updates is minimized, making it
  ideal for high-throughput applications.

## How It Works

The `BufferEnhancer` works by wrapping the store's `dispatch` function. When an action is dispatched, it is added to a
buffer instead of being immediately processed. Once the number of actions in the buffer reaches the
specified `bufferSize`, all buffered actions are dispatched to the store in the order they were added.

### Key Features

- **Buffering Mechanism**: Actions are accumulated in a buffer until the specified `bufferSize` is reached.
- **Thread-Safe Operation**: The enhancer uses a mutex to ensure that actions are safely added to the buffer in
  concurrent environments.
- **Order Preservation**: Actions are dispatched in the exact order they were buffered, ensuring that the sequence of
  operations is maintained.

### How to Use the `BufferEnhancer`

To use the `BufferEnhancer`, integrate it into your store during the creation process. You can specify the `bufferSize`
to control how many actions are accumulated before they are dispatched.

Example usage:

```kotlin
fun createStore(): Store<MyState, MyAction> {
    return store(
        initialState = MyState(),
        reducer = MyReducer()
    ) {
        buffer(size = 10)
    }
}
```