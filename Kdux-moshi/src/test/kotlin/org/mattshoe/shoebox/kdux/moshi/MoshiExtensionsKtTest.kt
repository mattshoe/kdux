package org.mattshoe.shoebox.kdux.moshi

import app.cash.turbine.test
import com.google.common.truth.Truth
import kdux.kdux
import kdux.reducer
import kdux.store
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private data class TestState(
    val foo: String,
    val bar: Int
)

private data class TestAction(val value: String)

class MoshiExtensionsKtTest {

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
            TestState("initial", 0),
            reducer { state, action ->
                TestState(action.value, state.bar + 1)
            }
        ) {
            persistWithMoshi("testStore") { _, error ->
                throw error
            }
        }

        initialStore.dispatch(TestAction("subsequent"))

        val subsequentStore = store<TestState, TestAction>(
            TestState("initial", 0),
            reducer { state, action ->
                TestState(action.value, state.bar + 1)
            }
        ) {
            persistWithMoshi("testStore") { _, error ->
                throw error
            }
        }

        subsequentStore.state.test {
            Truth.assertThat(awaitItem().foo).isEqualTo("subsequent")
            expectNoEvents()
        }
    }
}