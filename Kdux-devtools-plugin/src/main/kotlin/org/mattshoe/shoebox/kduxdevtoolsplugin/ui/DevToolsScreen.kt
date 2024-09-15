package org.mattshoe.shoebox.kduxdevtoolsplugin.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
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
import kotlinx.coroutines.flow.map
import org.mattsho.shoebox.devtools.common.DispatchRequest
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.DevToolsViewModel
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.DispatchLog
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.UiState
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.UserIntent

@Composable
fun DevToolsScreen(
    viewModel: DevToolsViewModel
) {
    val state by viewModel.state.collectAsState()
    val dispatchLogs: List<DispatchLog> by viewModel.dispatchLogStream.collectAsState(emptyList())
    var selectedStore: String? by remember {
        mutableStateOf(null)
    }
    Column {
        when (state) {
            is UiState.DebuggingStopped -> StoreNameInput(viewModel) { store ->
                selectedStore = store
            }
            is UiState.Debugging, is UiState.DebuggingPaused -> {
                DebugWindow(selectedStore, viewModel) {
                    selectedStore?.let {
                        println("Debug Close/Stop Event!! --> $it")
                        viewModel.handleIntent(UserIntent.StopDebugging(it))
                    }
                    selectedStore = null
                }
            }
        }
        DispatchLogList(
            selectedStore,
            dispatchLogs
        ) {
            viewModel.handleIntent(UserIntent.ClearLogs)
        }
    }

}

