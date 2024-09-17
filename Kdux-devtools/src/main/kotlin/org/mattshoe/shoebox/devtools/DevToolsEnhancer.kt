package org.mattshoe.shoebox.devtools

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mattsho.shoebox.devtools.common.ServerRequest
import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattsho.shoebox.devtools.common.DispatchResult
import org.mattshoe.shoebox.devtools.server.ServerClient
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store
import org.mattshoe.shoebox.kdux.__internalstateOverride
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Command
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.CurrentState
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.TimeStamper
import java.util.UUID


private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private data class Snapshot<State: Any, Action: Any>(
    val action: Action?,
    val state: State
)

/**
 * The `DevToolsEnhancer` class enhances a Kdux `Store` by integrating it with an external server-based
 * debugging tool. It enables remote control of the store's state and actions through a WebSocket
 * connection, allowing users to track, replay, and override state transitions and dispatched actions
 * in real time.
 *
 * ### Features:
 *
 * - **Serialization/Deserialization**: The enhancer requires serializer and deserializer functions for
 *   both actions and state. These functions convert actions and state to and from JSON or other formats.
 * - **Real-Time Debugging**: The enhancer communicates with a server using WebSockets, enabling live
 *   inspection of the store's state and actions. Commands sent from the server can trigger specific actions
 *   such as replaying actions or overriding the current state.
 * - **History Tracking**: Keeps track of a history of dispatched actions and the resulting states,
 *   allowing for action replay, time travel, and state restoration.
 *
 * @param actionSerializer A suspend function that serializes an `Action` to a `String` for transmission
 *   over the WebSocket.
 * @param actionDeserializer A suspend function that deserializes a `String` into an `Action` from data
 *   received over the WebSocket.
 * @param stateSerializer A suspend function that serializes the `State` to a `String` for transmission
 *   over the WebSocket.
 * @param stateDeserializer A suspend function that deserializes a `String` into a `State` from data
 *   received over the WebSocket.
 * @param State The type of state managed by the store.
 * @param Action The type of actions that can be dispatched to the store.
 */
