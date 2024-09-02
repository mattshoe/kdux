package kdux.tools

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kdux.reducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class TimeoutEnhancerTest {

    @Test
    fun `WHEN action dispatches within timeout THEN it is processed successfully`() = runTest {
        val store = kdux.store<Int, Int>(
            initialState = 0,
            reducer = reducer { state, action -> state + action }
        ) {
            timeout(1.seconds)
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            store.dispatch(3)
            assertThat(awaitItem()).isEqualTo(3)

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN action dispatch exceeds timeout THEN dispatch is canceled`() = runTest {
        val store = kdux.store<Int, Int>(
            initialState = 0,
            reducer = reducer { state, action ->
                delay(2000) // Simulate a long-running operation
                state + action
            }
        ) {
            timeout(500.milliseconds)
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            try {
                store.dispatch(3)
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(TimeoutCancellationException::class.java)
            }

            expectNoEvents()
        }
    }

    @Test
    fun `WHEN multiple actions are dispatched THEN only those within timeout are processed`() = runTest {
        val store = kdux.store<Int, suspend () -> Unit>(
            initialState = 0,
            reducer = reducer { state, action ->
                action()
                state + 1
            }
        ) {
            timeout(500.milliseconds)
        }

        store.state.test {
            assertThat(awaitItem()).isEqualTo(0)

            // Don't exceed timeout
            store.dispatch {
                delay(499)
            }

            assertThat(awaitItem()).isEqualTo(1)

            try {
                store.dispatch {
                    delay(501)
                }
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(TimeoutCancellationException::class.java)
            }
            expectNoEvents()
        }
    }

    @Test
    fun `WHEN duration is set to zero THEN throw exception`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            TimeoutEnhancer<Int, Int>(0.milliseconds)
        }
        assertThat(exception).hasMessageThat().contains("Timeout must be greater than zero.")
    }
}