package com.amusic.themes.darkmodern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amusic.core.design.Artwork
import com.amusic.core.design.SongRow
import com.amusic.core.design.albumArtUri
import com.amusic.core.theming.HomeActions
import com.amusic.core.theming.HomeStatus
import com.amusic.core.theming.HomeUi
import com.amusic.core.theming.R as ThemingR

/** Hero block, a horizontal shelf of new music, then a dense list. */
@Composable
internal fun DarkHome(ui: HomeUi, actions: HomeActions) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(
                Modifier.padding(16.dp).fillMaxWidth().clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(ThemingR.string.home_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    pluralStringResource(ThemingR.plurals.home_song_count, ui.songCount, ui.songCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    ui.status == HomeStatus.SCANNING -> Text(stringResource(ThemingR.string.home_scanning))
                    ui.status == HomeStatus.FAILED -> Text(
                        stringResource(ThemingR.string.home_scan_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                    ui.songCount == 0 -> Text(stringResource(ThemingR.string.home_empty))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = actions::shuffleAll, enabled = ui.songCount > 0) {
                        Text(stringResource(ThemingR.string.home_shuffle_all))
                    }
                    TextButton(onClick = actions::rescan, enabled = ui.status != HomeStatus.SCANNING) {
                        Text(stringResource(ThemingR.string.home_rescan))
                    }
                }
            }
        }
        if (ui.recentlyAdded.isNotEmpty()) {
            item { Heading(ThemingR.string.home_recently_added) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(ui.recentlyAdded, key = { _, s -> s.id }) { index, song ->
                        Column(Modifier.width(128.dp).clickable { actions.play(ui.recentlyAdded, index) }) {
                            Artwork(albumArtUri(song.albumId), Modifier.size(128.dp))
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (ui.recentlyPlayed.isNotEmpty()) {
            item { Heading(ThemingR.string.home_recently_played) }
            items(ui.recentlyPlayed.size, key = { ui.recentlyPlayed[it].id }) { index ->
                SongRow(ui.recentlyPlayed[index], onClick = { actions.play(ui.recentlyPlayed, index) })
            }
        }
    }
}

@Composable
private fun Heading(text: Int) = Text(
    stringResource(text),
    Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
    style = MaterialTheme.typography.titleMedium,
)
