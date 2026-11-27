package com.amusic.themes.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amusic.core.common.Song
import com.amusic.core.design.Artwork
import com.amusic.core.design.SongRow
import com.amusic.core.design.albumArtUri
import com.amusic.core.theming.HomeActions
import com.amusic.core.theming.HomeStatus
import com.amusic.core.theming.HomeUi
import com.amusic.core.theming.R as ThemingR

/** A centred greeting pane, recent plays in panes, then a grid of new music. */
@Composable
internal fun GlassHome(ui: HomeUi, actions: HomeActions) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier.fillMaxWidth().glass().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(ThemingR.string.home_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    pluralStringResource(ThemingR.plurals.home_song_count, ui.songCount, ui.songCount),
                    textAlign = TextAlign.Center,
                )
                when {
                    ui.status == HomeStatus.SCANNING -> Text(stringResource(ThemingR.string.home_scanning))
                    ui.status == HomeStatus.FAILED -> Text(
                        stringResource(ThemingR.string.home_scan_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                    ui.songCount == 0 -> Text(stringResource(ThemingR.string.home_empty))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = actions::shuffleAll, enabled = ui.songCount > 0) {
                        Text(stringResource(ThemingR.string.home_shuffle_all))
                    }
                    OutlinedButton(onClick = actions::rescan, enabled = ui.status != HomeStatus.SCANNING) {
                        Text(stringResource(ThemingR.string.home_rescan))
                    }
                }
            }
        }
        if (ui.recentlyPlayed.isNotEmpty()) {
            heading(ThemingR.string.home_recently_played, "played_heading")
            ui.recentlyPlayed.forEachIndexed { index, song ->
                item(key = "played-${song.id}", span = { GridItemSpan(maxLineSpan) }) {
                    SongRow(song, { actions.play(ui.recentlyPlayed, index) }, Modifier.glass())
                }
            }
        }
        if (ui.recentlyAdded.isNotEmpty()) {
            heading(ThemingR.string.home_recently_added, "added_heading")
            ui.recentlyAdded.forEachIndexed { index, song ->
                item(key = "added-${song.id}") {
                    Tile(song) { actions.play(ui.recentlyAdded, index) }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.heading(text: Int, key: String) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) {
        Text(stringResource(text), style = MaterialTheme.typography.titleMedium)
    }

@Composable
private fun Tile(song: Song, onClick: () -> Unit) {
    Column(Modifier.glass().clickable(onClick = onClick).padding(10.dp)) {
        Artwork(albumArtUri(song.albumId), Modifier.fillMaxWidth().aspectRatio(1f))
        Text(
            song.title, Modifier.padding(top = 6.dp, start = 6.dp, end = 6.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
