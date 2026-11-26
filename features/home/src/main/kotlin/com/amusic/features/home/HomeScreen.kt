package com.amusic.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.common.Song
import com.amusic.core.design.SongRow
import com.amusic.data.ScanState

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val loaded by viewModel.state.collectAsStateWithLifecycle()
    val state = loaded ?: return
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
                val count = state.all.size
                Text(pluralStringResource(R.plurals.home_song_count, count, count))
                when {
                    state.scan == ScanState.SCANNING -> Text(stringResource(R.string.home_scanning))
                    state.scan == ScanState.FAILED -> Text(
                        stringResource(R.string.home_scan_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                    count == 0 -> Text(stringResource(R.string.home_empty))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = viewModel::shuffleAll, enabled = count > 0) {
                        Text(stringResource(R.string.home_shuffle_all))
                    }
                    OutlinedButton(onClick = viewModel::rescan, enabled = state.scan != ScanState.SCANNING) {
                        Text(stringResource(R.string.home_rescan))
                    }
                }
            }
        }
        section(R.string.home_recently_played, "played", state.recentlyPlayed, viewModel::play)
        section(R.string.home_recently_added, "added", state.recentlyAdded, viewModel::play)
    }
}

private fun LazyListScope.section(
    title: Int,
    key: String,
    songs: List<Song>,
    onPlay: (List<Song>, Int) -> Unit,
) {
    if (songs.isEmpty()) return
    item(key = key) {
        Text(
            stringResource(title),
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    itemsIndexed(songs, key = { _, song -> "$key-${song.id}" }) { index, song ->
        SongRow(song, onClick = { onPlay(songs, index) })
    }
}
