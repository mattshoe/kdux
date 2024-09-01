package kdux

import app.cash.turbine.test
import com.google.common.truth.Truth
import kdux.tools.PerformanceData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.mattshoe.shoebox.kdux.Reducer
import kotlin.math.log
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class KduxGlobalTest {

    @Before
    fun before() {
        kdux { clearGlobals() }
    }

    @After
    fun after() {
        kdux { clearGlobals() }
    }

    @Test
    fun globalLogger() = runTest {
        val logs = mutableListOf<Any>()
        kdux {
            globalLogger { logs.add(it) }
        }

        val store = kdux.store<Int, Int>(
            0,
            reducer { state, action ->
                state + action
            }
        )

        store.dispatch(4)
        advanceUntilIdle()

        Truth.assertThat(logs).containsExactly(4)
    }



    @Test
    fun globalPerformanceMonitor() = runTest {
        val data = mutableListOf<PerformanceData<*>>()
        kdux {
            globalPerformanceMonitor {
                data.add(it)
            }
        }

        val store = kdux.store<Int, Int>(
            0,
            reducer { state, action ->
                state + action
            }
        )

        store.dispatch(3)

        Truth.assertThat(data).hasSize(1)
        Truth.assertThat(data.first().action).isEqualTo(3)
    }

    @Test
    fun globalGuardBlocks() = runTest {
        kdux {
            globalGuard {
                false
            }
        }

        val store = kdux.store<Int, Int>(
            0,
            reducer { state, action ->
                state + action
            }
        )
        store.dispatch(3)
        advanceUntilIdle()

        store.state.test {
            // Initial value
            Truth.assertThat(awaitItem()).isEqualTo(0)

            expectNoEvents()
        }
    }

    @Test
    fun globalGuardAllowsPass() = runTest {
        kdux {
            globalGuard { true }
        }

        val store = store<Int, Int>(
            0,
            reducer { state, action ->
                state + action
            }
        )
        store.dispatch(3)
        advanceUntilIdle()

        store.state.test {
            Truth.assertThat(awaitItem()).isEqualTo(3)
            expectNoEvents()
        }
    }
}