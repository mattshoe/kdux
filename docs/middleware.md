# Middleware in Kdux

Middleware in Kdux represents a powerful mechanism for intercepting and processing actions as they are dispatched to the
store. Middleware acts as an intermediary between dispatching an action and the reducer that processes it, allowing for
side effects, action modification, logging, or even stopping actions from reaching the reducer.

Middleware is essential for managing side effects in a predictable manner, such as handling asynchronous operations,
logging actions, performing validation, or augmenting actions before they are passed to the reducer. It provides a
mechanism to handle these concerns separately from the core state management logic.

## How Middleware Works

Middleware sits between the action dispatch and the reducer. When an action is dispatched, it first passes through the
middleware chain before reaching the reducer.

1. **Modify Actions**: Middleware can alter the action, such as adding metadata, before passing it along.
2. **Handle Side Effects**: Middleware can perform side effects like network requests, logging, etc.
3. **Intercept and Block Actions**: Middleware can choose to stop the action from reaching the reducer, effectively
   preventing any state change.
4. **Asynchronous Processing**: Middleware can pause the action flow to wait for asynchronous operations (e.g., network
   requests or database operations) to complete.
5. **Chain Actions**: Middleware can dispatch new actions or chain actions together, creating complex workflows.

The middleware chain is processed in a recursive fashion, where each middleware can choose to either pass the action to
the next middleware in the chain or stop the flow entirely.

## Use Cases for Middleware

Middleware is used to separate side effects and additional logic from the core state management logic. Common use cases
for middleware include:

- **Logging**: Log every action that is dispatched and the resulting state after the action is processed.
- **Asynchronous Actions**: Handle async workflows like network requests, debouncing user input, or delaying actions.
- **Validations**: Validate actions before they are processed, ensuring that only valid actions can affect the state.
- **Instrumentation & Tracing**: Notify or augment actions before they are dispatched, adding timestamps or user
  information.
- **Error Handling**: Catch and handle errors that occur during the dispatch of actions, ensuring the application
  remains in a consistent state.
- **Action Filtering**: Block certain actions from reaching the reducer based on logic or application rules.

## Middleware Example

Imagine a logging middleware that prints each action and the resulting state:

```kotlin
class LoggingMiddleware<State : Any, Action : Any> : Middleware<State, Action> {
    override suspend fun apply(store: Store<State, Action>, action: Action, next: suspend (Action) -> Unit) {
        println("Dispatching action: ${action}")
        next(action) // Pass the action to the next middleware or reducer
        println("New state: ${store.state.value}")
    }
}
```

In this example, the middleware intercepts every action that is dispatched, logs it, and then passes it to the next
middleware (or reducer if it is the last middleware in the chain). After the action has been processed, it logs the new
state. This is a basic example of how middleware can extend the store’s behavior without interfering with the reducer
logic.

## Sequence of Events

1. **Action Dispatch**: An action is dispatched to the store.
2. **First Middleware**: The first middleware in the chain receives the action. It can either modify the action, perform
   side effects, or pass it along to the next middleware in the chain.
3. **Middleware Chain**: Each middleware in the chain receives the action and can choose to pass it to the next
   middleware or stop the action.
4. **Reducer**: Once the action has passed through all middleware, it reaches the reducer, where it is used to produce
   the next state of the application.

## Chain of Responsibility

Middleware follows the "Chain of Responsibility" pattern, where each middleware has the responsibility to either pass an
action along to the next handler in the chain or stop it. This can be thought of as a pipeline where actions are
processed sequentially until they either reach the reducer or are stopped by middleware.

## How to Implement Middleware

To implement a custom middleware, you must define the `apply` method, which accepts three parameters:

- **store**: The current `Store` instance. This allows the middleware to access the current state of the store or to
  dispatch further actions if necessary.
- **action**: The action that is being dispatched. The middleware can modify or block the action before it reaches the
  next middleware or the reducer.
- **next**: A function that represents the next step in the middleware chain. Middleware can call `next(action)` to pass
  the action to the next middleware or the reducer. If `next` is not called, the action will not be passed on.

### Example: Async Middleware

Here is an example of middleware that handles asynchronous operations, such as waiting for a network response before
allowing the action to proceed. The example show 3 different ways you could construct your middleware, based on your 
personal programming style and application conventions.

```kotlin
// Option 1: Implement the interface
class AsyncMiddleware<State : Any, Action : Any> : Middleware<State, Action> {
    override suspend fun apply(store: Store<State, Action>, action: Action, next: suspend (Action) -> Unit) {
        if (action is AsyncAction) {
            // Perform the suspending operation
            val result = action.executeAsync()
            // Dispatch the result or pass it to the next middleware
            next(result)
        } else {
            next(action) // Continue with normal actions
        }
    }
}

// Option 2: Use the DSL
val asyncMiddleware = middleware { _, action, next ->
    if (action is AsyncAction) {
        // Perform the suspending operation
        val result = action.executeAsync()
        // Dispatch the result or pass it to the next middleware
        next(result)
    } else {
        next(action) // Continue with normal actions
    }
}

// Option 3: Delegate via DSL
class AsyncMiddleware<State : Any, Action : Any> : Middleware<State, Action>
by middleware(
    { _, action, next ->
        if (action is AsyncAction) {
            // Perform the suspending operation
            val result = action.executeAsync()
            // Dispatch the result or pass it to the next middleware
            next(result)
        } else {
            next(action) // Continue with normal actions
        }
    } 
) 
```

This example middleware checks if the dispatched action is of type AsyncAction. If it is, it waits for the asynchronous
operation to complete before passing the result to the next middleware or reducer. This is an example of how middleware
can manage asynchronous workflows while keeping the rest of the application logic clean and synchronous.

## Conclusion

Middleware provides a powerful mechanism to extend the behavior of a store in a Redux-like architecture by intercepting
actions and handling side effects. It is an essential part of decoupling side effects from state management, allowing
for predictable state updates and testable side-effect management. Middleware can perform tasks like logging,
validation, and async processing, enabling a wide variety of use cases.