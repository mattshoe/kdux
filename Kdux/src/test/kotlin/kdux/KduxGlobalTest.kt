package kdux

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kdux.tools.FailSafeEnhancerTest
import kdux.tools.PerformanceData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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
    fun globalErrorHandlerInvokedOnError() = runTest {
        val capturedErrors = mutableListOf<Throwable>()
        kdux {
            globalErrorHandler { _, _, error ->
                capturedErrors.add(error)
            }
        }

        val store = kdux.store<Int, Int>(
            0,
            reducer { _, _ ->
                throw IllegalStateException("Test exception")
            }
        )

        store.dispatch(3)
        advanceUntilIdle()

        assertThat(capturedErrors).hasSize(1)
        assertThat(capturedErrors.first()).isInstanceOf(IllegalStateException::class.java)
        assertThat(capturedErrors.first()).hasMessageThat().isEqualTo("Test exception")
    }

    @Test
    fun globalErrorHandlerDoesNotBlockDispatch() = runTest {
        val capturedErrors = mutableListOf<Throwable>()
        kdux {
            globalErrorHandler { _, _, error ->
                capturedErrors.add(error)
            }
        }

        val store = kdux.store<Int, Int>(
            0,
            reducer { state, action ->
                if (action == 3) {
                    throw IllegalArgumentException("Intentional error")
                }
                state + action
            }
        )

        store.dispatch(3)
        advanceUntilIdle()

        store.state.test {
            // Despite the error, initial state should remain intact
            assertThat(awaitItem()).isEqualTo(0)
            expectNoEvents()
        }
    }

    @Test
    fun globalErrorHandlerCapturesMultipleErrors() = runTest {
        val capturedErrors = mutableListOf<Throwable>()
        kdux {
            globalErrorHandler { _, _, error ->
                capturedErrors.add(error)
            }
        }

        val store = kdux.store<Int, Int>(
            0,
            reducer { state, action ->
                if (action % 2 == 0) {
                    throw IllegalStateException("Even number error")
                }
                state + action
            }
        )

        store.dispatch(2)
        store.dispatch(4)
        advanceUntilIdle()

        assertThat(capturedErrors).hasSize(2)
        assertThat(capturedErrors[0]).hasMessageThat().contains("Even number error")
        assertThat(capturedErrors[1]).hasMessageThat().contains("Even number error")
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

        assertThat(logs).containsExactly(4)
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

        assertThat(data).hasSize(1)
        assertThat(data.first().action).isEqualTo(3)
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
            assertThat(awaitItem()).isEqualTo(0)

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
            assertThat(awaitItem()).isEqualTo(3)
            expectNoEvents()
        }
    }
}