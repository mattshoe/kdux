# Performance Enhancer

The `PerformanceEnhancer` is a valuable tool for monitoring and analyzing the performance of your state management
system. By integrating this enhancer into your store, you can gain insights into the time taken to process each action
from the moment it is dispatched to the moment the dispatch process completes. This includes the time spent in
middleware, enhancers, and the reducer.

## Overview

### What is the `PerformanceEnhancer`?

The `PerformanceEnhancer` is an enhancer that measures and logs the performance of each action dispatched to the store.
It tracks the total time taken from the dispatch of an action to the completion of the dispatch function, including all
the processing done by middleware, enhancers, and the reducer. This helps in identifying slow actions that may impact
the application's performance.

### Why Use the `PerformanceEnhancer`?

In complex applications, certain actions may take longer to process due to extensive middleware, complex reducers, or
other factors. The `PerformanceEnhancer` provides a mechanism to:

- **Monitor Performance**: Keep track of how long it takes to process actions, helping you understand the performance
  characteristics of your state management system.
- **Identify Bottlenecks**: Detect actions that are slow to process, which may indicate areas where performance
  optimizations are needed.
- **Optimize Application**: Use the insights gained from performance data to improve the overall responsiveness and
  efficiency of your application.

## How It Works

The `PerformanceEnhancer` works by wrapping the store's `dispatch` function. Every time an action is dispatched, the
enhancer measures the total time taken for the dispatch process to complete. This time includes all middleware,
enhancers, and the reducer. The measured duration is then logged using a custom logging function.

- **Time Measurement**: The enhancer uses Kotlin's `measureTime` function to accurately measure the time taken for the
  entire dispatch process.
- **Performance Data Logging**: The enhancer accepts a suspendable `log` function that logs the performance data for
  each dispatched action. This function can be customized to log data to the console, a file, or a remote logging
  service.

### How to Use the PerformanceEnhancer

To use the PerformanceEnhancer, integrate it into your store during the creation process. Here’s an example of how to do
this:

```kotlin
val myStore = kdux.store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    monitorPerformance { 
        println("Store: ${it.storeName}, Action: ${it.action}, Duration: ${it.dispatchDuration}")
    }
}
```

