package org.mattshoe.shoebox.kdux.gson

import app.cash.turbine.test
import com.google.common.truth.Truth
import kdux.kdux
import kdux.reducer
import kdux.store
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

data class TestState(
    val value: String
)

data class TestAction(val value: String)

class PersistenceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun test() = runTest {
        val cacheLocation = tempFolder.newFolder("cache")
        kdux {
            cacheDir(cacheLocation)
        }

        val initialStore = store<TestState, TestAction>(
            initialState = TestState("initialOriginal"),
            reducer = reducer { state, action ->
                TestState(action.value)
            }
        ) {
            persistWithGson("test") { state, error ->
                println(error)
            }
        }

        initialStore.dispatch(TestAction("subsequentState"))

        advanceUntilIdle()

        initialStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("subsequentState")
        }

        val secondStore = store<TestState, TestAction>(
            initialState = TestState("initialOriginal"),
            reducer = reducer { state, action ->
                TestState(action.value)
            }
        ) {
            persistWithGson("test")
        }

        secondStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("subsequentState")
        }

    }
}