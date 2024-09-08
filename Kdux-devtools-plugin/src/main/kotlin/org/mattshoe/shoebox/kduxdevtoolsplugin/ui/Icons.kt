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