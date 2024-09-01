# Kdux DSL: Detailed Guide

The Kdux DSL provides a powerful and flexible way to configure and manage state in your Kotlin applications. This guide will walk you through each component of the DSL, explaining how to use it effectively.

## Store

### How to Create a Store

You can create a store using the `kdux.store` function provided by the DSL. The lambda allows you to configure any 
additional functionality you want your `Store` to have, such as debouncing, action logging, batched dispatches, etc.

The lambda and all functions inside it are optional.

```kotlin
val store = kdux.store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    // Add middleware (order matters)
    add(MyMiddleware1(), MyMiddleware2(), MyMiddleware3())
    
    // Add custom enhancers
    add(MyEnhancer(), AnotherEnhancer())
    
    // Add logging
    log { doTheLogging(it) }
    
    // Add dispatch debouncing
    debounce(1.seconds)
    
    // Batch your dispatches
    batched(duration = 1.minute)
    
    // Buffer your dispatches to be flushed when they reach a limit
    buffer(size = 12)
    
    // Add performance reporting
    monitorPerformance { doTheMonitoring(it) }
}
```