class DevToolsEnhancer<State: Any, Action: Any>(
    private val actionSerializer: suspend (Action) -> String,
    private val actionDeserializer: suspend (org.mattsho.shoebox.devtools.common.Action) -> Action,
    private val stateSerializer: suspend (State) -> String,
    private val stateDeserializer: suspend (org.mattsho.shoebox.devtools.common.State) -> State
): Enhancer<State, Action> {
    override fun enhance(store: Store<State, Action>): Store<State, Action> {
        return object : Store<State, Action> {

            private val historyMutex = Mutex()
            private val dispatchMutex = Mutex()
            private val history = mutableListOf<Snapshot<State, Action>>()
            private val dispatchMap = mutableMapOf<UUID, Snapshot<State, Action>>()
            private val socket = ServerClient.startSession(name)
            private var transactionLock: CompletableDeferred<Unit>? = null
            private var bypassServerRequest: Action? = null

            init {
                socket.adHocCommands
                    .onEach { message ->
                        try {
                            handleServerCommand(null, message, UUID.randomUUID())
                        } catch (e: Throwable) {
                            println("Error processing Server Message --> $message")
                            println(e)
                        }
                    }.launchIn(coroutineScope)

                state
                    .onEach { newState ->
                        socket.send(
                            ServerRequest(
                                null,
                                ServerRequest.Type.CURRENT_STATE,
                                Json.encodeToString(
                                    CurrentState(
                                        storeName = name,
                                        org.mattsho.shoebox.devtools.common.State(
                                            newState::class.simpleName ?: "UNKNOWN",
                                            stateSerializer(newState)
                                        )
                                    )
                                )
                            )
                        )
                    }.launchIn(coroutineScope)
                history.add(
                    Snapshot(null, currentState)
                )
            }

            override val name: String
                get() = store.name
            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) = coroutineScope {
                val dispatchId = UUID.randomUUID()
                val request = buildDispatchRequest(action, dispatchId)
                if (bypassServerRequest === action) {
                    handleContinueCommand(action, null, dispatchId)
                } else {
                    val response = socket.awaitResponse(
                        ServerRequest(
                            responseCorrelationId = dispatchId.toString(),
                            type = ServerRequest.Type.DISPATCH_REQUEST,
                            data = Json.encodeToString(request)
                        )
                    )
                    handleServerCommand(action, response, dispatchId)
                }

                socket.send(
                    buildDispatchResult(
                        action,
                        dispatchId,
                        request
                    )
                )
            }

            private suspend fun handleServerCommand(action: Action?, command: Command, dispatchId: UUID) {
                when (command.name) {
                    "continue" -> handleContinueCommand(action, command, dispatchId)
                    "pause" -> handlePauseCommand(action.requireFor(command), dispatchId)
                    "next" -> handleNextCommand(action.requireFor(command), dispatchId)
                    "previous" -> handlePreviousCommand()
                    "replay" -> handleReplayCommand(command)
                    "override" -> handleOverrideCommand(command, dispatchId)
                    "restoreState" -> handleRestoreState(command)
                    "replayAction" -> handleReplayAction(command)
                    "replayDispatch" -> handleReplayDispatch(command)
                }
            }

            private suspend fun handleContinueCommand(action: Action?, command: Command?, dispatchId: UUID) {
                val actionToDispatch = action
                    ?: try {
                        actionDeserializer(
                            Json.decodeFromString<org.mattsho.shoebox.devtools.common.Action>(command?.payload ?: "UNKNOWN")
                        )
                    } catch (e: Throwable) {
                        println(e)
                        throw e
                    }
                store.dispatch(actionToDispatch)
                val dispatch = Snapshot(action, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
            }

            private suspend fun handlePauseCommand(action: Action, dispatchId: UUID) {
                store.dispatch(action)
                val dispatch = Snapshot(action, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
            }

            private suspend fun handleNextCommand(action: Action, dispatchId: UUID) {
                store.dispatch(action)
                val dispatch = Snapshot(action, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
            }

            private suspend fun handlePreviousCommand() {
                try {
                    val dispatchOverride = historyMutex.withLock {
                        history.removeAt(history.lastIndex)
                    }
                    __internalstateOverride[name]
                    forceStateChange(dispatchOverride.state)
                } catch (e: Throwable) {
                    println(e)
                }
            }

            private suspend fun handleReplayCommand(command: Command) {
                val dispatchId = UUID.fromString(command.payload)
                val dispatchToReplay = historyMutex.withLock {
                    dispatchMap[dispatchId]
                }

                dispatchToReplay?.let {
                    forceStateChange(it.state)
                }
                val dispatch = Snapshot<State, Action>(null, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
            }

            private suspend fun handleOverrideCommand(command: Command, dispatchId: UUID = UUID.randomUUID()) {
                val actionContainer = Json.decodeFromString<org.mattsho.shoebox.devtools.common.Action>(command.payload ?: "")
                val actionOverride = actionDeserializer(actionContainer)

                store.dispatch(actionOverride)
                val dispatch = Snapshot(actionOverride, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
            }

            private suspend fun handleRestoreState(command: Command) {
                val dispatch = Json.decodeFromString<DispatchResult>(command.payload ?: "")
                forceStateChange(
                    stateDeserializer(dispatch.newState)
                )
            }

            private suspend fun handleReplayAction(command: Command) {
                val payload = Json.decodeFromString<DispatchResult>(command.payload ?: "")
                val action = actionDeserializer(payload.action)
                bypassServerRequest = action
                this.dispatch(action)
            }

            private suspend fun handleReplayDispatch(command: Command) {
                val dispatch = Json.decodeFromString<DispatchResult>(command.payload ?: "")
                transactionLock = CompletableDeferred()
                try {
                    val stateReset = stateDeserializer(dispatch.previousState)
                    forceStateChange(stateReset)
                    delay(100)
                    val action = actionDeserializer(dispatch.action)
                    bypassServerRequest = action
                    dispatch(action)
                } catch (e: Throwable) {
                    println("Error while executing ReplayDispatch --> $e")
                } finally {
                    transactionLock?.complete(Unit)
                }
            }

            private suspend fun buildDispatchRequest(action: Action, id: UUID): DispatchRequest {
                return DispatchRequest(
                    dispatchId = id.toString(),
                    storeName = name,
                    currentState = org.mattsho.shoebox.devtools.common.State(
                        name = currentState::class.simpleName ?: "UNKNOWN",
                        json = stateSerializer(currentState)
                    ),
                    action = org.mattsho.shoebox.devtools.common.Action(
                        name = action::class.simpleName ?: "UNKNOWN",
                        json = actionSerializer(action)
                    ),
                    ""
                )
            }

            private suspend fun buildDispatchResult(action: Action, id: UUID, request: DispatchRequest): ServerRequest {
                return ServerRequest(
                    responseCorrelationId = null,
                    type = ServerRequest.Type.DISPATCH_RESULT,
                    Json.encodeToString(
                        DispatchResult(
                            dispatchId = id.toString(),
                            storeName = name,
                            request = request,
                            previousState = org.mattsho.shoebox.devtools.common.State(
                                name = request.currentState::class.simpleName ?: "UNKNOWN",
                                json = request.currentState.json
                            ),
                            action = org.mattsho.shoebox.devtools.common.Action(
                                name = request.action.name,
                                json = request.action.json
                            ),
                            newState = org.mattsho.shoebox.devtools.common.State(
                                name = currentState::class.simpleName ?: "UNKNOWN",
                                json = stateSerializer(currentState)
                            ),
                            timestamp = TimeStamper.now()
                        )
                    )
                )
            }

            private fun forceStateChange(state: State) {
                with(__internalstateOverride[name] as? MutableStateFlow<State>) {
                    println("Overriding State --> ${this.toString()} --> $state")
                    this?.update { state }
                }

            }

            private fun Action?.requireFor(command: Command): Action {
                return this ?: throw IllegalArgumentException("The `${command.name}` command requires an `action`, but no action was provided.")
            }

        }
    }
}