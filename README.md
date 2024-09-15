# **Kdux**

**Kdux** is a state management library that takes a more modern and practical approach to the [Redux](https://redux.js.org/tutorials/fundamentals/part-1-overview) pattern. **Kdux** is
optimized to take advantage of all the great features modern Kotlin has to offer. With a [custom DSL](docs/dsl.md) and
built-in
coroutines support, you get all the benefits of Kotlin.

**Kdux** enhances the [Redux](https://redux.js.org/tutorials/fundamentals/part-1-overview) pattern in a few ways: by moving away from the monolithic architecture of a Global State
used by traditional Redux, by enforcing sequential-by-default behavior of dispatches, and by merging functional programming concepts with the benefits of OOP. 

These enhancements strongly enforce
Structured Concurrency in your state management while drastically reducing race conditions, making state transitions truly
deterministic, and reducing the cognitive load of traditional Redux. 

The [**Kdux** pattern](#the-kdux-pattern) is particularly well-suited to integrate with
Android's [MVI architecture](docs/kdux_mvi.md). **Kdux** and **MVI** are
both "action-driven" architectures, and so it works wonderfully to help segregate concerns between your view, business,
and data layers. Check out this [much more in depth discussion on the benefits here](docs/kdux_mvi.md).

In an Android application, monolithic state become unwieldy very quickly. So by taking a modularized approach to State,
we can more easily manage the limited available memory and recover from process death or configuration changes without
worrying about exceeding memory limits due to enormous state objects.

<br>
<br>

## Table of Contents

- [The **Kdux** Pattern](#the-kdux-pattern)
- [Third Party Integration](docs/third_party_support.md)
- [Getting Started](#getting-started)
    - [Installation](#installation)
    - [Basic Usage](#basic-usage)
- [**Kdux** and MVI](docs/kdux_mvi.md)
- [DSL Guide](docs/dsl.md)
- [Advanced Usage](#advanced-usage)
    - [DevTools](docs/devtools.md)
    - [Middleware](docs/middleware.md)
    - [Enhancers](docs/enhancer.md)
    - [StoreCreator](docs/store_creator.md)
- [Testing](docs/testing.md)
- [Contributing](#contributing)

<br>
<br>

## Features

- **Expressive DSL**: **Kdux** provides an [expressive and flexible DSL](docs/dsl.md) that can be used to build tooling.
- **Structured Concurrency**: Built-in support for coroutines ensures that **Kdux** aligns with Kotlin's structured
  concurrency model.
- **Flexible Architecture**: Users have complete control over how they define state, actions, reducers, middleware, and
  enhancers.
- **Integration with MVI**: **Kdux** is particularly well-suited for use in Android applications [using MVI](docs/kdux_mvi.md) (
  Model-View-Intent) architecture.
- **Extensive Tooling**: The **Kdux** library comes with built-in support for [all major relevant libraries](docs/third_party_support.md), and extensive tooling for you to take advantage of; such
  as [persistence](docs/persistence_enhancer.md), [error reporting](docs/failsafe_enhancer.md), [performance logging](docs/performance_enhancer.md), [authorization](docs/guard_enhancer.md), [timeouts](docs/timeout_enhancer.md), [buffering](docs/buffer_enhancer.md), [debouncing](docs/debounce_enhancer.md), [throttling](docs/throttle_enhancer.md),
  and many more. New tools are being added all the time!

<br>
<br>

## The **Kdux** Pattern

At the heart of **Kdux** is the [Redux pattern](https://redux.js.org/tutorials/fundamentals/part-1-overview), a well-established approach to managing state in a predictable and
traceable manner. The **Kdux Pattern** revolves around these core principles:

1. **Unidirectional Data Flow:** Data flows in a single direction—actions are dispatched, reducers process them, and the
   store updates the state. This simplifies the data flow, making the application easier to reason about, debug, and
   maintain.
2. **Cohesive State:** You application should group only related state data into the same `Store`/`State` to avoid
   unwieldy state objects and logic. States should only be as large as they are cohesive; meaning you should group as
   much related State together as you can, but not unrelated states.
3. **Object-Oriented**: While traditional Redux focuses on functional programming, **Kdux** integrates
   the principles of functional programming into an object-oriented design. This approach allows for better code reuse,
   reduces cognitive load, and encourages a strong separation of concerns. By using pure functions within an
   object-oriented structure, **Kdux** leverages the predictability and simplicity of functional programming while
   maintaining the modularity and scalability of object-oriented design.
4. **Read-Only State:** The state cannot be modified directly. Instead, actions are dispatched to indicate the intent
   to change the state. This ensures that all state transitions are explicit, traceable, and follow a predictable flow.
5. **Single Source of Truth:** Your cohesive chunk of state is stored in a single object, which ensures consistency and
   provides a clear and accessible snapshot of the application at any point in time.

#### How **Kdux** Differs from Traditional Redux

While **Kdux** is heavily inspired by the traditional [Redux pattern](https://redux.js.org/tutorials/fundamentals/part-1-overview), it introduces several key differences and
enhancements
tailored specifically for Kotlin applications, particularly in environments like Android where proper resource
management
and memory usage are critical, and process death can occur at any time. Below are some of the things that set **Kdux**
apart.

#### Object-Oriented

**Kdux** emphasizes the use of pure functions within an object-oriented design. Unlike traditional Redux, which is
heavily rooted in functional programming paradigms, **Kdux** takes a more object-oriented approach while still
leveraging the benefits of pure functions.

#### Advantages of Object-Oriented Pure Functions

- **Dependency Injection:** Each component (middleware, store, enhancer, reducer) in **Kdux** can be fully decoupled from 
  each other and integrated into Dependency Injection (DI) frameworks like Hilt or Dagger. Functional designs tend to 
  rely on accessing dependencies from outer scopes, which results in monolithic files of spaghettified logic. Using Object-Oriented
  practices to inject dependencies directly into your components **_significantly_** reduces this problem.
- **Separation of Concerns:** The object-oriented structure encourages a clearer separation of concerns, making it
  easier to maintain and scale the application. Pure functional approaches tend to create extremely large files with 
  poor cohesion, while Object-Oriented approaches focus on discrete classes.
- **Ease of Testing:** Pure functions are inherently easier to test, and when combined with object-oriented principles,
  they allow for isolated unit tests that are less brittle and more reliable.
- **Code Reuse:** The encapsulation of pure functions within objects allows for better reuse of logic across different
  areas of the application, reducing redundancy and improving maintainability.
- **Improved Maintainability:** The clear separation of concerns facilitated by object-oriented pure functions leads to
  more maintainable code, as changes to one part of the system are less likely to impact others.

#### Cohesive State; not Global State

Unlike traditional Redux, where the entire application’s state is typically centralized in a single store, **Kdux**
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

#### Advantages of Cohesive State Management

- **Improved Performance:** By managing smaller, focused state objects, you reduce the memory footprint and the
  computational cost associated with state updates and selective serialization/deserialization.
- **Scalability:** As your application grows, you can easily add new stores to manage new features, without affecting
  the existing state management logic.
- **Modularity:** Each store can be developed, tested, and maintained independently, promoting better code organization
  and easier maintenance.
- **Resource Management:** In environments like Android, where resources are constrained, segregated state management
  ensures that only the necessary parts of the state are kept in memory, avoiding unnecessary serialization of the
  entire application state.

#### Synchronous Dispatch and Structured Concurrency

Another key difference in **Kdux** is how it handles the dispatch operation. In traditional Redux, dispatching actions
is
often asynchronous, and each of its middleware/enhancers are asynchronous, which can introduce massive complexities
around
managing race conditions and ensuring that state updates occur in a predictable order.

**Kdux**, however, adheres to Kotlin’s structured concurrency model. In **Kdux**:

- **Synchronous Dispatch by Default:** Dispatch operations are not asynchronous by default. When an action is
  dispatched, it is processed sequentially and predictably within the current coroutine context. Each of its middleware
  and enhancers also obey structured concurrency consequently. This synchronous behavior allows you to (if you wish to do so) 
  ensure that each action is fully processed before the next one begins; eliminating race conditions and 
  making state transitions significantly more predictable.
- **Structured Concurrency:** **Kdux** leverages Kotlin’s structured concurrency to ensure that all state transitions
  and side effects are managed within a defined scope. This means that dispatch operations are always predictable and
  occur
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
  management logic, as you can much more easily account for the complexities of asynchronous action processing.

#### tl;dr

**Kdux** enhances the well-proven [Redux pattern](https://redux.js.org/tutorials/fundamentals/part-1-overview) with a few tweaks to provide a powerful, flexible, and scalable state
management solution for Kotlin projects. By enforcing predictability, centralization, and testability, **Kdux** ensures
that even the most complex applications can maintain a consistent and reliable state management strategy.


<br>
<br>

### Flow of Events in **Kdux**

The flow of events in **Kdux** follows a clear and structured path, ensuring that every state change is intentional and
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

<br>
<br>

## Getting Started

### Installation

Add the dependency for **Kdux** to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.mattshoe.shoebox:Kdux:1.0.10")
}
```

## Usage

Here's a simple example of a [Store](docs/store.md) that manages a "Counter" to track a value:

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
3. Define the store's [reducer](docs/reducer.md). A reducer simply modifies the `State` given an `Action`
    ```kotlin
    // Option 1: Define your Reducer as a class (recommended)
    class CounterReducer : Reducer<CounterState, CounterAction> {
        override suspend fun reduce(state: CounterState, action: CounterAction): CounterState {
            return when (action) {
                is CounterAction.Increment -> state.copy(count = state.count + 1)
                is CounterAction.Decrement -> state.copy(count = state.count - 1)
            }
        }
    }
   
   // Option 2: Define your reducer inline
   val reducer = kdux.reducer { state, action ->
        when (action) {
            is CounterAction.Increment -> state.copy(count = state.count + 1)
            is CounterAction.Decrement -> state.copy(count = state.count - 1)
        }
   }
   ```

4. Define your [Store](docs/store.md). This is done using the [**Kdux** DSL](docs/dsl.md). This allows you to delegate
   to a class, or
   store it in a
   property
   with the same ease.
    ```kotlin
    // Option 1: Create a Store class by delegation using DSL (recommended)
    class CounterStore(
        initialState: CounterState = CounterState(0),
        reducer: Reducer<CounterState, CounterAction> = CounterReducer()
    ): Store<CounterState, CounterAction> 
        by kdux.store(
            initialState, 
            reducer
        )
   
   // Option 2: Create a Store inline with the kdux DSL
    val counterStore = kdux.store(
        initialState = CounterState(0),
        reducer = CounterReducer()
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

<br>
<br>

## Advanced Usage

**Kdux** is designed to be highly flexible and extensible. The documents below will dive deep into how you can leverage
middleware, enhancers, and various other tools to build complex state management solutions that cater to your
specific needs.

- [DevTools](docs/devtools.md)
- [DSL Guide](docs/dsl.md)
- [Using **Kdux** with MVI](docs/kdux_mvi.md)
- [Third Party Integration](docs/third_party_support.md)
- [Testing](docs/testing.md)
- [Middleware](docs/middleware.md)
- [Enhancer](docs/enhancer.md)
- [Reducer](docs/reducer.md)
- [Store](docs/store.md)
- [StoreCreator](docs/store_creator.md)

### Tools

- [DevTools](docs/devtools.md)
- [Persistence](docs/persistence_enhancer.md)
- [Logging](docs/logging_enhancer.md)
- [Authorization](docs/guard_enhancer.md)
- [Error Handling](docs/failsafe_enhancer.md)
- [Timeout](docs/timeout_enhancer.md)
- [Performance Monitoring](docs/performance_enhancer.md)
- [Buffer](docs/buffer_enhancer.md)
- [Debounce](docs/debounce_enhancer.md)
- [Throttle](docs/throttle_enhancer.md)
- [Batch](docs/batch_enhancer.md)

<br>
<br>

# Contributing

Contributors are absolutely welcome! Contributions can come in many forms: bug reports, enhancements, PRs, documentation
requests, you name it.

See our [Contributing](CONTRIBUTING.md) documentation for how to get started!
