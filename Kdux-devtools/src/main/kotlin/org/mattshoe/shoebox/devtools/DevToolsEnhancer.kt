package org.mattshoe.shoebox.devtools

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mattsho.shoebox.devtools.common.Action
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
                        println("Sending New Store State -> $newState")
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
                println("starting dispatch -- currentState --> $currentState")
                val request: DispatchRequest
                dispatchMutex.withLock {
                    request = buildDispatchRequest(action, dispatchId)
                    println("sending request --> $request")
                    val response = socket.awaitResponse(
                        ServerRequest(
                            responseCorrelationId = dispatchId.toString(),
                            type = ServerRequest.Type.DISPATCH_REQUEST,
                            data = Json.encodeToString(request)
                        )
                    )
                    println("response received --> $response")
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
                }
            }

            private suspend fun handleContinueCommand(action: Action?, command: Command, dispatchId: UUID) {
                println("Received Continue Command")
                val actionToDispatch = action
                    ?: try {
                        actionDeserializer(
                            Json.decodeFromString<org.mattsho.shoebox.devtools.common.Action>(command.payload ?: "UNKNOWN")
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
                println("Received Pause Command")
                store.dispatch(action)
                val dispatch = Snapshot(action, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
            }

            private suspend fun handleNextCommand(action: Action, dispatchId: UUID) {
                println("Received Next Command")
                store.dispatch(action)
                val dispatch = Snapshot(action, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
            }

            private suspend fun handlePreviousCommand() {
                println("Received Previous Command")
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
                println("Received Replay Command")
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
                println("Received Override Command")
                val actionContainer = Json.decodeFromString<org.mattsho.shoebox.devtools.common.Action>(command.payload ?: "")
                val actionOverride = actionDeserializer(actionContainer)

                store.dispatch(actionOverride)
                val dispatch = Snapshot(actionOverride, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
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