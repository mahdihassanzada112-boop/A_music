package com.amusic.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.audio.PlayerController
import com.amusic.core.audio.SleepTimer
import com.amusic.data.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val player: PlayerController,
    val sleepTimer: SleepTimer,
    private val library: LibraryRepository,
) : ViewModel() {
    val favoriteIds = library.favoriteIds.map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setFavorite(songId: Long, favorite: Boolean) {
        viewModelScope.launch { library.setFavorite(listOf(songId), favorite) }
    }
}
