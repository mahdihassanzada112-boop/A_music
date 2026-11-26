package com.amusic.features.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.design.ScreenScaffold

@Composable
fun LibraryScreen(viewModel: LibraryViewModel = hiltViewModel()) {
    val count by viewModel.songCount.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.library_title)) {
        count?.let {
            Text(
                pluralStringResource(R.plurals.library_song_count, it, it),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Text(stringResource(R.string.library_note), style = MaterialTheme.typography.bodyLarge)
    }
}
