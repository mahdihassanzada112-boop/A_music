package com.amusic.features.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.audio.EqualizerControl
import com.amusic.core.common.EqPreset
import com.amusic.core.common.EqualizerSettings
import com.amusic.core.common.LandscapeLayout
import com.amusic.core.common.ThemeId
import com.amusic.core.storage.SettingsRepository
import com.amusic.data.EqCurve
import com.amusic.data.LayoutProfile
import com.amusic.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [layout] is null while the theme uses its own landscape design. */
data class EditorState(val theme: ThemeId, val layout: LandscapeLayout?, val profiles: List<LayoutProfile>)

/** Edits the current theme's landscape arrangement. Every change is stored at once. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LayoutEditorViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    /** null until read. */
    val state = settings.themeId
        .flatMapLatest { theme ->
            combine(settings.landscapeLayout(theme), profiles.layouts(theme)) { layout, saved ->
                EditorState(theme, layout, saved)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Changes the custom layout; does nothing while the theme's own design is in use. */
    fun change(change: (LandscapeLayout) -> LandscapeLayout) =
        store { current -> current?.let(change) }

    /** Starts a custom layout with this many columns, or adjusts the existing one. */
    fun setColumns(count: Int) = store { current ->
        current?.withColumnCount(count)
            ?: if (count == 2) LandscapeLayout.TwoColumns else LandscapeLayout.ThreeColumns
    }

    fun restoreThemeDesign() = store { null }

    fun apply(layout: LandscapeLayout) = store { layout }

    fun saveProfile(name: String) {
        val current = state.value ?: return
        val layout = current.layout ?: return
        viewModelScope.launch { profiles.saveLayout(current.theme, name, layout) }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch { profiles.deleteLayout(id) }
    }

    private fun store(change: (LandscapeLayout?) -> LandscapeLayout?) {
        val theme = state.value?.theme ?: return
        viewModelScope.launch { settings.updateLandscapeLayout(theme, change) }
    }
}

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val profiles: ProfileRepository,
    control: EqualizerControl,
) : ViewModel() {
    /** null until read. */
    val equalizer = settings.equalizer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val curves = profiles.curves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<EqCurve>())

    /** Known once the playback service runs. */
    val headphones = control.headphones

    fun update(change: (EqualizerSettings) -> EqualizerSettings) {
        viewModelScope.launch { settings.updateEqualizer(change) }
    }

    fun use(curve: EqCurve) =
        update { it.copy(enabled = true, preset = EqPreset.CUSTOM, bands = curve.bands) }

    fun reset() = update {
        EqualizerSettings(enabled = it.enabled, headphonesOnly = it.headphonesOnly)
    }

    fun saveCurve(name: String) {
        val bands = equalizer.value?.bands ?: return
        viewModelScope.launch { profiles.saveCurve(name, bands) }
    }

    fun deleteCurve(id: Long) {
        viewModelScope.launch { profiles.deleteCurve(id) }
    }
}
