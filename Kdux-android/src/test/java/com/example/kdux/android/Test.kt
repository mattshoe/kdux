package com.example.kdux.android

import android.os.Parcelable
import app.cash.turbine.test
import com.google.common.truth.Truth
import kdux.kdux
import kdux.reducer
import kdux.store
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Parcelize
data class TestState(
    val value: String
): Parcelable

data class TestAction(val value: String)

@RunWith(RobolectricTestRunner::class)
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
            persistAsParcelable("test") { state, error ->
                throw error
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
            persistAsParcelable("test") { state, error ->
                throw error
            }
        }

        secondStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("subsequentState")
        }

    }
}