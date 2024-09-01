# Logging Enhancer

The `LoggingEnhancer` is a powerful tool designed to provide real-time insights into the actions being dispatched to
your store in a Redux-like state management system. By integrating this enhancer into your store, you can effectively
monitor the flow of actions and observe how they impact the application's state over time. This is particularly useful
for debugging, performance monitoring, and gaining a deeper understanding of your application's behavior.

## Overview

### What is the `LoggingEnhancer`?

The `LoggingEnhancer` is an enhancer that intercepts every action dispatched to the store and logs it asynchronously.
This means that the logging process does not block or delay the dispatch process, allowing your application to continue
running smoothly while logging occurs in the background.

### Why Use the `LoggingEnhancer`?

In complex applications, keeping track of the actions being dispatched and the resulting state changes can be
challenging. The `LoggingEnhancer` provides a straightforward solution by logging every action as it occurs, offering
the following benefits:

- **Debugging**: Easily track down bugs by reviewing the sequence of actions leading up to an issue.
- **Monitoring**: Monitor how actions affect the state, ensuring that your application behaves as expected.
- **Performance Analysis**: Observe the frequency and types of actions dispatched to optimize performance.

## How It Works

The `LoggingEnhancer` works by wrapping the store's `dispatch` function. Every time an action is dispatched, the
enhancer logs the action asynchronously before passing it along to the next middleware or reducer. Here's a breakdown of
the key components:

- **Asynchronous Logging**: The logging is handled in a non-blocking way using Kotlin coroutines, ensuring that the
  dispatch process is not delayed.
- **Flexible Logging Function**: The enhancer accepts a suspendable `log` function as a parameter. This function is
  responsible for logging the action and can be customized to suit your needs, whether you want to log to the console, a
  file, or a remote logging service.

### How to Use the LoggingEnhancer

To use the LoggingEnhancer, simply integrate it into your store during the creation process. Here’s an example of how to
do this:

```kotlin
val myStore =  kdux.store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    log { println(it) }
}
```