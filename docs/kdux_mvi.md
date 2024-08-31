## Using Kdux with MVI Architecture

Kdux can seamlessly integrate with the MVI (Model-View-Intent) architecture to provide a clear, predictable, and
scalable approach to managing application state. This integration ensures that the state management logic remains
decoupled from the view layer while maintaining a reactive flow between user interactions and UI updates.

### Overview of MVI with Kdux

In the MVI architecture:

1. **Model** represents the state of the UI.
2. **View** displays the state and interacts with the user.
3. **Intent** captures user actions or events that represent the user's intentions.

When integrating Kdux with MVI, the primary focus is on keeping the MVI strictly concerned with the view layer, while
leveraging Kdux to manage the underlying application state. Here’s how it works:

### Flow of Interaction

1. **View (UI Layer)**
    - The View interacts with the ViewModel by dispatching `Intent` objects. These intents encapsulate the user's
      actions or events from the UI.

2. **ViewModel (Controller Layer)**
    - The ViewModel exposes a single function `handleIntent` to the View. It receives the `Intent`, maps it to a
      corresponding Kdux `Action`, and dispatches this action to the appropriate Kdux store.
    - The ViewModel then observes the `state` flow from the store to receive state updates. It maps the updated state to
      a `UiState` that the View can display.

3. **Kdux Store (State Management Layer)**
    - The Kdux store manages the application’s state and processes actions dispatched by the ViewModel. Each action is
      processed by the store's reducer, which produces a new state.
    - The store’s `state` flow emits the updated state, which the ViewModel observes and maps to a `UiState`.

### Detailed Interaction Flow

```plaintext
+-----------------+                   +-----------------+                               
|     View        |                   |     View        |                               
| (User Action)   |                   | (Update UI)     |                               
+--------+--------+                   +-----------------+                               
         V                                    ^                
         v                                    ^
+-----------------+                           ^
|   ViewModel     |                   +-----------------+  
|  handleIntent() |                   |    ViewModel    |    
+--------+--------+                   | Maps Kdux State |            
         v                            |   to UiState    |  
         v                            +--------+--------+  
+-----------------+                            ^           
|  Map Intent to  |                            ^
|    Action       |                            ^
+--------+--------+                            ^
         v                            +-----------------+  
         v                            | Store emits new |  
+-----------------+                   |      State      |  
|  Dispatch to    |                   +--------+--------+   
|  Kdux Store     |                            ^
+--------+--------+                            ^
         v                                     ^                  
         v                                     ^                  
+-----------------+                   +-----------------+           
|   Kdux Store    |  >>>>>>>>>>>>>>   |   Kdux Store    | 
| (Process Action)|                   | (Update State)  |           
+--------+--------+                   +--------+--------+           
```

### Justification for this Approach

1. **Separation of Concerns**: By mapping `Intents` to `Actions` in the ViewModel, Kdux allows the ViewModel to act as
   an intermediary between the view and the underlying state management, maintaining a clear separation between the UI
   and business logic.

2. **Scalability**: Each part of the state can be managed by a separate store in Kdux, allowing your application to
   scale gracefully. This modular approach makes it easy to maintain and extend the application.

3. **Predictability and Testability**: Since each `Action` leads to a specific state transition managed by the store’s
   reducer, the state management remains predictable and easy to test. The synchronous dispatch of actions ensures that
   state transitions occur in a defined, sequential manner.

4. **Reactivity**: The `state` flow from Kdux stores ensures that the ViewModel receives real-time updates of the state,
   enabling the View to reactively update the UI. This approach leverages Kotlin's coroutines for smooth, responsive UI
   experiences.

### Example Implementation

Here's an example implementation demonstrating how the View, ViewModel, and Kdux store interact:

```kotlin
// Define the Intent for the UI
sealed interface MyIntent {
   data object LoadData : MyIntent
}



// Define the State for the UI
data class UiState(val isLoading: Boolean, val data: String?)



// Define the Action for the Store
sealed interface MyAction {
   data object FetchData : MyAction
}



// Define the State for the Store
data class MyState(val data: String)



// Define the Reducer for the Store
class MyReducer : Reducer<MyState, MyAction> {
   override suspend fun reduce(state: MyState, action: MyAction): MyState {
      return when (action) {
         is MyAction.FetchData -> state.copy(data = "Hello, Kdux!")
      }
   }
}




// Define the Store
class MyStore: Store<MyState, MyAction>
    by kdux.store(
        initialState = MyState("oh jeez"),
        reducer = MyReducer()
    )




// Implement the ViewModel
@HiltViewModel
class MyViewModel @Inject constructor(
   private val store: MyStore
) : ViewModel() {
    
   // Observe the state from the store and map it to UiState
   val uiState: Flow<UiState> = store.state.map { state ->
      UiState(
         isLoading = false,
         data = state.data
      )
   }.stateIn(
      viewModelScope, 
      SharingStarted.Eagerly, 
      UiState(true, null)
   )

   // Expose a function for the View to send intents
   fun handleIntent(intent: MyIntent) {
      when (intent) {
         is MyIntent.LoadData -> store.dispatch(MyAction.FetchData)
      }
   }
}






// Compose UI
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) { 
   // React to the state exposed by the ViewModel
   val uiState by viewModel.uiState.collectAsState()
   
   Button(
      onClick = { 
          // Send Intent to ViewModel
          viewModel.handleIntent(MyIntent.LoadData) 
      }
   ) {
      Text("Load Data")
   }
   
   Spacer(modifier = Modifier.height(16.dp))
   
   MyText(uiState.data)
}

@Composable
fun MyText(text: String?) {
   Text(text = text)
}
```