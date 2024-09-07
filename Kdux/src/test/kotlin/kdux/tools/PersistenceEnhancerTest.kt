package kdux.tools

import app.cash.turbine.test
import com.google.common.truth.Truth
import io.mockk.*
import kdux.KduxMenu
import kdux.caching.CacheUtility
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mattshoe.shoebox.kdux.Reducer
import org.mattshoe.shoebox.kdux.Store
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PersistenceEnhancerTest {

    private lateinit var testStore: Store<TestState, TestAction>
    private val key = "testKey"
    private val testState = TestState("initial")
    private val updatedState = TestState("updated")
    private lateinit var mockFile: File
    private lateinit var mockCacheDir: File
    private lateinit var mockInputStream: FileInputStream
    private lateinit var mockOutputStream: FileOutputStream
    private val dispatcher = StandardTestDispatcher()

    data class TestState(val value: String) : java.io.Serializable

    sealed class TestAction {
        object UpdateState : TestAction()
    }

    private class TestReducer : Reducer<TestState, TestAction> {
        override suspend fun reduce(state: TestState, action: TestAction): TestState {
            return when (action) {
                TestAction.UpdateState -> state.copy(value = "updated")
            }
        }
    }

    @Before
    fun setUp() {
        mockFile = mockk(relaxed = true)
        mockCacheDir = mockk(relaxed = true)
        mockInputStream = mockk(relaxed = true)
        mockOutputStream = mockk(relaxed = true)
        CacheUtility.setCacheDirectory(mockFile)
    }

    @Test
    fun `GIVEN no persisted state WHEN store is initialized THEN it uses the default state`() = runTest(dispatcher) {
        every { mockFile.exists() } returns false

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
                s.write(42)
            },
            deserializer = {
                it.read()
                TestState("loaded")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = { mockOutputStream },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        testStore.state.test {
            val initial = awaitItem()
            Truth.assertThat(initial.value).isEqualTo("initial")
            expectNoEvents()

            verify(exactly = 0) { mockInputStream.read() }
        }
    }

    @Test
    fun `GIVEN persisted state WHEN store is initialized THEN it uses the default currentState`() = runTest(dispatcher) {
        every { mockFile.exists() } returns true

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
                s.write(42)
            },
            deserializer = {
                it.read()
                TestState("loaded")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = { mockOutputStream },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        Truth.assertThat(testStore.currentState.value).isEqualTo("loaded")
    }

    @Test
    fun `GIVEN no persisted state WHEN store is initialized THEN it uses the default currentState`() = runTest(dispatcher) {
        every { mockFile.exists() } returns false

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
                s.write(42)
            },
            deserializer = {
                it.read()
                TestState("loaded")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = { mockOutputStream },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        Truth.assertThat(testStore.currentState.value).isEqualTo("initial")
    }

    @Test
    fun `GIVEN no persisted state AND subscription occurs after first emission THEN only proper value is emitted`() = runTest(dispatcher) {
        every { mockFile.exists() } returns false

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
                s.write(42)
            },
            deserializer = {
                it.read()
                TestState("loaded")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = { mockOutputStream },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        testStore.state.test {
            val initial = awaitItem()
            Truth.assertThat(initial.value).isEqualTo("initial")
            expectNoEvents()

            testStore.dispatch(TestAction.UpdateState)

            Truth.assertThat(awaitItem().value).isEqualTo("updated")
        }

        testStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("updated")
            expectNoEvents()
        }

        testStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("updated")
            expectNoEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN persisted state AND nobody consumes initial emission THEN only future value is emitted`() = runTest(dispatcher) {
        every { mockFile.exists() } returns true

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
                s.write(42)
            },
            deserializer = {
                it.read()
                TestState("loaded")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = { mockOutputStream },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        testStore.dispatch(TestAction.UpdateState)
        advanceUntilIdle()

        testStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("updated")
            expectNoEvents()
        }

        testStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("updated")
            expectNoEvents()
        }

        testStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("updated")
            expectNoEvents()
        }
    }

    @Test
    fun `GIVEN persisted state AND subscription occurs after first emission THEN only proper value is emitted`() = runTest(dispatcher) {
        every { mockFile.exists() } returns true

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
                s.write(42)
            },
            deserializer = {
                it.read()
                TestState("loaded")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = { mockOutputStream },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        testStore.state.test {
            val initial = awaitItem()
            Truth.assertThat(initial.value).isEqualTo("loaded")
            expectNoEvents()

            testStore.dispatch(TestAction.UpdateState)

            Truth.assertThat(awaitItem().value).isEqualTo("updated")
        }

        testStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("updated")
            expectNoEvents()
        }

        testStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("updated")
            expectNoEvents()
        }
    }

    @Test
    fun `GIVEN persisted state exists WHEN store is initialized THEN it recovers the persisted state`() = runTest(dispatcher) {
        every { mockFile.exists() } returns true

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
                s.write(42)
            },
            deserializer = {
                TestState("loaded")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = { mockOutputStream },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        testStore.state.test {
            val restoredState = awaitItem()
            Truth.assertThat(restoredState.value).isEqualTo("loaded")
            expectNoEvents()
        }
    }

    @Test
    @Ignore
    fun `GIVEN state is updated WHEN action is dispatched THEN state is persisted`() = runTest(dispatcher) {
        every { mockFile.exists() } returns false
        every { mockOutputStream.write(any<ByteArray>()) } just Runs
        every { mockOutputStream.close() } just Runs

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
                s.write(42)
            },
            deserializer = {
                TestState("loaded")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = { mockOutputStream },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        testStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("initial")

            testStore.dispatch(TestAction.UpdateState)

            Truth.assertThat(awaitItem().value).isEqualTo("updated")

            verify(exactly = 1) { mockOutputStream.write(42) }
            verify { mockOutputStream.close() }

        }
    }

    @Test
    fun `GIVEN an error during deserialization WHEN store is initialized THEN it falls back to the default state`() = runTest(dispatcher) {
        every { mockFile.exists() } returns true

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
                s.write(42)
            },
            deserializer = {
                throw IOException("Deserialization error")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = { mockOutputStream },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        testStore.state.test {
            val restoredState = awaitItem()
            Truth.assertThat(restoredState.value).isEqualTo("initial")
            expectNoEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN an error during serialization WHEN action is dispatched THEN no state is persisted`() = runTest(dispatcher) {
        every { mockFile.exists() } returns false

        val enhancer = PersistenceEnhancer<TestState, TestAction>(
            key = key,
            serializer = { _, s ->
               s.write(42)
            },
            deserializer = {
                TestState("loaded from cache")
            },
            fileProvider = { mockFile },
            inputStreamProvider = { mockInputStream },
            outputStreamProvider = {
                throw IOException("don't do that!")
            },
            dispatcher = dispatcher
        )

        testStore = kdux.store(
            initialState = testState,
            reducer = TestReducer()
        ) {
            add(enhancer)
        }

        testStore.state.test {
            Truth.assertThat(awaitItem().value).isEqualTo("initial")

            testStore.dispatch(TestAction.UpdateState)

            Truth.assertThat(awaitItem().value).isEqualTo("updated")
        }

        advanceUntilIdle()

        verify(exactly = 0) { mockOutputStream.write(any<ByteArray>()) }
    }
}