package com.amusic.features.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.design.ScreenScaffold

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val count by viewModel.songCount.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.home_title)) {
        count?.let {
            Text(
                pluralStringResource(R.plurals.home_song_count, it, it),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Text(stringResource(R.string.home_note), style = MaterialTheme.typography.bodyLarge)
    }
}
