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
                    .onEach {
                        try {
                            val actionWrapper: org.mattsho.shoebox.devtools.common.Action = Json.decodeFromString(it.payload ?: "")
                            val action = actionDeserializer(actionWrapper)
                            handleServerCommand(action, it, UUID.randomUUID())
                        } catch (e: Throwable) {
                            println(e)
                        }
                    }.launchIn(coroutineScope)

                state
                    .onEach {
                        println("New State --> $it")
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
                println("History at start --> \n\t${history.joinToString("\n\t") { it.toString() }}")
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

            private suspend fun handleServerCommand(action: Action, command: Command, dispatchId: UUID) {
                when (command.command) {
                    "continue" -> handleContinueCommand(action, dispatchId)
                    "pause" -> handlePauseCommand(action, dispatchId)
                    "next" -> handleNextCommand(action, dispatchId)
                    "previous" -> handlePreviousCommand(action, command)
                    "replay" -> handleReplayCommand(action, command)
                    "override" -> handleOverrideCommand(action, command, dispatchId)
                }
            }

            private suspend fun handleContinueCommand(action: Action, dispatchId: UUID) {
                println("Received Continue Command")
                store.dispatch(action)
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

            private suspend fun handlePreviousCommand(action: Action, command: Command) {
                println("Received Previous Command")
                try {
                    val dispatchOverride = historyMutex.withLock {
                        println("History Before --> \n\t${history.joinToString("\n\t") { it.toString() }}")
                        history.removeAt(history.lastIndex).also {
                            println("History After --> \n\t${history.joinToString("\n\t") { it.toString() }}")
                        }
                    }
                    __internalstateOverride[name]
                    forceStateChange(dispatchOverride.state)
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
                    forceStateChange(it.state)
                }
                val dispatch = Snapshot(action, currentState)
                historyMutex.withLock {
                    history.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
            }

            private suspend fun handleOverrideCommand(action: Action, command: Command, dispatchId: UUID) {
                println("Received Override Command")
                val actionContainer = Json.decodeFromString<org.mattsho.shoebox.devtools.common.Action>(command.payload ?: "")
                val actionOverride = actionDeserializer(actionContainer)

                store.dispatch(actionOverride)
                val dispatch = Snapshot(action, currentState)
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

        }
    }
}