package kdux

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kdux.DefaultStoreIntegrationTest.Action
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mattshoe.shoebox.kdux.*
import org.mattshoe.shoebox.kdux.DefaultStore

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultStoreIntegrationTest {
    private lateinit var store: Store<Int, Action>
    private val dispatcher = StandardTestDispatcher()

    sealed interface Action {
        data object Increment : Action
        data object Decrement : Action
    }

    @Before
    fun setup() {
        store = DefaultStore(initialState = 0, reducer = SimpleReducer())
    }

    // region Routine Tests

    @Test
    fun `WHEN store is initialized THEN state should be 0`() = runTest(dispatcher) {
        val initialState = store.state.first()
        assertThat(initialState).isEqualTo(0)
    }

    @Test
    fun `WHEN increment action is dispatched THEN state should increase by 1`() = runTest(dispatcher) {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test
    fun `WHEN decrement action is dispatched THEN state should decrease by 1`() = runTest(dispatcher) {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)

            store.dispatch(Action.Decrement)
            assertThat(awaitItem()).isEqualTo(0)
        }
    }

    @Test
    fun `WHEN multiple actions are dispatched THEN state should reflect all changes`() = runTest(dispatcher) {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(2)

            store.dispatch(Action.Decrement)
            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test
    fun `WHEN middleware is applied THEN actions should pass through middleware before reducing`() = runTest(dispatcher) {
        val middleware = LoggingMiddleware()
        store = DefaultStore(initialState = 0, reducer = SimpleReducer(), middlewares = listOf(middleware))

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)

            store.dispatch(Action.Decrement)
            assertThat(awaitItem()).isEqualTo(0)
        }

        assertThat(middleware.loggedActions).containsExactly(Action.Increment, Action.Decrement)
    }

    @Test
    fun `WHEN no middleware is present THEN actions should be reduced directly`() = runTest(dispatcher) {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test
    fun `WHEN state is observed THEN state flow should emit each state change`() = runTest(dispatcher) {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)

            store.dispatch(Action.Decrement)
            assertThat(awaitItem()).isEqualTo(0)
        }
    }

    // endregion

    // region Enhancer Tests

    @Test
    fun `WHEN enhancer is applied THEN increment action should double increment the state`() = runTest(dispatcher) {
        val enhancer = IncrementalEnhancer()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(enhancer)
            .build()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(2)
        }
    }

    @Test
    fun `WHEN enhancer is applied THEN decrement action should not be affected`() = runTest(dispatcher) {
        val enhancer = IncrementalEnhancer()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(enhancer)
            .build()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Decrement)
            assertThat(awaitItem()).isEqualTo(-1)
        }
    }

    @Test
    fun `WHEN enhancer and middleware are applied THEN middleware logs all actions including enhancer generated`() = runTest(dispatcher) {
        val enhancer = IncrementalEnhancer()
        val middleware = LoggingMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(middleware)
            .add(enhancer)
            .build()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(2)
        }

        assertThat(middleware.loggedActions).containsExactly(Action.Increment, Action.Increment)
    }

    // endregion
    
    // region Middleware Tests

    @Test
    fun `WHEN blocking middleware is applied THEN decrement action should be blocked`() = runTest(dispatcher) {
        val blockingMiddleware = BlockingMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(blockingMiddleware)
            .build()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Decrement)
            expectNoEvents()
        }
    }

    @Test
    fun `WHEN modifying middleware is applied THEN increment action should be modified to decrement`() = runTest(dispatcher) {
        val modifyingMiddleware = ModifyingMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(modifyingMiddleware)
            .build()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(-1)
        }
    }

    @Test
    fun `WHEN logging middleware is applied THEN all actions should be logged`() = runTest(dispatcher) {
        val loggingMiddleware = LoggingMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(loggingMiddleware)
            .build()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            store.dispatch(Action.Decrement)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(0)
        }

        assertThat(loggingMiddleware.loggedActions).containsExactly(Action.Increment, Action.Decrement)
    }

    @Test
    fun `WHEN side effect middleware is applied THEN side effect should be performed`() = runTest(dispatcher) {
        val sideEffectMiddleware = SideEffectMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(sideEffectMiddleware)
            .build()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)
        }

        assertThat(sideEffectMiddleware.sideEffectPerformed).isTrue()
    }

    @Test
    fun `WHEN async middleware is applied THEN actions should be delayed but still processed`() = runTest(dispatcher) {
        val asyncMiddleware = AsyncMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(asyncMiddleware)
            .build()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            val start = currentTime
            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(currentTime - start).isEqualTo(100)
        }
    }

    @Test
    fun `WHEN multiple middleware are applied THEN they should execute in order`() = runTest(dispatcher) {
        val loggingMiddleware = LoggingMiddleware()
        val modifyingMiddleware = ModifyingMiddleware()

        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(loggingMiddleware)
            .add(modifyingMiddleware)
            .build()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(-1)
        }

        // Ensure that logging middleware captured the original action before it was modified
        assertThat(loggingMiddleware.loggedActions).containsExactly(Action.Increment)
    }

    @Test
    fun `WHEN middleware dispatches additional action THEN both actions are processed in correct order`() = runTest {
        val dispatchingMiddleware = DispatchingMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(dispatchingMiddleware)
            .build()

        store.state.test {
            store.dispatch(Action.Increment)

            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(0) // Decrement action dispatched by middleware
        }
    }

    @Test
    fun `WHEN middleware dispatches multiple additional actions THEN all actions are processed in correct order`() = runTest {
        val multipleDispatchingMiddleware = MultipleDispatchingMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(multipleDispatchingMiddleware)
            .build()

        store.state.test {
            store.dispatch(Action.Increment)

            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(0) // First Decrement action dispatched by middleware
            assertThat(awaitItem()).isEqualTo(-1) // Second Decrement action dispatched by middleware
        }
    }
    
    // endregion

    // region Structured Concurrency Tests

    @Test
    fun `WHEN multiple actions are dispatched THEN they should be processed in the order they are dispatched`() = runTest {
        val loggingMiddleware = LoggingMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(loggingMiddleware)
            .build()

        store.state.test {
            store.dispatch(Action.Increment)
            store.dispatch(Action.Decrement)
            store.dispatch(Action.Increment)

            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(1)
        }

        assertThat(loggingMiddleware.loggedActions).containsExactly(
            Action.Increment, Action.Decrement, Action.Increment
        ).inOrder()
    }

    @Test
    fun `WHEN an action is delayed THEN subsequent actions should wait until the current action is processed`() = runTest {
        val delayMiddleware = AsyncMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(delayMiddleware)
            .build()

        store.state.test {
            store.dispatch(Action.Increment)
            store.dispatch(Action.Decrement)

            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(0)
        }
    }

    @Test
    fun `WHEN an exception occurs THEN it should not affect subsequent actions`() = runTest {
        val exceptionMiddleware = ExceptionMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(exceptionMiddleware)
            .build()

        store.state.test {
            store.dispatch(Action.Increment)

            try {
                store.dispatch(Action.Decrement)
            } catch (e: IllegalStateException) {
                // Expected exception
            }

            store.dispatch(Action.Increment)

            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(2) // Increment after exception should still process
        }
    }

    @Test
    fun `WHEN an action is canceled THEN it should not affect other actions`() = runTest {
        val cancellableMiddleware = CancellableMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(cancellableMiddleware)
            .build()

        val job = launch {
            store.dispatch(Action.Decrement) // This will block indefinitely
        }

        delay(100) // Give it a little time to start

        job.cancel() // Cancel the action

        store.state.test {
            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test
    fun `WHEN middleware launches coroutines THEN logic is processed in the correct order`() = runTest {
        val middleware = CoroutineOrderingMiddleware()
        store = Store.Builder(initialState = 0, reducer = SimpleReducer())
            .add(middleware)
            .build()

        store.state.test {
            store.dispatch(Action.Increment)
            assertThat(awaitItem()).isEqualTo(0)
            assertThat(awaitItem()).isEqualTo(1)

            store.dispatch(Action.Decrement)
            assertThat(awaitItem()).isEqualTo(0)
        }

        assertThat(middleware.executionOrder).containsExactly(
            "before: Increment",
            "dispatched: Increment",
            "after: Increment",
            "before: Decrement",
            "dispatched: Decrement",
            "after: Decrement"
        ).inOrder()
    }

    // endregion
}

class SimpleReducer : Reducer<Int, Action> {
    override suspend fun reduce(state: Int, action: Action): Int {
        return when (action) {
            is Action.Increment -> state + 1
            is Action.Decrement -> state - 1
        }
    }
}

class LoggingMiddleware : Middleware<Int, Action> {
    val loggedActions = mutableListOf<Action>()

    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        loggedActions.add(action)
        next(action)
    }
}

