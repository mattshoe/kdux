import app.cash.turbine.test
import com.google.common.truth.Truth
import kdux.kdux
import kdux.reducer
import kdux.store
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.mattshoe.shoebex.kdux.kotlinx.serialization.persistWithKotlinxSerialization
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@Serializable
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
            persistWithKotlinxSerialization("test") { state, error ->
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
            persistWithKotlinxSerialization("test")
        }

        secondStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("subsequentState")
        }

    }
}