package org.mattshoe.shoebox.devtools

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mattsho.shoebox.devtools.common.ServerRequest
import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattsho.shoebox.devtools.common.DispatchResult
import org.mattshoe.shoebox.devtools.server.ServerClient
import org.mattshoe.shoebox.devtools.server.log
import org.mattshoe.shoebox.kdux.Enhancer
import org.mattshoe.shoebox.kdux.Store
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.CommandPayload
import org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common.Registration
import java.util.UUID


private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private data class Dispatch<Action: Any>(
    val id: UUID,
    val action: Action
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
            private val dispatchHistory = mutableListOf<Dispatch<Action>>()
            private val dispatchMap = mutableMapOf<UUID, Dispatch<Action>>()
            private val socket = ServerClient.startSession(name)

            init {
                socket.adHocCommands
                    .onEach {
                        println("AdHoc Request --> $it")
                        // TODO
                    }.launchIn(coroutineScope)
            }

            override val name: String
                get() = store.name
            override val state: Flow<State>
                get() = store.state
            override val currentState: State
                get() = store.currentState

            override suspend fun dispatch(action: Action) = coroutineScope {
                val dispatchId = UUID.randomUUID()
                val dispatch = Dispatch(dispatchId, action)
                historyMutex.withLock {
                    dispatchHistory.add(dispatch)
                    dispatchMap[dispatchId] = dispatch
                }
                val request = buildDispatchRequest(action, dispatchId)
                val response = socket.awaitResponse(
                    ServerRequest(
                        id = dispatchId.toString(),
                        type = ServerRequest.Type.DISPATCH_REQUEST,
                        data = Json.encodeToString(request)
                    )
                )
                println("Response received!!! --> $response")
                handleServerCommand(action, response)
                socket.send(
                    buildDispatchResult(
                        action,
                        dispatchId,
                        request
                    )
                )
            }

            private suspend fun handleServerCommand(action: Action, command: CommandPayload) {
                when (command.command) {
                    "continue" -> handleContinueCommand(action, command)
                    "pause" -> handlePauseCommand(action, command)
                    "next" -> handleNextCommand(action, command)
                    "previous" -> handlePreviousCommand(action, command)
                    "replay" -> handleReplayCommand(action, command)
                    "override" -> handleOverrideCommand(action, command)
                }
            }

            private suspend fun handleContinueCommand(action: Action, command: CommandPayload) {
                println("Received Continue Command")
                store.dispatch(action)
            }

            private suspend fun handlePauseCommand(action: Action, command: CommandPayload) {
                println("Received Pause Command")
                store.dispatch(action)
            }

            private suspend fun handleNextCommand(action: Action, command: CommandPayload) {
                println("Received Next Command")
                store.dispatch(action)
            }

            private suspend fun handlePreviousCommand(action: Action, command: CommandPayload) {
                println("Received Previous Command")
                val dispatchOverride = historyMutex.withLock {
                    dispatchHistory.firstOrNull()
                }

                dispatchOverride?.let {
                    store.dispatch(it.action)
                }
            }

            private suspend fun handleReplayCommand(action: Action, command: CommandPayload) {
                println("Received Replay Command")
                val dispatchId = UUID.fromString(command.data)
                val dispatchToReplay = historyMutex.withLock {
                    dispatchMap[dispatchId]
                }

                dispatchToReplay?.let {
                     store.dispatch(dispatchToReplay.action)
                }
            }

            private suspend fun handleOverrideCommand(action: Action, command: CommandPayload) {
                println("Received Override Command")
                val actionContainer = Json.decodeFromString<org.mattsho.shoebox.devtools.common.Action>(command.data ?: "")
                val actionOverride = actionDeserializer(actionContainer)

                store.dispatch(actionOverride)
            }

            private suspend fun buildDispatchRequest(action: Action, id: UUID): DispatchRequest {
                println("Building Dispatch Request  --  State --> ${currentState::class.simpleName}  ||  Action --> ${action::class.simpleName}")
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
                            timestamp = ""
                        )
                    )
                )

            }

        }


    }

}