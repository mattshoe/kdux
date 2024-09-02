package kdux.tools

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kdux.store
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class ThrottleEnhancerTest {

    private lateinit var store: Store<Int, TestAction>
    private val initialState = 0

    sealed class TestAction {
        object Increment : TestAction()
    }

    private class TestReducer : Reducer<Int, TestAction> {
        override suspend fun reduce(state: Int, action: TestAction): Int {
            return when (action) {
                is TestAction.Increment -> state + 1
            }
        }
    }

    @Before
    fun setUp() {
        store = store(
            initialState = initialState,
            reducer = TestReducer()
        ) {
            throttle(500.milliseconds)
        }
    }

    @Test
    fun `WHEN actions are dispatched rapidly THEN they are throttled correctly`() = runBlocking {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            var timeTaken = measureTime {
                store.dispatch(TestAction.Increment)
                awaitItem()
            }
            assertThat(timeTaken).isLessThan(50.milliseconds)

            timeTaken = measureTime {
                store.dispatch(TestAction.Increment)
                awaitItem()
            }
            assertThat(timeTaken).isGreaterThan(500.milliseconds)
            assertThat(timeTaken).isLessThan(550.milliseconds)

            timeTaken = measureTime {
                store.dispatch(TestAction.Increment)
                awaitItem()
            }
            assertThat(timeTaken).isGreaterThan(500.milliseconds)
            assertThat(timeTaken).isLessThan(550.milliseconds)

            expectNoEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `WHEN dispatch is called in parallel THEN actions are queued and throttled`() = runBlocking {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            val timeTaken = measureTime {
                store.dispatch(TestAction.Increment)
                store.dispatch(TestAction.Increment)
                store.dispatch(TestAction.Increment)

                assertThat(awaitItem()).isEqualTo(1)
                assertThat(awaitItem()).isEqualTo(2)
                assertThat(awaitItem()).isEqualTo(3)
                expectNoEvents()
            }

            assertThat(timeTaken).isGreaterThan(1000.milliseconds)
            assertThat(timeTaken).isLessThan(1100.milliseconds)
        }
    }

    @Test
    fun `WHEN dispatch is called with delay THEN each action is processed immediately`() = runBlocking {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(TestAction.Increment)
            assertThat(awaitItem()).isEqualTo(1)

            delay(600) // Delay longer than the throttle interval

            var timeTaken = measureTime {
                store.dispatch(TestAction.Increment)
                assertThat(awaitItem()).isEqualTo(2)
            }
            assertThat(timeTaken).isLessThan(50.milliseconds)

            delay(600) // Delay longer than the throttle interval

            timeTaken = measureTime {
                store.dispatch(TestAction.Increment)
                assertThat(awaitItem()).isEqualTo(3)
            }
            assertThat(timeTaken).isLessThan(50.milliseconds)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN throttle duration is set to less than zero THEN throw exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ThrottleEnhancer<Int, TestAction>(Duration.ZERO)
        }
        assertThat(exception).hasMessageThat().contains("Throttle interval must be greater than zero.")
    }
}