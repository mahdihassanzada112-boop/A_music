package com.amusic.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.audio.PlayerController
import com.amusic.core.common.Song
import com.amusic.core.common.SortOrder
import com.amusic.core.common.sortedBy
import com.amusic.data.LibraryRepository
import com.amusic.data.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class HomeState(
    val all: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val scan: ScanState = ScanState.IDLE,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val player: PlayerController,
) : ViewModel() {
    /** null while the database is still loading. */
    val state = combine(library.songs, library.recentlyPlayed, library.scanState) { all, recent, scan ->
        HomeState(all, recent.take(5), all.sortedBy(SortOrder.DATE_ADDED).take(10), scan)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun play(songs: List<Song>, index: Int) = player.play(songs, index)

    fun shuffleAll() = player.shufflePlay(state.value?.all.orEmpty())

    fun rescan() = library.requestScan()
}
