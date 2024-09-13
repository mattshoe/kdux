package org.mattshoe.shoebox.devtools

import com.google.gson.Gson
import kdux.reducer
import kdux.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

data class TestAction(val value: Int)
data class TestState(val value: Int)

val gson = Gson()

class DevToolsEnhancerTest {

    @Test
    fun test() = runBlocking {
        repeat(1) { storeNumber ->
            launch {
                val store = store<TestState, TestAction>(
                    initialState = TestState(0),
                    reducer = reducer { state, action ->
                        TestState(action.value + state.value)
                    }
                ) {
                    name("TestStore$storeNumber")
                    devtools(
                        actionSerializer = {
                            gson.toJson(it)
                        },
                        actionDeserializer = {
                            gson.fromJson(it.json, TestAction::class.java)
                        },
                        stateSerializer = {
                            gson.toJson(it)
                        },
                        stateDeserializer = {
                            gson.fromJson(it.json, TestState::class.java)
                        }
                    )
                }

                repeat (10) {
                    delay(1000)
                    store.dispatch(TestAction(1))
                }
            }
        }

        while (true) {
            delay(100)
        }
    }
}