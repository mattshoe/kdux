package org.mattshoe.shoebox.devtools

import com.google.gson.Gson
import kdux.kdux
import kdux.reducer
import kdux.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

data class TestAction(val value: Int)
data class TestState(val value: Int)

val gson = Gson()

class DevToolsEnhancerTest {

    @Test
    fun test() = runBlocking {

        val store = store<TestState, TestAction>(
            initialState = TestState(0),
            reducer = reducer { state, action ->
                TestState(action.value + state.value)
            }
        ) {
            name("test")
            devtools(
                {
                    gson.toJson(it)
                },
                {
                    gson.fromJson(it.json, TestAction::class.java)
                },
                {
                    gson.toJson(it)
                },
                {
                    gson.fromJson(it.json, TestState::class.java)
                }
            )
        }

        repeat (20) {
            delay(1000)
            store.dispatch(TestAction(1))
        }
    }
}