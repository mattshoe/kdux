package kdux.tools

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class BatchEnhancerTest {

    private lateinit var store: Store<Int, TestAction>
    private val initialState = 0

    sealed class TestAction {
        object Increment : TestAction()
    }

    private class TestReducer() : Reducer<Int, TestAction> {

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
            batched(500.milliseconds)
        }
    }

    @Test
    fun `WHEN actions dispatched within batch duration THEN they are batched and dispatched together`() = runBlocking {
        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)

        delay(600) // Exceed batch duration

        store.dispatch(TestAction.Increment) // Trigger this batch flush

        store.state.test {
            assertThat(awaitItem()).isEqualTo(4)
        }
    }

    @Test
    fun `WHEN actions are dispatched with delay exceeding batch duration THEN they are processed separately`() = runBlocking {
        store.dispatch(TestAction.Increment)
        delay(600) // Exceed batch duration
        store.dispatch(TestAction.Increment) // Trigger flush
        delay(100)
        store.state.test {
            assertThat(awaitItem()).isEqualTo(2)
        }

        store.dispatch(TestAction.Increment)
        delay(600) // Exceed batch duration
        store.dispatch(TestAction.Increment) // Trigger flush
        store.state.test {
            assertThat(awaitItem()).isEqualTo(4)
        }
    }

    @Test
    fun `WHEN dispatch is called in parallel THEN actions are batched correctly`() = runBlocking {
        launch { store.dispatch(TestAction.Increment) }
        launch { store.dispatch(TestAction.Increment) }
        launch { store.dispatch(TestAction.Increment) }

        delay(600) // Exceed batch duration
        store.dispatch(TestAction.Increment) // trigger flush

        store.state.test {
            assertThat(awaitItem()).isEqualTo(4)
        }
    }

    @Test
    fun `WHEN batch duration is less than or equal to zero THEN throw exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            BatchEnhancer<Int, TestAction>(0.milliseconds)
        }
        assertThat(exception).hasMessageThat().contains("Batch duration must be greater than zero.")
    }

    @Test
    fun `WHEN multiple batches of actions are dispatched THEN they are processed in correct intervals`() = runBlocking {
        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)

        delay(600) // Exceed batch duration
        store.dispatch(TestAction.Increment) // trigger flush
        store.state.test {
            assertThat(awaitItem()).isEqualTo(3)
        }

        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)

        delay(500) // Exceed batch duration
        store.dispatch(TestAction.Increment) // trigger flush
        store.state.test {
            assertThat(awaitItem()).isEqualTo(6)
        }
    }
}