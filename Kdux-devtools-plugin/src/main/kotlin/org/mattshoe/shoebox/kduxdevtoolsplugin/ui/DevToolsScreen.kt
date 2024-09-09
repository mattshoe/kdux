package org.mattshoe.shoebox.kduxdevtoolsplugin.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.DevToolsViewModel
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.DispatchLog
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.State
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.UserIntent

@Composable
fun DevToolsScreen(
    viewModel: DevToolsViewModel
) {
    val state by viewModel.state.collectAsState()
    val dispatchLog: List<DispatchLog> by viewModel.dispatchStream.collectAsState(emptyList())
    Column {
        when (state) {
            is State.Stopped -> StoreNameInput(viewModel)
            is State.Debugging, is State.Paused -> {
                val storeName = (state as? State.Debugging)?.storeName ?: (state as State.Paused).storeName
                DebugWindow(storeName, viewModel)
            }
        }
        DispatchLogList(dispatchLog)
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

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun DebugWindow(
    storeName: String,
    viewModel: DevToolsViewModel
) {
    val isPaused by derivedStateOf {
        viewModel.state.value is State.Paused
    }
    val incomingDispatch by viewModel.debugStream.collectAsState(null)
    val isDisabled by derivedStateOf { incomingDispatch == null }

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
                viewModel.handleIntent(UserIntent.StopDebugging(storeName))
            }
        }


        SectionTitle(
            modifier = Modifier.padding(end = 4.dp),
            title = "Incoming Dispatch Request"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .alpha(if (isDisabled) 0.4f else 1f)  // Reduce opacity when disabled
                .pointerInput(isDisabled) {  // Block input if disabled
                    if (isDisabled) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()  // Consume all touch events
                            }
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalAlignment = Alignment.Bottom,
            ) {

                Column(
                    Modifier.fillMaxHeight()
                ) {
                    Row {
                        MonospaceText(
                            text = "ID:",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        SelectionContainer {
                            MonospaceText(
                                text = incomingDispatch?.dispatchId ?: ""
                            )
                        }
                    }
                    Row {
                        MonospaceText(
                            text = "Store:",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        SelectionContainer {
                            MonospaceText(
                                text = incomingDispatch?.storeName ?: ""
                            )
                        }
                    }

                    Column(
                        Modifier.fillMaxWidth(0.33f)
                    ) {
                        MonospaceText(
                            text = "CurrentState:",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        SelectionContainer {
                            FormattedCodeBox(
                                text = Json.encodeToString(incomingDispatch?.currentState)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(Modifier.padding(horizontal = 8.dp)) {
                        EditableCodeBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            """
                        fjdkslaf;jdkslafjdklsa
                        fdjksalfjdjkafdafdsafdsafdsafdsafdsafdsafjkdl;ajfkdlsa;jflkds;afjdklsa
                        fjdklas;fkda
                        fjdksla;fjdkslafdsafdsafjkld;safjkld;ajfkdl;ajfkdl;ajfdkla;jfi9pqwnfjker;ahjgfioerwq;jafkoahgi9ropnagiroapnbgirangioa[ngir9peahgiroangioreuaphgioreqp
                        fjkdsla;fdklsa;
                        
                        df
                        dsafdsafdsa
                        fdsa
                        fdsafdsafdsa
                        f
                        dsafdsafd
                        saf
                        dsa
                        fdsafdsafdsfjdkslaf;jdkslafjdklsa
fdjksalfjdjkafdafdsafdsafdsafdsafdsafdsafjkdl;ajfkdlsa;jflkds;afjdklsa
fjdklas;fkda
fjdksla;fjdkslafdsafdsafjkld;safjkld;ajfkdl;ajfkdl;ajfdkla;jfi9pqwnfjker;ahjgfioerwq;jafkoahgi9ropnagiroapnbgirangioa[ngir9peahgiroangioreuaphgioreqp
fjkdsla;fdklsa;

df
dsafdsafdsa
fdsa
fdsafdsafdsa
f
dsafdsafd
saf
dsa
fdsafdsafds
af
d
af
dsa
fds

fdsa
fdsa
d
as
fdsafd
sa
                        af
                        d
                        af
                        dsa
                        fds
                        
                        fdsa
                        fdsa
                        d
                        as
                        fdsafd
                        sa
                    """.trimIndent()
                        ) {

                        }
                    }
                }
            }
            Spacer(
                modifier = Modifier.fillMaxWidth().height(16.dp)
            )
        }
    }



}


@Composable
fun DispatchLogList(dispatchLog: List<DispatchLog>) {
    // Store the expanded states of the items in a mutable state map
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    SectionTitle(title = "Dispatch History")
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
            Column(modifier = Modifier.padding(start = 24.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
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
fun EditableCodeBox(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = 12.sp,
    onTextChange: (String) -> Unit // To handle text updates
) {
    val clipboardManager = LocalClipboardManager.current
    var editableText by remember { mutableStateOf(text) }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Colors.DarkGray)
            .verticalScroll(verticalScrollState)  // Add vertical scroll
            .horizontalScroll(horizontalScrollState)  // Add horizontal scroll
            .then(modifier)
    ) {
        BasicTextField(
            value = editableText,
            onValueChange = {
                editableText = it
                onTextChange(it)  // Notify about text changes
            },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,  // Retain monospace font
                fontSize = fontSize,
                color = Color.LightGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)  // Adjust padding for better visuals
        )

        CopyIcon(
            modifier = Modifier
                .size(20.dp)
                .padding(top = 3.dp)
                .align(Alignment.TopEnd),
        ) {
            clipboardManager.setText(AnnotatedString(editableText))
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

@Composable
fun SectionTitle(
    modifier: Modifier = Modifier,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = title,
            color = Color.LightGray,
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))  // Space after text
        Divider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .padding(end = 4.dp),
            color = Color.Gray
        )
    }
}