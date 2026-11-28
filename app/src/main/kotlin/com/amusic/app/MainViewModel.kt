package com.amusic.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.audio.PlayerController
import com.amusic.core.common.AutoBackground
import com.amusic.core.common.nextScene
import com.amusic.core.common.rotationPool
import com.amusic.core.storage.SettingsRepository
import com.amusic.data.PresetRepository
import com.amusic.data.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val presets: PresetRepository,
    private val library: LibraryRepository,
    private val player: PlayerController,
) : ViewModel() {
    /** null until the stored preference has been read, to avoid a theme flash. */
    val themeMode = settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeId = settings.themeId.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            player.state.map { it.current?.songId }.filterNotNull().distinctUntilChanged()
                .drop(1) // the song already there when the app opens
                .collect { changeBackground() }
        }
    }

    /** Moves to another background for the new song, if the user asked for that. */
    private suspend fun changeBackground() {
        val saved = presets.presets.first().map { it.scene }
        settings.updateBackground {
            if (it.enabled && it.auto != AutoBackground.OFF) {
                it.copy(scene = nextScene(rotationPool(saved), it.scene, it.auto))
            } else {
                it
            }
        }
    }

    fun onLibraryAccess() {
        library.start()
        player.connect()
    }
}