@Composable
fun StoreNameInput(
    viewModel: DevToolsViewModel,
    onStoreSelected: (String?) -> Unit
) {
    var storeName: String? by remember {
        mutableStateOf(null)
    }
    val options by viewModel.registrationStream.collectAsState(emptyList())
    var selectedStore: String? by remember { mutableStateOf(null) }
    val placeholder = "Select a store to debug"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
            .padding(8.dp)
            .background(Colors.DarkGray),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dropdown(
            modifier = Modifier.weight(1f),
            options = options.map { it.storeName },
            selectedOption = selectedStore,
            placeholder = placeholder,
            onOptionSelected = {
                selectedStore = it
                if (it != placeholder) {
                    storeName = it
                    onStoreSelected(it)
                } else {
                    storeName = null
                    onStoreSelected(null)
                }
            }
        )
        Spacer(Modifier.width(4.dp))
        Disabler(
            isDisabled = storeName == null
        ) {
            DebugIcon(
                modifier = Modifier
                    .size(32.dp)
            ) {
                println("Debug clicked for $storeName")
                storeName?.let {
                    viewModel.handleIntent(UserIntent.StartDebugging(it))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
fun DebugWindow(
    storeName: String?,
    viewModel: DevToolsViewModel,
    onClose: () -> Unit
) {
    val isDebuggingPaused by derivedStateOf {
        viewModel.state.value is UiState.DebuggingPaused
    }
    val incomingDispatch: DispatchRequest? by viewModel.state.map {
        when (it) {
            is UiState.Debugging -> it.dispatchRequest
            else -> null
        }
    }.collectAsState(null)
    val currentState by viewModel.state.map {
        when (it) {
            is UiState.Debugging -> it.currentState?.state
            is UiState.DebuggingPaused -> it.currentState?.state
            else -> null
        }
    }.collectAsState(null)
    val isDisabled by derivedStateOf { incomingDispatch == null }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                StepBackIcon {
                    storeName?.let {
                        viewModel.handleIntent(UserIntent.StepBack(it))
                    }
                }
                Spacer(Modifier.width(24.dp))
                if (isDebuggingPaused) {
                    DebugIcon {
                        storeName?.let {
                            viewModel.handleIntent(UserIntent.StartDebugging(storeName))
                        }
                    }
                } else {
                    Disabler(
                        isDisabled = incomingDispatch == null
                    ) {
                        ContinueIcon {
                            storeName?.let {
                                viewModel.handleIntent(UserIntent.PauseDebugging(storeName))
                            }
                        }
                    }
                }
                Spacer(Modifier.width(24.dp))
                Disabler(
                    isDisabled = incomingDispatch == null
                ) {
                    StepOverIcon {
                        storeName?.let {
                            viewModel.handleIntent(UserIntent.StepOver(storeName))
                        }
                    }
                }

            }
            CloseIcon {
                onClose()
            }
        }

        SectionTitle(
            modifier = Modifier.padding(end = 4.dp),
            title = storeName ?: "UNKNOWN"
        )

        Row {
            MonospaceText(
                text = "CurrentState:",
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            MonospaceText(
                text = currentState?.name ?: "UNKNOWN",
            )
        }

        FormattedCodeBox(
            Modifier.fillMaxWidth(),
            text = currentState?.json ?: "UNKNOWN"
        )

        Spacer(Modifier.height(16.dp))

        SectionTitle(
            modifier = Modifier.padding(end = 4.dp),
            title = "Incoming Dispatch"
        )

        Disabler(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            isDisabled
        ) {
            Column {
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
                Spacer(Modifier.width(4.dp))
                Row {
                    MonospaceText(
                        text = "Action:",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    MonospaceText(
                        text = incomingDispatch?.action?.name ?: "UNKNOWN"
                    )
                }
                EditableCodeBox(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = incomingDispatch?.action?.json ?: "UNKNOWN"
                ) {
                    // TODO
                }
            }
        }
        Spacer(
            modifier = Modifier.fillMaxWidth().height(16.dp)
        )
    }


}


@Composable
fun DispatchLogList(
    selectedStore: String?,
    dispatchLog: List<DispatchLog>,
    onClear: () -> Unit
) {
    // Store the expanded states of the items in a mutable state map
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    val filteredLogs = dispatchLog.filter {
        selectedStore == null || it.result.storeName == selectedStore
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(
            modifier = Modifier.weight(1f),
            title = "Dispatch History"
        )
        // TODO: Get this to work. Buggy for some reason
//        Spacer(Modifier.width(4.dp))
//        TrashIcon {
//            onClear()
//        }
//        Spacer(Modifier.width(8.dp))
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        items(filteredLogs) { log ->
            DispatchLogRow(log, expandedStates)
        }
    }
}


@Composable
fun DispatchLogRow(log: DispatchLog, expandedState: MutableMap<String, Boolean>) {
    val isExpanded = expandedState[log.result.dispatchId] ?: false
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Row with caret
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Toggle the expansion state for the specific log item
                    expandedState[log.result.dispatchId] = !isExpanded
                },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                MonospaceText(text = log.result.timestamp)
                Spacer(Modifier.width(16.dp))
                MonospaceText(text = log.result.storeName)
                Spacer(Modifier.width(16.dp))
                MonospaceText(text = log.result.action.name)
            }
            MonospaceText(text = if (isExpanded) "▲" else "▼")
        }

        // Expanded section with Request and Result JSONs
        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 24.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                Spacer(Modifier.height(4.dp))
                Row {
                    Button(
                        text = "Restore State",
                        icon = {
                            SendIcon {
                                // TODO
                            }
                        }
                    ) {
                        // TODO
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        text = "Replay Action",
                        icon = {
                            SendIcon {
                                // TODO
                            }
                        }
                    ) {

                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        text = "Replay Dispatch",
                        icon = {
                            SendIcon {
                                // TODO
                            }
                        }
                    ) {

                    }
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    MonospaceText(
                        text = "Dispatch ID:",
                        horizontalPadding = 0.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    SelectionContainer {
                        MonospaceText(
                            text = log.result.dispatchId,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row {
                    MonospaceText(
                        text = "Original State:",
                        horizontalPadding = 0.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    SelectionContainer {
                        MonospaceText(
                            text = log.result.request.currentState.name,
                            fontWeight = FontWeight.Bold,
                            horizontalPadding = 0.dp
                        )
                    }
                }
                SelectionContainer {
                    FormattedCodeBox(text = log.result.request.currentState.json)
                }
                Spacer(Modifier.height(8.dp))


                Row {
                    MonospaceText(
                        text = "Action:",
                        horizontalPadding = 0.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    SelectionContainer {
                        MonospaceText(
                            text = log.result.action.name,
                            fontWeight = FontWeight.Bold,
                            horizontalPadding = 0.dp
                        )
                    }
                }
                SelectionContainer {
                    FormattedCodeBox(text = log.result.action.json)
                }
                Spacer(Modifier.height(8.dp))



                Row {
                    MonospaceText(
                        text = "Updated State:",
                        horizontalPadding = 0.dp
                    )
                    Spacer(Modifier.width(16.dp))
                    SelectionContainer {
                        MonospaceText(
                            text = log.result.newState.name,
                            fontWeight = FontWeight.Bold,
                            horizontalPadding = 0.dp
                        )
                    }
                }
                SelectionContainer {
                    FormattedCodeBox(text = log.result.newState.json)
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
    var editableText by remember {
        mutableStateOf(text)
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(text) {
        editableText = text
    }

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
            fontWeight = FontWeight.Bold,

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

@Composable
fun Dropdown(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedOption: String?,
    placeholder: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Colors.DarkGray)
            .padding(8.dp)
            .then(modifier)
    ) {
        // Trigger for the dropdown menu
        Text(
            text = if (selectedOption.isNullOrBlank()) placeholder else selectedOption,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = true
                }
                .padding(4.dp)
                .drawBehind {
                    // Draw the bottom border only
                    val strokeWidth = 1.dp.toPx()  // Thickness of the bottom border
                    val y = size.height  // Bottom position
                    drawLine(
                        color = Color.Gray,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                },
            color = if (selectedOption.isNullOrBlank()) Color.Gray else Colors.LightGray,
            fontSize = 20.sp
        )

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
                .background(Colors.DarkGray)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Colors.DarkGray)
                    ,
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                ) {
                    MonospaceText(
                        text = option,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun Disabler(
    modifier: Modifier = Modifier,
    isDisabled: Boolean,
    contents: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .then(modifier)
            .alpha(if (isDisabled) 0.4f else 1f)  // Reduce opacity when disabled
            .gesturesDisabled(isDisabled)
    ) {
        contents()
    }
}

@Composable
fun Button(
    text: String,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .border(1.5.dp, Colors.LightGray, RoundedCornerShape(6.dp))
            .clickable {
                onClick()
            }
    ) {
        Row(
            Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonospaceText(text = text)
            icon?.let {
                Spacer(Modifier.width(6.dp))
                icon()
            }
        }
    }
}

fun Modifier.gesturesDisabled(disabled: Boolean = true) =
    if (disabled) {
        pointerInput(Unit) {
            awaitPointerEventScope {
                // we should wait for all new pointer events
                while (true) {
                    awaitPointerEvent(pass = PointerEventPass.Initial)
                        .changes
                        .forEach(PointerInputChange::consume)
                }
            }
        }
    } else {
        this
    }
