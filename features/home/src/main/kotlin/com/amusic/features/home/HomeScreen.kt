package com.amusic.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.common.Song
import com.amusic.core.theming.HomeActions
import com.amusic.core.theming.HomeStatus
import com.amusic.core.theming.HomeUi
import com.amusic.core.theming.LocalPlayerTheme
import com.amusic.data.ScanState

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val loaded by viewModel.state.collectAsStateWithLifecycle()
    val state = loaded ?: return
    val actions = remember(viewModel) {
        object : HomeActions {
            override fun shuffleAll() = viewModel.shuffleAll()
            override fun rescan() = viewModel.rescan()
            override fun play(songs: List<Song>, index: Int) = viewModel.play(songs, index)
        }
    }
    val status = when (state.scan) {
        ScanState.IDLE -> HomeStatus.IDLE
        ScanState.SCANNING -> HomeStatus.SCANNING
        ScanState.FAILED -> HomeStatus.FAILED
    }
    LocalPlayerTheme.current.Home(
        HomeUi(state.all.size, status, state.recentlyPlayed, state.recentlyAdded),
        actions,
    )
}
