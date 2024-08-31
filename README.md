# Kdux

**Kdux** is a Kotlin-based state management library that brings the power of the Redux pattern to any Kotlin project.
With built-in coroutine support, Kdux is designed to integrate seamlessly with structured concurrency, making it an
ideal choice for modern Kotlin applications. The Redux pattern is particularly well-suited to integrate with Android's
MVI architecture.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
    - [Installation](#installation)
    - [Basic Usage](#basic-usage)
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

## Getting Started

### Installation

To add Kdux to your project, include the following in your `build.gradle.kts` (for Kotlin DSL):

```kotlin
dependencies {
    implementation("com.example:kdux:1.0.0")
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

- [Middleware](docs/middleware.md)
- [Enhancer](docs/enhancer.md)
- [Store](docs/store.md)
- [StoreCreator](docs/store_creator.md)
