package kdux

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kdux.tools.PerformanceData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class StoreDslIntegrationTest {

    private lateinit var store: Store<Int, TestAction>
    private val initialState = 0

    sealed class TestAction {
        object Increment : TestAction()
        object Decrement : TestAction()
    }

    private class TestReducer : Reducer<Int, TestAction> {
        override suspend fun reduce(state: Int, action: TestAction): Int {
            return when (action) {
                TestAction.Increment -> state + 1
                TestAction.Decrement -> state - 1
            }
        }
    }

    @Before
    fun setUp() {
        KduxMenu().clearGlobals() // Clear any global configurations before each test
    }

    @Test
    fun `WHEN action is dispatched THEN state is updated correctly`() = runTest {
        store = store(
            initialState = initialState,
            reducer = TestReducer()
        )

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Increment)
            assertThat(awaitItem()).isEqualTo(1)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN logging enhancer is added THEN actions are logged`() = runTest {
        val loggedActions = mutableListOf<TestAction>()

        store = store(
            initialState = initialState,
            reducer = TestReducer()
        ) {
            log { action -> loggedActions.add(action) }
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Increment)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(loggedActions).containsExactly(TestAction.Increment)
            expectNoEvents()
        }
    }

    @Test
    fun `WHEN performance enhancer is added THEN performance is monitored`() = runTest {
        val performanceLogs = mutableListOf<PerformanceData<TestAction>>()

        store = store(
            initialState = initialState,
            reducer = TestReducer()
        ) {
            monitorPerformance { data -> performanceLogs.add(data) }
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Increment)
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(performanceLogs).hasSize(1)
            assertThat(performanceLogs.first().action).isEqualTo(TestAction.Increment)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN debounce enhancer is added THEN actions are debounced`() = runBlocking {
        store = store(
            initialState = initialState,
            reducer = TestReducer()
        ) {
            debounce(500.milliseconds)
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Increment)
            store.dispatch(TestAction.Decrement)
            delay(550) // Delay to exceed debounce duration
            store.dispatch(TestAction.Decrement) // Trigger flush

            // Only one of first 2 dispatches should be emitted, so it increments by 1
            assertThat(awaitItem()).isEqualTo(1)
            // Third dispatch was outside debounce window, should dispatch immediately
            assertThat(awaitItem()).isEqualTo(0)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN buffer enhancer is added THEN actions are buffered and dispatched together`() = runTest {
        store = store(
            initialState = initialState,
            reducer = TestReducer()
        ) {
            buffer(3)
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Increment)
            expectNoEvents()
            store.dispatch(TestAction.Increment)
            expectNoEvents()
            store.dispatch(TestAction.Increment)

            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(2)
            assertThat(awaitItem()).isEqualTo(3)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN global guard is added THEN actions are blocked`() = runTest {
        KduxMenu().globalGuard { action -> action is TestAction.Increment } // Only allow Increment actions

        store = store(
            initialState = initialState,
            reducer = TestReducer()
        )

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Decrement)
            expectNoEvents() // Action is blocked

            store.dispatch(TestAction.Increment)
            assertThat(awaitItem()).isEqualTo(1)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN multiple enhancers and middleware are added THEN store behaves correctly`() = runTest {
        val loggedActions = mutableListOf<TestAction>()
        val performanceLogs = mutableListOf<PerformanceData<TestAction>>()

        store = store(
            initialState = initialState,
            reducer = TestReducer()
        ) {
            log { action -> loggedActions.add(action) }
            monitorPerformance { data -> performanceLogs.add(data) }
            buffer(2)
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Increment)
            expectNoEvents()
            store.dispatch(TestAction.Decrement)

            assertThat(awaitItem()).isEqualTo(1) // Buffered actions are dispatched together
            assertThat(awaitItem()).isEqualTo(0)

            assertThat(loggedActions).hasSize(2)
            assertThat(performanceLogs).hasSize(2)

            expectNoEvents()
        }
    }
}