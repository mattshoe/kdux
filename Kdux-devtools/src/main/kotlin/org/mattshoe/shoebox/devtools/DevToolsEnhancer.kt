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
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Command
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
            private val history = mutableListOf<Snapshot<State, Action>>()
            private val dispatchMap = mutableMapOf<UUID, Snapshot<State, Action>>()
            private val socket = ServerClient.startSession(name)
            private var _currentState: State = store.currentState
            private val stateOverride = MutableSharedFlow<State>()

            init {
                socket.adHocCommands
                    .onEach {
                        // TODO
                    }.launchIn(coroutineScope)

                state
                    .onEach {
                        _currentState = it
                    }.launchIn(coroutineScope)
                history.add(
                    Snapshot(null, currentState)
                )
            }

            override val name: String
                get() = store.name
            override val state: Flow<State>
                get() = merge(
                    store.state,
                    stateOverride
                )
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) = coroutineScope {
                val dispatchId = UUID.randomUUID()
                val request = buildDispatchRequest(action, dispatchId)
                val response = socket.awaitResponse(
                    ServerRequest(
                        id = dispatchId.toString(),
                        type = ServerRequest.Type.DISPATCH_REQUEST,
                        data = Json.encodeToString(request)
                    )
                )
                handleServerCommand(action, response)
                val dispatch = Snapshot(action, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
                socket.send(
                    buildDispatchResult(
                        action,
                        dispatchId,
                        request
                    )
                )
            }

            private suspend fun handleServerCommand(action: Action, command: Command) {
                when (command.command) {
                    "continue" -> handleContinueCommand(action, command)
                    "pause" -> handlePauseCommand(action, command)
                    "next" -> handleNextCommand(action, command)
                    "previous" -> handlePreviousCommand(action, command)
                    "replay" -> handleReplayCommand(action, command)
                    "override" -> handleOverrideCommand(action, command)
                }
            }

            private suspend fun handleContinueCommand(action: Action, command: Command) {
                println("Received Continue Command")
                store.dispatch(action)
            }

            private suspend fun handlePauseCommand(action: Action, command: Command) {
                println("Received Pause Command")
                store.dispatch(action)
            }

            private suspend fun handleNextCommand(action: Action, command: Command) {
                println("Received Next Command")
                store.dispatch(action)
            }

            private suspend fun handlePreviousCommand(action: Action, command: Command) {
                println("Received Previous Command")
                try {
                    val dispatchOverride = historyMutex.withLock {
                        history[history.lastIndex - 1]
                    }
                    stateOverride.emit(dispatchOverride.state)
                } catch (e: Throwable) {
                    println(e)
                }
            }

            private suspend fun handleReplayCommand(action: Action, command: Command) {
                println("Received Replay Command")
                val dispatchId = UUID.fromString(command.payload)
                val dispatchToReplay = historyMutex.withLock {
                    dispatchMap[dispatchId]
                }

                dispatchToReplay?.let {
                    stateOverride.emit(it.state)
                }
            }

            private suspend fun handleOverrideCommand(action: Action, command: Command) {
                println("Received Override Command")
                val actionContainer = Json.decodeFromString<org.mattsho.shoebox.devtools.common.Action>(command.payload ?: "")
                val actionOverride = actionDeserializer(actionContainer)

                store.dispatch(actionOverride)
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
                    id = null,
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

        }
    }
}