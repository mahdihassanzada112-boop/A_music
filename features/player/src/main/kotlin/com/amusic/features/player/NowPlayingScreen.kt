package com.amusic.features.player

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.amusic.core.audio.PlayerState
import com.amusic.core.common.formatDuration
import com.amusic.core.design.Artwork
import com.amusic.core.design.artistLabel
import kotlinx.coroutines.delay

/** Functional player of the app shell. The themed portrait and landscape players are Phase 3. */
@Composable
fun NowPlayingScreen(onBack: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.player.state.collectAsStateWithLifecycle()
    var showQueue by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showQueue) { showQueue = false }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (showQueue) showQueue = false else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.player_back))
            }
            Text(
                stringResource(if (showQueue) R.string.player_queue else R.string.player_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            if (state.queue.isNotEmpty()) {
                TextButton(onClick = { showQueue = !showQueue }) {
                    Text(stringResource(if (showQueue) R.string.player_title else R.string.player_queue))
                }
            }
        }
        val current = state.current
        when {
            current == null -> Text(stringResource(R.string.player_nothing), Modifier.padding(24.dp))
            showQueue -> QueueList(state, viewModel.player)
            else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                if (maxWidth > maxHeight) {
                    val side = min(maxHeight - 32.dp, maxWidth * 0.4f)
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Artwork(state.artworkUri, Modifier.size(side))
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) { Details(state, viewModel) }
                    }
                } else {
                    val side = min(maxWidth - 48.dp, maxHeight * 0.45f)
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.padding(vertical = 12.dp)) {
                            Artwork(state.artworkUri, Modifier.size(side).aspectRatio(1f))
                        }
                        Details(state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun Details(state: PlayerState, viewModel: PlayerViewModel) {
    val current = state.current ?: return
    val player = viewModel.player
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val favorite = current.songId in favorites

    Text(
        current.title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(artistLabel(current.artist), textAlign = TextAlign.Center)
    if (state.album.isNotEmpty()) {
        Text(
            state.album,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    if (state.hasError) {
        Text(stringResource(R.string.player_error), color = MaterialTheme.colorScheme.error)
    }
    IconButton(onClick = { viewModel.setFavorite(current.songId, !favorite) }) {
        Icon(
            if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            stringResource(if (favorite) R.string.player_unfavorite else R.string.player_favorite),
            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
    SeekBar(state, viewModel)
    TransportRow(
        state,
        onShuffle = { player.toggleShuffle() },
        onPrevious = { player.previous() },
        onToggle = { player.togglePlay() },
        onNext = { player.next() },
        onRepeat = { player.cycleRepeat() },
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SpeedMenu(state.speed) { player.setSpeed(it) }
        SleepMenu(viewModel)
    }
}

@Composable
private fun SeekBar(state: PlayerState, viewModel: PlayerViewModel) {
    var position by remember { mutableLongStateOf(viewModel.player.positionMs()) }
    var dragging by remember(state.current?.songId) { mutableStateOf<Float?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Polls only while the screen is visible.
    LaunchedEffect(state.index, state.current?.songId, state.isPlaying, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                position = viewModel.player.positionMs()
                delay(500)
            }
        }
    }
    val duration = state.durationMs.coerceAtLeast(1)
    val shown = dragging?.toLong() ?: position.coerceIn(0, duration)
    LeftToRight {
        Column {
            Slider(
                value = shown.toFloat(),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let {
                        position = it.toLong()
                        viewModel.player.seekTo(it.toLong())
                    }
                    dragging = null
                },
                valueRange = 0f..duration.toFloat(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(shown), style = MaterialTheme.typography.labelMedium)
                Text(
                    "-" + formatDuration((state.durationMs - shown).coerceAtLeast(0)),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun SpeedMenu(speed: Float, onSelect: (Float) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(stringResource(R.string.player_speed, "${speed}×")) }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach {
                DropdownMenuItem(text = { Text("${it}×") }, onClick = { open = false; onSelect(it) })
            }
        }
    }
}

@Composable
private fun SleepMenu(viewModel: PlayerViewModel) {
    var open by remember { mutableStateOf(false) }
    val endsAt by viewModel.sleepTimer.endsAt.collectAsStateWithLifecycle()
    var minutesLeft by remember { mutableLongStateOf(0) }
    LaunchedEffect(endsAt) {
        while (true) {
            val end = endsAt ?: break
            minutesLeft = (end - SystemClock.elapsedRealtime() + 59_999).coerceAtLeast(0) / 60_000
            delay(1_000)
        }
    }
    Box {
        TextButton(onClick = { open = true }) {
            Text(
                if (endsAt == null) stringResource(R.string.sleep_timer)
                else stringResource(R.string.sleep_left, minutesLeft.toInt()),
            )
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.state_off)) },
                onClick = { open = false; viewModel.sleepTimer.cancel() },
            )
            listOf(15, 30, 45, 60).forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sleep_minutes, minutes)) },
                    onClick = { open = false; viewModel.sleepTimer.start(minutes) },
                )
            }
        }
    }
}
