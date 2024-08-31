import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.mattshoe.shoebox.kdux.*
import kotlin.time.measureTime

sealed interface SampleState {
    data object Loading: SampleState
    data object Success: SampleState
    data object Failure: SampleState
}



sealed interface SampleAction {
    data object Foo: SampleAction
    data object Bar: SampleAction
}



class SampleMiddleware: Middleware<SampleState, SampleAction> {
    override suspend fun apply(
        store: Store<SampleState, SampleAction>,
        action: SampleAction,
        next: suspend (SampleAction) -> Unit
    ) {
        val elapsedTime = measureTime {
            next(action)
        }
        println("Action processing took ${elapsedTime.inWholeMilliseconds}ms")
    }
}



class SampleEnhancer: Enhancer<SampleState, SampleAction> {
    override fun enhance(store: Store<SampleState, SampleAction>): Store<SampleState, SampleAction> {
        return object : Store<SampleState, SampleAction> {
            private val history = mutableListOf<SampleAction>()

            override val state: Flow<SampleState>
                get() = store.state
                    .filter {
                        it is SampleState.Failure
                    }

            override val currentState: SampleState
                get() = store.currentState

            override suspend fun dispatch(action: SampleAction) {
                history.add(action)
                store.dispatch(action)
            }
        }
    }
}


class SampleReducer: Reducer<SampleState, SampleAction> {
    override suspend fun reduce(state: SampleState, action: SampleAction): SampleState {
        return when (state) {
            is SampleState.Loading -> SampleState.Success
            is SampleState.Failure -> SampleState.Loading
            is SampleState.Success -> SampleState.Success
        }
    }
}

val store2 = store(
    SampleState.Success,
    SampleReducer()
) {

}

class SampleStore(
    reducer: Reducer<SampleState, SampleAction>
): Store<SampleState, SampleAction>
by store(
    SampleState.Success,
    reducer,
    {
        add(
            SampleMiddleware(),
            SampleMiddleware()
        )
        add(
            SampleEnhancer(),
            SampleEnhancer()
        )
    }
)
