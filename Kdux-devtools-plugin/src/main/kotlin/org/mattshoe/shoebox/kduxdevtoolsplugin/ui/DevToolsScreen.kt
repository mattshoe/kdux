package org.mattshoe.shoebox.kduxdevtoolsplugin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.DevToolsViewModel
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.DispatchLog
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.State
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.UserIntent

@Composable
fun DevToolsScreen(
    viewModel: DevToolsViewModel
) {
    val state by viewModel.state.collectAsState()
    when (state) {
        is State.Stopped -> StoreNameInput(viewModel)
        is State.Debugging -> DebugWindow((state as State.Debugging).storeName, viewModel)
    }
}

@Composable
fun StoreNameInput(
    viewModel: DevToolsViewModel
) {
    var storeName: String by remember {
        mutableStateOf("")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
            .padding(8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            value = storeName,
            onValueChange = { newText ->
                storeName = newText
            },
            singleLine = true,
            trailingIcon = {
                DebugIcon(
                    modifier = Modifier
                        .size(24.dp)
                ) {
                    viewModel.handleIntent(UserIntent.StartDebugging(storeName))
                }
            },
            label = { Text("Store Name") }
        )
    }
}

@Composable
fun DebugWindow(
    storeName: String,
    viewModel: DevToolsViewModel
) {
    val dispatchLog: List<DispatchLog> by viewModel.dispatchStream.collectAsState(emptyList())
    val isPaused by derivedStateOf {
        val currentState = viewModel.state.value
        currentState is State.Debugging && currentState.paused
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Bottom)
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Spacer(Modifier.width(8.dp))
                PreviousIcon {
                    viewModel.handleIntent(UserIntent.PreviousDispatch(storeName))
                }
                Spacer(Modifier.width(8.dp))
                if (isPaused) {
                    PlayIcon {
                        viewModel.handleIntent(UserIntent.StartDebugging(storeName))
                    }
                } else {
                    PauseIcon {
                        viewModel.handleIntent(UserIntent.PauseDebugging(storeName))
                    }
                }
                Spacer(Modifier.width(8.dp))
                NextIcon {
                    viewModel.handleIntent(UserIntent.NextDispatch(storeName))
                }
            }
            CloseIcon {
                viewModel.handleIntent(UserIntent.StopDebugging)
            }
        }

        DispatchLogList(dispatchLog)
    }

}


@Composable
fun DispatchLogList(dispatchLog: List<DispatchLog>) {
    // Store the expanded states of the items in a mutable state map
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        items(dispatchLog) { log ->
            val isExpanded by remember {
                derivedStateOf {
                    expandedStates[log.request.timestamp] ?: false
                }
            }

            println("${log.request.dispatchId}::recomposed --> $isExpanded")

            DispatchLogRow(log, expandedStates)
        }
    }
}


@Composable
fun DispatchLogRow(log: DispatchLog, expandedState: MutableMap<String, Boolean>) {
    val isExpanded = expandedState[log.request.dispatchId] ?: false
    println("${log.request.dispatchId}::isExpanded --> $isExpanded")
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Row with caret
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Toggle the expansion state for the specific log item
                    println("${log.request.dispatchId}::setIsExpanded --> ${isExpanded.not()}")
                    expandedState[log.request.dispatchId] = !isExpanded
                },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                MonospaceText(text = log.request.timestamp)
                Spacer(Modifier.width(16.dp))
                MonospaceText(text = log.request.storeName)
                Spacer(Modifier.width(16.dp))
                MonospaceText(text = log.request.action.name)
            }
            MonospaceText(text = if (isExpanded) "▲" else "▼")
        }

        // Expanded section with Request and Result JSONs
        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                Row {
                    MonospaceText(
                        text = "Dispatch ID:",
                        fontWeight = FontWeight.Bold,
                        horizontalPadding = 0.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    SelectionContainer {
                        MonospaceText(text = log.request.dispatchId)
                    }
                }

                Row {
                    MonospaceText(
                        text = "Action:",
                        fontWeight = FontWeight.Bold,
                        horizontalPadding = 0.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    SelectionContainer {
                        MonospaceText(text = log.request.action.name)
                    }
                }

                MonospaceText(
                    text = "Current State",
                    fontWeight = FontWeight.Bold,
                    horizontalPadding = 0.dp
                )
                SelectionContainer {
                    FormattedCodeBox(text = log.request.currentState.json)
                }
                Spacer(Modifier.height(8.dp))

                MonospaceText(
                    text = "Request",
                    fontWeight = FontWeight.Bold,
                    horizontalPadding = 0.dp
                )
                SelectionContainer {
                    FormattedCodeBox(text = log.request.action.json)
                }
                Spacer(Modifier.height(8.dp))
                MonospaceText(
                    text = "Result",
                    fontWeight = FontWeight.Bold,
                    horizontalPadding = 0.dp
                )
                SelectionContainer {
                    FormattedCodeBox(text = log.result?.action?.json ?: "UNKNOWN")
                }
            }
        }
    }
}

@Composable
fun MonospaceText(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    horizontalPadding: Dp = 6.dp,
    verticalPadding: Dp = 6.dp
) {
    Text(
        text = text,
        modifier = Modifier
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .then(modifier),
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            color = Color.LightGray
        ),
        fontWeight = fontWeight
    )
}

@Composable
fun FormattedCodeBox(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 12.sp
) {
    val clipboardManager = LocalClipboardManager.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Colors.DarkGray)
            .then(modifier)
    ) {

        MonospaceText(
            text = text,
            fontSize = fontSize
        )

        CopyIcon(
            modifier = Modifier
                .size(20.dp)
                .padding(top = 3.dp)
                .align(Alignment.TopEnd),
        ) {
            clipboardManager.setText(AnnotatedString(text))
        }
    }
}