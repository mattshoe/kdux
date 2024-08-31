# Kdux

**Kdux** is a Kotlin-based state management library that brings the power of the Redux pattern to any Kotlin project.
With built-in coroutine support, Kdux is designed to integrate seamlessly with structured concurrency, making it an
ideal choice for modern Kotlin applications. The Redux pattern is particularly well-suited to integrate with Android's
MVI architecture.

## Table of Contents

- [Features](#features)
- [Why Kdux?](#what-is-kdux)
- [Getting Started](#getting-started)
    - [Installation](#installation)
    - [Basic Usage](#basic-usage)
- [Kdux and MVI](docs/kdux_mvi.md)
- [Advanced Usage](#advanced-usage)
    - [Middleware](docs/middleware.md)
    - [Enhancers](docs/enhancer.md)
    - [StoreCreator](docs/store_creator.md)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Platform-Agnostic**: Kdux supports any Kotlin project, whether it's mobile, server-side, or any other Kotlin
  environment.
- **Structured Concurrency**: Built-in support for coroutines ensures that Kdux aligns with Kotlin's structured
  concurrency model.
- **Flexible Architecture**: Users have complete control over how they define state, actions, reducers, middleware, and
  enhancers.
- **Integration with MVI**: Kdux is particularly well-suited for use in Android applications using MVI (
  Model-View-Intent) architecture.

## Why Kdux?

Kdux is a Kotlin-based state management library that implements the Redux pattern, designed to offer a **predictable**,
**deterministic**, and **centralized** way to manage application state across any Kotlin project. The Kdux pattern is
particularly effective in
scenarios where state consistency, scalability, and traceability are critical, making it an ideal solution for complex
applications, especially those using MVI (Model-View-Intent) architecture.

### The Kdux Pattern

At the heart of Kdux is the Redux pattern, a well-established approach to managing state in a predictable and traceable
manner. The pattern revolves around three core principles:

1. **Single Source of Truth:** Your state is stored in a single object, which ensures consistency and provides a clear
   and accessible snapshot of the application at any point in time.
2. **State is Read-Only:** The state cannot be modified directly. Instead, actions are dispatched to indicate the intent
   to change the state. This ensures that all state transitions are explicit, traceable, and follow a predictable flow.
3. **Changes are Made with Pure Functions:** Reducers are pure functions that take the current state and an action as
   input, and return a new state. This guarantees that the state transitions are predictable and easy to test.

### How Kdux Differs from Traditional Redux

While Kdux is heavily inspired by the traditional Redux pattern, it introduces several key differences and enhancements
tailored specifically for Kotlin applications, particularly in environments like Android where proper resource
management
and memory usage are critical, and process death can occur at any time. Below are some of the things that set Kdux
apart.

#### Segregated State Management

Unlike traditional Redux, where the entire application’s state is typically centralized in a single store, Kdux
encourages the segregation of state into logical chunks, each managed by its own store. This allows you to create
multiple stores, each responsible for a distinct part of your application’s state. For example, you may have an app that
segregates state into something along the lines of:

- AuthStore: authentication-related state, such as user login status and tokens.
- UserCartStore: state of the user’s shopping cart, including items, quantities, and prices.
- LocationStore: location of the user in an app where the user's location is important.
- etc, etc

This segregation is particularly beneficial in Android applications, where memory is limited and managing a monolithic
application state can be inefficient. By dividing the state into smaller, more manageable chunks, each store can be
optimized independently, reducing the overhead associated with serializing and deserializing large state objects.

Advantages of Segregated State Management

- **Improved Performance:** By managing smaller, focused state objects, you reduce the memory footprint and the
  computational cost associated with state updates.
- **Scalability:** As your application grows, you can easily add new stores to manage new features, without affecting
  the existing state management logic.
- **Modularity:** Each store can be developed, tested, and maintained independently, promoting better code organization
  and easier maintenance.
- **Resource Management:** In environments like Android, where resources are constrained, segregated state management
  ensures that only the necessary parts of the state are kept in memory, avoiding unnecessary serialization of the
  entire application state.

#### Synchronous Dispatch and Structured Concurrency

Another key difference in Kdux is how it handles the dispatch operation. In traditional Redux, dispatching actions is
often asynchronous, which can introduce complexities around managing race conditions and ensuring that state updates
occur in a predictable order.

Kdux, however, adheres to Kotlin’s structured concurrency model. In Kdux:

- **Synchronous Dispatch by Default:** Dispatch operations are not asynchronous by default. When an action is
  dispatched, it is processed sequentially and predictably within the current coroutine context. This synchronous
  behavior ensures that each action is fully processed before the next one begins, eliminating race conditions and
  making state transitions more predictable.
- **Structured Concurrency:** Kdux leverages Kotlin’s structured concurrency to ensure that all state transitions and
  side effects are managed within a defined scope. This means that dispatch operations are always predictable and occur
  within the bounds of the coroutine scope in which they are executed, simplifying resource management and reducing the
  likelihood of memory leaks or orphaned coroutines.

Benefits of Synchronous Dispatch and Structured Concurrency

- **Predictability:** Since dispatch operations are synchronous and sequential, you can be confident that state
  transitions occur in a well-defined order, making debugging and reasoning about your application’s behavior much
  simpler.
- **Simplified Resource Management:** Structured concurrency ensures that all side effects and state updates are
  contained within the same coroutine scope, reducing the risk of resource leaks and making it easier to manage system
  resources.
- **Easier Testing:** The predictable nature of synchronous dispatch makes it easier to write tests for your state
  management logic, as you don’t need to account for the complexities of asynchronous action processing.

#### tl;dr

Kdux enhances the well-proven Redux pattern with a few tweaks to provide a powerful, flexible, and scalable state management solution for
Kotlin projects. By enforcing predictability, centralization, and testability, Kdux ensures that even the most complex
applications can maintain a consistent and reliable state management strategy.

### Flow of Events in Kdux

The flow of events in Kdux follows a clear and structured path, ensuring that every state change is intentional and
controlled. Here’s how the flow works:

```
    +------------------+
    |     Dispatch     |
    |      Action      |
    +--------+---------+
             |
             v
    +------------------+
    |  Middleware 1    |  
    +--------+---------+
             |          
             v          
    +------------------+
    |  Middleware 2    |
    +--------+---------+
             |          
             v          
    +------------------+
    |  Middleware N    |  
    +--------+---------+
             |          
             v          
    +------------------+
    |     Reducer      |
    +--------+---------+
             |
             v
+---------------------------+
|     New State Created     |
|  (Calculated by Reducer)  |
+------------+--------------+
             |
             v
 +-------------------------+
 |  Store Emits New State  |
 +-----------+-------------+
        
```

## Getting Started

### Installation

To add Kdux to your project, include the following in your `build.gradle.kts` (for Kotlin DSL):

```kotlin
dependencies {
    implementation("com.example:kdux:1.0.1")
}
```

## Usage

Here’s a simple example to get you started:

1. Define the State your store will be operating on
    ```kotlin
    data class CounterState(val count: Int = 0)
    ```

2. Define the actions your store will take
    ```kotlin
    sealed class CounterAction {
        object Increment : CounterAction()
        object Decrement : CounterAction()
    }
   ```
3. Define the store's reducer. A reducer simply modifies the `State` given an `Action`
    ```kotlin
    // Define your Reducer
    class CounterReducer : Reducer<CounterState, CounterAction> {
        override suspend fun reduce(state: CounterState, action: CounterAction): CounterState {
            return when (action) {
                is CounterAction.Increment -> state.copy(count = state.count + 1)
                is CounterAction.Decrement -> state.copy(count = state.count - 1)
            }
        }
    }
   ```

4. Define your store. This is done using the Kdux DSL. This allows you to delegate to a class, or store it in a property
   with the same ease.
    ```kotlin
    // Option 1: Create a Store variable with DSL
    val counterStore = kdux.store(
            initialState = CounterState(0),
            reducer = CounterReducer()
        )
    
    // Option 2: Create a Store class by delegation using DSL
    class CounterStore(
        initialState: CounterState = CounterState(0),
        reducer: Reducer<CounterState, CounterAction> = CounterReducer()
    ): Store<CounterState, CounterAction> 
        by kdux.store(
            initialState, 
            reducer
        )
   ```

5. Stream the store's state
    ```kotlin
    counterStore.state
        .onEach {
            handleStateChanged(it)
        }.launchIn(yourScope)
    ```

6. Dispatch your actions. You've defined the data flow, now let it run!
    ```kotlin
    // Dispatch actions
    store.dispatch(CounterAction.Increment)
    store.dispatch(CounterAction.Decrement)
    ```

## Advanced Usage

Kdux is designed to be highly flexible and extensible. This documents below will dive deep into how you can leverage
middleware, enhancers, and custom store creators to build complex state management solutions that cater to your
specific needs.

- [Integrating Kdux with MVI](docs/kdux_mvi.md)
- [Middleware](docs/middleware.md)
- [Enhancer](docs/enhancer.md)
- [Store](docs/store.md)
- [StoreCreator](docs/store_creator.md)


