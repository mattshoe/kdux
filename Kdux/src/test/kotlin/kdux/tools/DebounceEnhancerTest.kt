package kdux.tools

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class DebounceEnhancerTest {

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
            debounce(500.milliseconds)
        }
    }

    @Test
    fun `WHEN dispatch is called rapidly THEN only one action is processed`() = runTest {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Increment)
            store.dispatch(TestAction.Increment)
            store.dispatch(TestAction.Increment)
            store.dispatch(TestAction.Increment)
            
            assertThat(awaitItem()).isEqualTo(1)

            advanceTimeBy(2000) // Advance time to exceed debounce duration

             // Only the first action is processed
            expectNoEvents() // No further state changes
        }
    }

    @Test
    fun `WHEN dispatch is called with sufficient delay THEN actions are processed`() = runBlocking {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            println("dispatching first increment")
            store.dispatch(TestAction.Increment)
            println("dispatched first increment")
            assertThat(awaitItem()).isEqualTo(1)

            println("advancing 2000")
            delay(700)

            println("dispatching second increment")
            store.dispatch(TestAction.Increment)
            println("second done")
            assertThat(awaitItem()).isEqualTo(2)
        }
    }

    @Test
    fun `WHEN dispatch is called in parallel THEN only one action is processed`() = runBlocking {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            launch { store.dispatch(TestAction.Increment) }
            launch { store.dispatch(TestAction.Increment) }
            launch { store.dispatch(TestAction.Increment) }
            delay(700) // Advance time to exceed debounce duration

            assertThat(awaitItem()).isEqualTo(1)
            expectNoEvents()
        }
    }

    @Test
    fun `WHEN duration is set to less than zero THEN throw exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            DebounceEnhancer<Int, TestAction>(0.seconds)
        }
        assertThat(exception).hasMessageThat().contains("Debounce duration must be greater than zero.")
    }

    @Test
    fun `WHEN multiple actions dispatched with delays THEN correct actions processed`() = runBlocking {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Increment)
            delay(700) // Advance time to exceed debounce duration
            assertThat(awaitItem()).isEqualTo(1)

            delay(500) // Advance less than debounce duration
            store.dispatch(TestAction.Increment)
            delay(500) // Now exceed the debounce duration
            assertThat(awaitItem()).isEqualTo(2)

            store.dispatch(TestAction.Increment)
            delay(1000) // Advance time to exceed debounce duration
            assertThat(awaitItem()).isEqualTo(3)
        }
    }
}