class IncrementalEnhancer : Enhancer<Int, Action> {
    override fun enhance(store: Store<Int, Action>): Store<Int, Action> {
        return object : Store<Int, Action> by store {
            override suspend fun dispatch(action: Action) {
                store.dispatch(action)
                if (action is Action.Increment) {
                    store.dispatch(Action.Increment)
                }
            }
        }
    }
}

class BlockingMiddleware : Middleware<Int, Action> {
    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        // Block any Decrement action
        if (action !is Action.Decrement) {
            next(action)
        }
    }
}

class ModifyingMiddleware : Middleware<Int, Action> {
    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        if (action is Action.Increment) {
            // Modify Increment action to behave like Decrement
            next(Action.Decrement)
        } else {
            next(action)
        }
    }
}

class SideEffectMiddleware : Middleware<Int, Action> {
    var sideEffectPerformed = false

    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        if (action is Action.Increment) {
            sideEffectPerformed = true
        }
        next(action)
    }
}

class AsyncMiddleware : Middleware<Int, Action> {
    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        if (action is Action.Increment) {
            delay(100)
        }
        next(action)
    }
}

class CancellableMiddleware : Middleware<Int, Action> {
    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        withContext(Dispatchers.IO) {
            if (action is Action.Decrement) {
                delay(Long.MAX_VALUE) // Simulate long-running or stuck action
            } else {
                next(action)
            }
        }
    }
}

class ExceptionMiddleware : Middleware<Int, Action> {
    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        if (action is Action.Decrement) {
            throw IllegalStateException("Test Exception")
        } else {
            next(action)
        }
    }
}

class CoroutineOrderingMiddleware : Middleware<Int, Action> {
    val executionOrder = mutableListOf<String>()

    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        coroutineScope {
            launch {
                executionOrder.add("before: $action")
                delay(500)
                executionOrder.add("after: $action")
            }
            delay(100)
            next(action)
            executionOrder.add("dispatched: $action")
        }
    }
}

class DispatchingMiddleware : Middleware<Int, Action> {
    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        if (action is Action.Increment) {
            // Dispatch an additional Decrement action after the Increment action
            next(action)
            store.dispatch(Action.Decrement)
        } else {
            next(action)
        }
    }
}

class MultipleDispatchingMiddleware : Middleware<Int, Action> {
    override suspend fun apply(store: Store<Int, Action>, action: Action, next: suspend (Action) -> Unit) {
        next(action)
        if (action is Action.Increment) {
            // Dispatch two additional Increment actions after the first Increment action
            store.dispatch(Action.Decrement)
            store.dispatch(Action.Decrement)
        }
    }
}