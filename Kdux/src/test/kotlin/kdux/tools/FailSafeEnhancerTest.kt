package kdux.tools

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import kdux.store
import kotlinx.coroutines.test.advanceUntilIdle
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store

class FailSafeEnhancerTest {

    private lateinit var store: Store<Int, TestAction>
    private val initialState = 0

    sealed class TestAction {
        object Increment : TestAction()
        object Fail : TestAction()
        object Decrement: TestAction()
    }

    private class TestReducer : Reducer<Int, TestAction> {
        override suspend fun reduce(state: Int, action: TestAction): Int {
            return when (action) {
                is TestAction.Increment -> state + 1
                is TestAction.Decrement -> state - 1
                is TestAction.Fail -> throw RuntimeException("Forced failure")
            }
        }
    }

    @Before
    fun setUp() {
        store = store(
            initialState,
            TestReducer()
        ) {
            onError { state, action, error, dispatch ->
                // If an error occurs, dispatch an Increment action as a recovery
                if (action is TestAction.Fail) {
                    dispatch(TestAction.Increment)
                }
            }
        }
    }

    @Test
    fun `WHEN an action fails THEN recovery action is dispatched`() = runTest {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Fail)
            assertThat(awaitItem()).isEqualTo(1) // Recovery action increments the state

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN an action succeeds THEN no additional actions are dispatched`() = runTest {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Decrement)
            assertThat(awaitItem()).isEqualTo(-1)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN multiple actions fail THEN recovery actions are dispatched`() = runTest {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Fail)
            assertThat(awaitItem()).isEqualTo(1)

            store.dispatch(TestAction.Fail)
            assertThat(awaitItem()).isEqualTo(2)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN no recovery action is provided THEN state remains unchanged after failure`() = runTest {
        val logs = mutableListOf<Any>()
        store = store(
            initialState,
            TestReducer()
        ) {
            onError { _, action, _, _ ->
                logs.add(action)
            }
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Fail)
            expectNoEvents() // State remains unchanged due to failure
            advanceUntilIdle()
            assertThat(logs).containsExactly(TestAction.Fail)

            store.dispatch(TestAction.Increment)
            assertThat(awaitItem()).isEqualTo(1)
        }
    }
}