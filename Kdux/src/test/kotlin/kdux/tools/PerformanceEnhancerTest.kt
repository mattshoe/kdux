package kdux.tools

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import org.junit.Before
import org.junit.Test
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class PerformanceEnhancerTest {

    private lateinit var store: Store<Int, TestAction>
    private val initialState = 0
    private val loggedPerformanceData = mutableListOf<PerformanceData<TestAction>>()

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
            monitorPerformance { 
                loggedPerformanceData.add(it)
            }
        }
    }

    @Test
    fun `WHEN an action is dispatched THEN its performance is logged`() = runTest {
        store.state.test {
            assertThat(awaitItem()).isEqualTo(0) // Initial state

            store.dispatch(TestAction.Increment)

            assertThat(loggedPerformanceData).hasSize(1)
            val loggedData = loggedPerformanceData.first()
            assertThat(loggedData.storeName).isNotEmpty() // Assuming store name is not empty
            assertThat(loggedData.action).isEqualTo(TestAction.Increment)
            assertThat(loggedData.duration).isGreaterThan(Duration.ZERO)

            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test
    fun `WHEN multiple actions are dispatched THEN all are logged with performance data`() = runTest {
        store.dispatch(TestAction.Increment)
        store.dispatch(TestAction.Increment)

        assertThat(loggedPerformanceData).hasSize(2)
        loggedPerformanceData.forEach { loggedData ->
            assertThat(loggedData.storeName).isNotEmpty()
            assertThat(loggedData.action).isEqualTo(TestAction.Increment)
            assertThat(loggedData.duration).isGreaterThan(Duration.ZERO)
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(2)
        }
    }
}