package com.amusic.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.database.SongDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(songs: SongDao) : ViewModel() {
    /** null while the database is still loading. */
    val songCount: StateFlow<Int?> =
        songs.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
