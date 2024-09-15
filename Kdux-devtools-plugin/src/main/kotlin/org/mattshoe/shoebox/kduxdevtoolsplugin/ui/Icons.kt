package org.mattshoe.shoebox.kduxdevtoolsplugin.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun PlayIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/play_icon.svg",
        description = "Run Dispatches without stopping.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun StopIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/stop_icon.svg",
        description = "Stop Debugging.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun NextIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/next_icon.svg",
        description = "Run next Dispatch.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PreviousIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/previous_icon.svg",
        description = "Replay last Dispatch.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun RecordIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/record_icon.svg",
        description = "Record dispatches.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun DebugIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/debug_icon.svg",
        description = "Start Debugging",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun SendIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/send_icon.svg",
        description = "Replay Dispatch.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun CopyIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/copy_icon.svg",
        description = "Copy Text",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun PauseIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/pause_icon.svg",
        description = "Pause Dispatching.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun CloseIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/close_icon.svg",
        description = "Close Debugging Window.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun StepOverIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/step_over_icon.svg",
        description = "Step over. Executes this dispatch and waits for the next.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun StepBackIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/step_back_icon.svg",
        description = "Step back. Restores the store's previous state.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun TrashIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/trash_icon.svg",
        description = "Delete all.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun ContinueIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray,
    onClick: () -> Unit = { }
) {
    Icon(
        modifier,
        resource = "/continue_icon.svg",
        description = "Continue execution. Executes dispatches without breaking.",
        tint = tint,
        onClick = onClick
    )
}

@Composable
fun DividerIcon(
    modifier: Modifier = Modifier,
    tint: Color = Colors.LightGray
) {
    Icon(
        modifier,
        resource = "/divider.svg",
        description = "Continue execution. Executes dispatches without breaking.",
        tint = tint,
        onClick = { }
    )
}


@Composable
fun Icon(
    modifier: Modifier = Modifier,
    resource: String,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Image(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable {
                onClick()
            }
            .then(modifier),
        painter = painterResource(resource),
        contentDescription = description,
        colorFilter = ColorFilter.tint(tint)
    )
}