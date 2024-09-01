package kdux.tools

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store
import kotlin.test.assertFailsWith
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class BufferEnhancerTest {

    private lateinit var store: Store<Int, TestAction>
    private val initialState = 0

    sealed class TestAction {
        object Increment : TestAction()
    }

    private class TestReducer : Reducer<Int, TestAction> {
        override suspend fun reduce(state: Int, action: TestAction): Int {
            return when (action) {
                TestAction.Increment -> state + 1
            }
        }
    }

    @Before
    fun setUp() {
        store = kdux.store(
            initialState,
            TestReducer()
        ) {
            buffer(size = 3)
        }
    }

    @Test
    fun `WHEN buffer size is reached THEN actions are dispatched`() = runTest {
        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)
            expectNoEvents()
        }
        store.dispatch(TestAction.Increment)
        advanceUntilIdle()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(3)
            expectNoEvents() // No further state changes
        }
    }

    @Test
    fun `WHEN buffer size is exceeded THEN new actions are buffered separately`() = runTest {
        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)
        advanceUntilIdle()
        store.state.test {
            assertThat(awaitItem()).isEqualTo(3) // First batch is dispatched
        }

        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)

        store.state.test {
            assertThat(awaitItem()).isEqualTo(3) // Nothing else dispatched yet
            expectNoEvents()
        }

        store.dispatch(TestAction.Increment)
        advanceUntilIdle()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(6) // Second batch is dispatched
        }
    }

    @Test
    fun `WHEN dispatch is called in parallel THEN actions are buffered and dispatched correctly`() = runTest {
        launch { store.dispatch(TestAction.Increment) }
        launch { store.dispatch(TestAction.Increment) }
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)
            expectNoEvents()
        }
        launch { store.dispatch(TestAction.Increment) }
        advanceUntilIdle()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(3)
        }

        launch { store.dispatch(TestAction.Increment) }
        launch { store.dispatch(TestAction.Increment) }
        store.state.test {
            assertThat(awaitItem()).isEqualTo(3)
            expectNoEvents()
        }
        launch { store.dispatch(TestAction.Increment) }
        advanceUntilIdle()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(6) // The next batch is dispatched together
        }
    }

    @Test
    fun `WHEN buffer size is set to zero THEN throw exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            BufferEnhancer<Int, TestAction>(0)
        }
        assertThat(exception).hasMessageThat().contains("Buffer size must be greater than zero.")
    }
}