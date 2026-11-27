package com.amusic.core.theming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.formatDuration

/** Media controls and timelines stay left-to-right in every language. */
@Composable
fun LeftToRight(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)

@Composable
fun PlayPauseIcon(showPause: Boolean, modifier: Modifier = Modifier) = Icon(
    painterResource(if (showPause) R.drawable.ic_pause else R.drawable.ic_play),
    stringResource(if (showPause) R.string.player_pause else R.string.player_play),
    modifier,
)

@Composable
fun PreviousButton(actions: PlayerActions, iconSize: Dp = 36.dp) =
    IconButton(actions::previous) {
        Icon(painterResource(R.drawable.ic_previous), stringResource(R.string.player_previous), Modifier.size(iconSize))
    }

@Composable
fun NextButton(actions: PlayerActions, iconSize: Dp = 36.dp) =
    IconButton(actions::next) {
        Icon(painterResource(R.drawable.ic_next), stringResource(R.string.player_next), Modifier.size(iconSize))
    }

@Composable
fun PlayButton(
    ui: PlayerUi,
    actions: PlayerActions,
    size: Dp,
    container: Color,
    content: Color,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(container)
            .clickable(role = Role.Button, onClick = actions::togglePlay),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            PlayPauseIcon(ui.showPause, Modifier.size(size / 2))
        }
    }
}

@Composable
fun ShuffleButton(ui: PlayerUi, actions: PlayerActions, active: Color) {
    val state = stringResource(if (ui.shuffle) R.string.state_on else R.string.state_off)
    IconButton(actions::toggleShuffle, Modifier.semantics { stateDescription = state }) {
        Icon(
            painterResource(R.drawable.ic_shuffle),
            stringResource(R.string.player_shuffle),
            tint = if (ui.shuffle) active else LocalContentColor.current.copy(alpha = 0.55f),
        )
    }
}

@Composable
fun RepeatButton(ui: PlayerUi, actions: PlayerActions, active: Color) {
    val state = stringResource(
        when (ui.repeat) {
            RepeatUi.OFF -> R.string.state_off
            RepeatUi.ALL -> R.string.repeat_all
            RepeatUi.ONE -> R.string.repeat_one
        },
    )
    IconButton(actions::cycleRepeat, Modifier.semantics { stateDescription = state }) {
        Icon(
            painterResource(
                if (ui.repeat == RepeatUi.ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
            ),
            stringResource(R.string.player_repeat),
            tint = if (ui.repeat == RepeatUi.OFF) LocalContentColor.current.copy(alpha = 0.55f) else active,
        )
    }
}

@Composable
fun FavoriteButton(ui: PlayerUi, actions: PlayerActions, active: Color) =
    IconButton(actions::toggleFavorite) {
        Icon(
            if (ui.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            stringResource(if (ui.favorite) R.string.player_unfavorite else R.string.player_favorite),
            tint = if (ui.favorite) active else LocalContentColor.current,
        )
    }

@Composable
fun BackButton(actions: PlayerActions) = IconButton(actions::back) {
    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.player_back))
}

/** Seek bar with elapsed and remaining time. */
@Composable
fun SeekBar(
    ui: PlayerUi,
    position: Position,
    actions: PlayerActions,
    color: Color,
    track: Color,
    modifier: Modifier = Modifier,
) {
    var dragging by remember(ui.title, ui.queueIndex) { mutableStateOf<Float?>(null) }
    val duration = ui.durationMs.coerceAtLeast(1)
    val shown = dragging?.toLong() ?: position.value.coerceIn(0, duration)
    LeftToRight {
        Column(modifier) {
            Slider(
                value = shown.toFloat(),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { actions.seekTo(it.toLong()) }
                    dragging = null
                },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = track,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(shown), style = MaterialTheme.typography.labelMedium)
                Text(
                    "-" + formatDuration((ui.durationMs - shown).coerceAtLeast(0)),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun SpeedMenu(ui: PlayerUi, actions: PlayerActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(stringResource(R.string.player_speed, "${ui.speed}×"), color = LocalContentColor.current)
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach {
                DropdownMenuItem(text = { Text("${it}×") }, onClick = { open = false; actions.setSpeed(it) })
            }
        }
    }
}

@Composable
fun SleepMenu(ui: PlayerUi, actions: PlayerActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            val left = ui.sleepMinutesLeft
            Text(
                if (left == null) stringResource(R.string.sleep_timer)
                else stringResource(R.string.sleep_left, left),
                color = LocalContentColor.current,
            )
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.state_off)) },
                onClick = { open = false; actions.setSleepTimer(null) },
            )
            listOf(15, 30, 45, 60).forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sleep_minutes, minutes)) },
                    onClick = { open = false; actions.setSleepTimer(minutes) },
                )
            }
        }
    }
}

/** One tap switches between portrait and landscape; the menu chooses the mode. */
@Composable
fun OrientationControls(ui: PlayerUi, actions: PlayerActions, isLandscape: Boolean) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                actions.setOrientationMode(
                    if (isLandscape) OrientationMode.PORTRAIT else OrientationMode.LANDSCAPE,
                )
            },
        ) {
            Icon(painterResource(R.drawable.ic_rotate), stringResource(R.string.orientation_switch))
        }
        Box {
            val labels = mapOf(
                OrientationMode.AUTO to R.string.orientation_auto,
                OrientationMode.PORTRAIT to R.string.orientation_portrait,
                OrientationMode.LANDSCAPE to R.string.orientation_landscape,
            )
            TextButton(onClick = { open = true }) {
                Text(stringResource(labels.getValue(ui.orientationMode)), color = LocalContentColor.current)
            }
            DropdownMenu(open, onDismissRequest = { open = false }) {
                labels.forEach { (mode, label) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = { open = false; actions.setOrientationMode(mode) },
                    )
                }
            }
        }
    }
}
