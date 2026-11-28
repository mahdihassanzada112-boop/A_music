package com.amusic.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.amusic.core.common.AutoBackground
import com.amusic.core.common.BackgroundSettings
import com.amusic.core.common.BuiltInPreset
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.ThemeId
import com.amusic.core.common.ThemeMode
import com.amusic.core.common.VisualizerColor
import com.amusic.core.common.VisualizerSettings
import com.amusic.core.common.VisualizerStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun defaultsToSystemThenPersistsChoice() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.root.resolve("test.preferences_pb")
        }
        val repository = SettingsRepository(store)
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun themeSettingsPersistAndReset() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.root.resolve("theme.preferences_pb")
        }
        val repository = SettingsRepository(store)
        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setThemeId(ThemeId.GLASS)
        repository.setOrientationMode(OrientationMode.LANDSCAPE)
        assertEquals(ThemeId.GLASS, repository.themeId.first())
        assertEquals(OrientationMode.LANDSCAPE, repository.orientationMode.first())

        repository.resetThemeSettings()
        assertEquals(ThemeId.DARK_MODERN, repository.themeId.first())
        assertEquals(OrientationMode.AUTO, repository.orientationMode.first())
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
    }

    @Test
    fun visualizerSettingsPersistWithinLimits() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.root.resolve("visualizer.preferences_pb")
        }
        val repository = SettingsRepository(store)
        assertEquals(VisualizerSettings(), repository.visualizer.first())
        repository.setVisualizer(
            VisualizerSettings(true, VisualizerStyle.RETRO, VisualizerColor.CYAN, 9f, 0.7f, 0f),
        )
        assertEquals(
            VisualizerSettings(true, VisualizerStyle.RETRO, VisualizerColor.CYAN, 1.5f, 0.7f, 0.5f),
            repository.visualizer.first(),
        )
    }

    @Test
    fun backgroundPersistsAndKeepsEveryQuickChange() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.root.resolve("background.preferences_pb")
        }
        val repository = SettingsRepository(store)
        assertEquals(BackgroundSettings(), repository.background.first())
        repository.updateBackground { it.copy(enabled = true, fpsLimit = 45) }
        repository.updateBackground { it.copy(scene = BuiltInPreset.CONCERT.scene) }
        repository.updateBackground { it.copy(auto = AutoBackground.SHUFFLE, batterySaver = true) }
        assertEquals(
            BackgroundSettings(true, BuiltInPreset.CONCERT.scene, AutoBackground.SHUFFLE, true, 45),
            repository.background.first(),
        )
        repository.updateBackground { it.copy(fpsLimit = 1_000) }
        assertEquals(BackgroundSettings.DEFAULT_FPS, repository.background.first().fpsLimit)
    }
}
