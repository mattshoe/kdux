# Kdux DSL: Detailed Guide

The Kdux DSL provides a powerful and flexible way to configure and manage state in your Kotlin applications. This guide
will walk you through each component of the DSL, explaining how to use it effectively.

## Global Settings

To configure global settings in your Kdux-powered application, you will use the `kdux {...}` function. This function accepts a
lambda for you to define global behaviors that will be applied to all Kdux Stores in the application.

### Usage Example

Here’s a list of the various global configurations available to you. 
<br> Note that each of these are entirely optional:

```kotlin
kdux {
    // Add a global action filter, blocking any dispatch in the application as you see fit
    globalGuard { action ->
        isUserLoggedIn() && isAllowed(action)
    }

    // Log all dispatches in the application
    globalLogger { action ->
        println("Action dispatched: $action")
    }

    // Receive performance metrics for every dispatch in the application
    globalPerformanceMonitor { data ->
        println("Store: ${data.storeName} -- Action `${data.action}` took ${data.duration.inWholeMilliseconds}ms")
    }
    
    // Clear any and all previously defined global behaviors
    clearGlobals()
}
```

## Creating a Store

You can create a store using the `store(...) {...}` function provided by the DSL. The lambda allows you to configure any
additional functionality you want your `Store` to have, such as debouncing, action logging, buffered dispatches, and
any other functionality you could come up with.

Note that the lambda and all functions inside it are entirely optional. Only `initialState` and `reducer` are required.

```kotlin
val store = kdux.store(
    initialState = MyState(),
    reducer = MyReducer()
) {
    // Give your store a custom name (helpful in debugging or reporting)
    name("MyStore")
    
    // Add middleware (order matters)
    add(MyMiddleware1(), MyMiddleware2(), MyMiddleware3())

    // Add custom enhancers
    add(MyEnhancer(), AnotherEnhancer())

    // Block actions that fail an authorization check
    guard { action ->
        isUserLoggedIn() && isAllowed(action)
    }

    // Add logging
    log { action ->
        doTheLogging(action)
    }

    // Add performance reporting
    monitorPerformance { data ->
        doTheMonitoring(data)
    }
    
    // Throttle dispatch processing to once per interval
    throttle(interval = 500.milliseconds)

    // Buffer your dispatches to be flushed all at once when they reach the size limit
    buffer(size = 12)

    // Add dispatch debouncing
    debounce(1.seconds)

    // Batch your dispatches
    batched(duration = 1.minute)
}
```