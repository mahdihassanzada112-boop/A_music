package com.amusic.core.theming

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val STEP_MS = 500L

/** Lyrics of the current song with their tools, or the ways to add them. */
@Composable
fun LyricsPanel(ui: PlayerUi, position: Position, actions: PlayerActions, modifier: Modifier = Modifier) {
    val lyrics = ui.lyrics
    Column(modifier.testTag("lyrics_panel")) {
        if (lyrics == null) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.lyrics_none), textAlign = TextAlign.Center)
                Button(onClick = actions::importLyrics) { Text(stringResource(R.string.lyrics_import)) }
                TextButton(onClick = actions::editLyrics) {
                    Text(stringResource(R.string.lyrics_write), color = LocalContentColor.current)
                }
            }
        } else {
            LyricsText(lyrics, position, actions, Modifier.weight(1f).fillMaxWidth())
            LyricsTools(lyrics, actions)
        }
    }
}

@Composable
private fun LyricsText(lyrics: LyricsUi, position: Position, actions: PlayerActions, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val margin = maxHeight / 3
        val listState = remember(lyrics.parsed) { androidx.compose.foundation.lazy.LazyListState() }
        val active by remember(lyrics, position) {
            derivedStateOf { lyrics.parsed.activeIndex(position.value, lyrics.offsetMs) }
        }
        // After the listener scrolls by hand, following resumes a moment later.
        val dragged by listState.interactionSource.collectIsDraggedAsState()
        var held by remember(listState) { mutableStateOf(false) }
        LaunchedEffect(dragged) {
            if (dragged) {
                held = true
            } else if (held) {
                delay(3_000)
                held = false
            }
        }
        LaunchedEffect(listState, active, held, margin) {
            if (!held && lyrics.parsed.synced) listState.animateScrollToItem(active.coerceAtLeast(0))
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = if (lyrics.parsed.synced) margin else 8.dp,
                bottom = if (lyrics.parsed.synced) margin * 2 else 8.dp,
            ),
        ) {
            itemsIndexed(lyrics.parsed.lines) { index, line ->
                val current = index == active
                val time = line.timeMs
                Text(
                    if (time != null && line.text.isEmpty()) "♪" else line.text,
                    Modifier.fillMaxWidth()
                        .then(
                            if (time == null) Modifier
                            else Modifier.clickable { actions.seekTo((time + lyrics.offsetMs).coerceAtLeast(0)) },
                        )
                        .semantics { selected = current }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    // Each line takes its own direction, so mixed-language songs read correctly.
                    style = (if (current) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
                        .copy(textDirection = TextDirection.Content),
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        current -> MaterialTheme.colorScheme.primary
                        lyrics.parsed.synced -> LocalContentColor.current.copy(alpha = 0.6f)
                        else -> LocalContentColor.current
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LyricsTools(lyrics: LyricsUi, actions: PlayerActions) {
    var open by remember { mutableStateOf(false) }
    val color = LocalContentColor.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (lyrics.parsed.synced) {
            TextButton(onClick = { actions.setLyricsOffset(lyrics.offsetMs - STEP_MS) }) {
                Text(stringResource(R.string.lyrics_earlier), color = color)
            }
            Text(
                stringResource(R.string.lyrics_offset, lyrics.offsetMs / 1_000f),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { actions.setLyricsOffset(lyrics.offsetMs + STEP_MS) }) {
                Text(stringResource(R.string.lyrics_later), color = color)
            }
        } else {
            Text(
                stringResource(R.string.lyrics_unsynced),
                Modifier.weight(1f).padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Box {
            IconButton(onClick = { open = true }) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.lyrics_options))
            }
            DropdownMenu(open, onDismissRequest = { open = false }) {
                @Composable
                fun item(label: Int, action: () -> Unit) = DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = { open = false; action() },
                )
                item(R.string.lyrics_edit, actions::editLyrics)
                item(R.string.lyrics_import, actions::importLyrics)
                if (lyrics.offsetMs != 0L) item(R.string.lyrics_reset_timing) { actions.setLyricsOffset(0) }
                item(R.string.lyrics_remove, actions::removeLyrics)
            }
        }
    }
}
