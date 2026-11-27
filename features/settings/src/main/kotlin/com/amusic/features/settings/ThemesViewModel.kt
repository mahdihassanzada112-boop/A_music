package com.amusic.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.ThemeId
import com.amusic.core.storage.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemesViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val selected = settings.themeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val orientation = settings.orientationMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrientationMode.AUTO)

    fun select(id: ThemeId) {
        viewModelScope.launch { settings.setThemeId(id) }
    }

    fun setOrientation(mode: OrientationMode) {
        viewModelScope.launch { settings.setOrientationMode(mode) }
    }

    fun reset() {
        viewModelScope.launch { settings.resetThemeSettings() }
    }
}
