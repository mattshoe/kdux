package org.mattshoe.shoebox.kduxdevtoolsplugin.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.DevToolsViewModel
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.State
import org.mattshoe.shoebox.kduxdevtoolsplugin.viewmodel.UserIntent

@Composable
@Preview
fun DevToolsScreen(
    viewModel: DevToolsViewModel
) {
    val state by viewModel.state.collectAsState()

    when (state) {
        is State.Stopped -> StoreNameInput(viewModel)
        is State.Debugging -> DebugWindow(viewModel)
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
    viewModel: DevToolsViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(Alignment.Bottom)
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        Spacer(Modifier.width(8.dp))
        PreviousIcon { }
        Spacer(Modifier.width(8.dp))
        StopIcon {
            viewModel.handleIntent(UserIntent.StopDebugging)
        }
        Spacer(Modifier.width(8.dp))
        PlayIcon { }
        Spacer(Modifier.width(8.dp))
        NextIcon { }
    }
}