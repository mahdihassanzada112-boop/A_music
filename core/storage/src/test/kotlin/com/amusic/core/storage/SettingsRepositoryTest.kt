package com.amusic.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.ThemeId
import com.amusic.core.common.ThemeMode
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
}
