package org.mattshoe.shoebox.devtools

import com.google.gson.Gson
import kdux.middleware
import kdux.reducer
import kdux.store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import kotlin.random.Random

data class TestAction(val value: Int)
data class TestState(val value: Int)

val gson = Gson()

@Ignore("Enable this test for manually testing the Kdux Devtools plugin")
class DevToolsEnhancerTest {

    @Test
    fun test() = runBlocking {
        repeat(1) { storeNumber ->
            launch {
                delay(storeNumber * 1000L)
                val store = store<TestState, TestAction>(
                    initialState = TestState(0),
                    reducer = reducer { state, action ->
                        TestState(state.value + action.value).also {
                            println("New Test State --> $it")
                        }
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

//                    add(
//                        middleware { store, action, next ->
//                            if (store.currentState.value < 3){
//                                store.dispatch(TestAction(10))
//                            }
//                        }
//                    )
                }

                repeat (20) {
                    delay(1000)
                    store.dispatch(
                        TestAction(
                            Random.nextInt(10)
                        )
                    )
                }
            }
        }

        while (true) {
            delay(1000)
        }
    }
}