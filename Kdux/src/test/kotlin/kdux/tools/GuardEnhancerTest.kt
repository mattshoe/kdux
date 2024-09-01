package kdux.tools

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kdux.reducer
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mattshoe.shoebox.kdux.Store

class GuardEnhancerTest {

    private lateinit var store: Store<Int, TestAction>

    sealed class TestAction {
        object AllowedAction : TestAction()
        object BlockedAction : TestAction()
    }

    @Before
    fun setUp() {
        store = kdux.store(
            initialState = 0,
            reducer = reducer { state, action ->
                when (action) {
                    is TestAction.AllowedAction -> state + 1
                    else -> state
                }
            }
        ) {
            guard { action -> action is TestAction.AllowedAction }
        }
    }

    @Test
    fun `WHEN allowed action is dispatched THEN state is updated`() = runTest {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.AllowedAction)

            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test
    fun `WHEN blocked action is dispatched THEN state is not updated`() = runTest {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.BlockedAction)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN multiple actions are dispatched THEN only allowed actions are processed`() = runTest {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.AllowedAction)
            store.dispatch(TestAction.BlockedAction)
            store.dispatch(TestAction.AllowedAction)

            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(2)
            expectNoEvents()
        }
    }
}