package com.amusic.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.audio.PlayerController
import com.amusic.core.storage.SettingsRepository
import com.amusic.data.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    private val library: LibraryRepository,
    private val player: PlayerController,
) : ViewModel() {
    /** null until the stored preference has been read, to avoid a theme flash. */
    val themeMode = settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onLibraryAccess() {
        library.start()
        player.connect()
    }
}
