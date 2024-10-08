# Enhancer in Kdux

An `Enhancer` in Kdux is an interface that modifies or extends the behavior of a `Store`. Enhancers are used to add new
capabilities or fundamentally change how a store operates by wrapping the store and modifying its internal behavior.

## Enhancer Lifecycle

1. **Store Creation**: The enhancer is applied when the store is created (usually via a builder or factory). The
   enhancer wraps the store with its extended functionality.
2. **Action Dispatch**: The enhanced store processes actions according to the logic provided by the enhancer. This could
   involve batching actions, modifying the dispatch process, or adding additional steps before or after an action is
   processed.
3. **State Observation**: The enhanced store may modify how the state is accessed or introduce new behavior when the
   state changes. This allows for features like automatic persistence or complex caching strategies.
4. **Extended Capabilities**: The enhancer may introduce new methods or capabilities to the store, such as time-travel
   debugging, batching, persistence, etc.

## Enhancers vs. Middleware

While middleware is used to intercept and manipulate actions as they are dispatched (typically on a per-action basis),
an `Enhancer` operates at a higher level by modifying the store itself. An enhancer can change how the
store's `dispatch` and `state` properties behave globally, introducing new logic that affects the entire lifecycle of
action processing and state management.

### Key Differences:

- **Middleware** handles actions one at a time and passes them through a chain, while an enhancer can modify how the
  store as a whole handles multiple actions, alters the flow of state management, or extends the store with new methods
  and behaviors that can't be achieved by middleware alone.
- **Enhancers** are typically used when you need to add complex behavior to a store that cannot be easily achieved with
  middleware.

### Examples of Enhancer Use Cases:

- **Action Batching**: Collect multiple actions into a batch and only update the state once after all actions have been
  processed.
- **Time-Travel Debugging**: Add the ability to move backward and forward through different states by replaying actions.
- **State Persistence**: Automatically save and restore the state of the store to and from local storage or a remote
  server.
- **Enhanced Performance Metrics**: Measure and log the time it takes for each action to be processed by the store,
  including how long reducers take to execute.

## How Enhancers Work

An enhancer takes an existing store and returns a new store with modified or extended behavior. The enhancer wraps the
store, potentially modifying the `dispatch` function to intercept actions, changing how the state is accessed, or adding
new capabilities to the store, such as batching, persistence, or other cross-cutting concerns.

### Chaining Enhancers:

- Multiple enhancers can be chained together to apply layers of behavior modification to a store. Each enhancer wraps
  the store created by the previous enhancer, allowing you to compose complex behavior in a modular and reusable way.

## Example: Action Buffering Enhancer

An example of behavior that cannot be achieved through middleware is a `BufferEnhancer`, which collects actions in a buffer
until a size limit is reach, then dispatches them all at once:

```
class BufferEnhancer<State: Any, Action: Any>(
    private val bufferSize: Int
): Enhancer<State, Action> {
    init {
        require(bufferSize > 0) {
            "Buffer size must be greater than zero."
        }
    }

    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {
            private val bufferMutex = Mutex()
            private val buffer = mutableListOf<Action>()
            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) {
                val actionsToDispatch = mutableListOf<Action>()
                bufferMutex.withLock {
                    buffer.add(action)

                    if (buffer.size >= bufferSize) {
                        actionsToDispatch.addAll(buffer)
                        buffer.clear()
                    }
                }
                actionsToDispatch.forEach {
                    store.dispatch(it)
                }
            }
        }
    }
}
```

In this example, the `BufferEnhancer` delays dispatch of any actions until the buffer is full. Once full, all actions
are dispatched sequentially.

## Conclusion

Enhancers in Kdux allow you to apply cross-cutting concerns that are beyond the scope of what middleware can achieve and
can fundamentally change how the store behaves. They provide a powerful mechanism for extending or modifying store
behavior, enabling advanced features like action batching, state persistence, and performance monitoring.