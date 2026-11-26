package com.amusic.features.library

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.amusic.core.common.Song
import com.amusic.core.common.formatDuration
import com.amusic.core.database.PlaylistSummary
import com.amusic.core.design.artistLabel

@Composable
internal fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                name, { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onDone(name); onDismiss() }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
internal fun PickPlaylistDialog(
    playlists: List<PlaylistSummary>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onNew: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_to_playlist)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                playlists.forEach { playlist ->
                    Text(
                        playlist.name,
                        Modifier.fillMaxWidth()
                            .clickable { onPick(playlist.id); onDismiss() }
                            .padding(vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNew) { Text(stringResource(R.string.playlist_new)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
internal fun InfoDialog(song: Song, onDismiss: () -> Unit) {
    val size = Formatter.formatShortFileSize(LocalContext.current, song.sizeBytes)
    val rows = listOf(
        R.string.info_title to song.title,
        R.string.info_artist to artistLabel(song.artist),
        R.string.info_album to song.album,
        R.string.info_genre to song.genre,
        R.string.info_folder to song.folder,
        R.string.info_duration to formatDuration(song.durationMs),
        R.string.info_size to size,
        R.string.info_type to song.mimeType,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_info)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rows.filter { it.second.isNotEmpty() }.forEach { (label, value) ->
                    Column {
                        Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
                        Text(value)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
internal fun ConfirmDeleteDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_delete)) },
        text = { Text(name) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(stringResource(R.string.playlist_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
