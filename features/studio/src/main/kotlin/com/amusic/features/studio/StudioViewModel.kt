package com.amusic.features.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.audio.AudioAnalyzer
import com.amusic.core.audio.PlayerController
import com.amusic.core.common.AudioFeed
import com.amusic.core.common.BackgroundScene
import com.amusic.core.common.BackgroundSettings
import com.amusic.core.storage.SettingsRepository
import com.amusic.data.PresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

typealias Change = (BackgroundSettings) -> BackgroundSettings

@HiltViewModel
class StudioViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val presetRepository: PresetRepository,
    player: PlayerController,
    analyzer: AudioAnalyzer,
) : ViewModel() {
    val audioFeed: AudioFeed = analyzer

    /** A change being dragged: shown in the preview, stored only when the finger lifts. */
    private val trying = MutableStateFlow<Change?>(null)

    /** null until read. */
    val background = combine(settings.background, trying) { stored, change ->
        change?.invoke(stored) ?: stored
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val presets = presetRepository.presets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The current cover and title, for the preview. */
    val song = player.state
        .map { Triple(it.current?.title, it.artworkUri, it.isPlaying) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Triple(null, null, false))

    fun preview(change: Change) {
        trying.value = change
    }

    fun update(change: Change) {
        viewModelScope.launch {
            settings.updateBackground(change)
            trying.value = null
        }
    }

    /** Editing the picture also switches the studio on, so the change is visible. */
    fun editScene(change: (BackgroundScene) -> BackgroundScene) =
        update { it.copy(enabled = true, scene = change(it.scene)) }

    fun previewScene(change: (BackgroundScene) -> BackgroundScene) =
        preview { it.copy(enabled = true, scene = change(it.scene)) }

    fun restoreDefaults() = update { BackgroundSettings(enabled = it.enabled) }

    fun savePreset(name: String) {
        val scene = background.value?.scene ?: return
        viewModelScope.launch { presetRepository.save(name, scene) }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch { presetRepository.delete(id) }
    }
}
