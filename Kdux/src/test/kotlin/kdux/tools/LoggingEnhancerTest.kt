package kdux.tools

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store

@OptIn(ExperimentalCoroutinesApi::class)
class LoggingEnhancerTest {

    private lateinit var store: Store<Int, TestAction>
    private val initialState = 0
    private val loggedActions = mutableListOf<TestAction>()

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
        loggedActions.clear()
        store = kdux.store(
            initialState,
            TestReducer()
        ) {
            log { loggedActions.add(it) }
        }
    }

    @Test
    fun `WHEN an action is dispatched THEN it is logged`() = runTest {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0) // Initial state

            store.dispatch(TestAction.Increment)

            assertThat(loggedActions).containsExactly(TestAction.Increment)
            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test
    fun `WHEN multiple actions are dispatched THEN all are logged`() = runTest {
        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)

        assertThat(loggedActions).containsExactly(
            TestAction.Increment,
            TestAction.Increment,
            TestAction.Increment
        )
        advanceUntilIdle()

        store.state.test {
            assertThat(awaitItem()).isEqualTo(3)
        }
    }
}