package com.amusic.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.SortOrder
import com.amusic.core.common.ThemeId
import com.amusic.core.common.ThemeMode
import com.amusic.core.common.VisualizerColor
import com.amusic.core.common.VisualizerSettings
import com.amusic.core.common.VisualizerStyle
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    val themeMode: Flow<ThemeMode> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { ThemeMode.fromName(it[THEME_MODE]) }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    val sortOrder: Flow<SortOrder> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { SortOrder.fromName(it[SORT_ORDER]) }
        .distinctUntilChanged()

    suspend fun setSortOrder(order: SortOrder) {
        store.edit { it[SORT_ORDER] = order.name }
    }

    val themeId: Flow<ThemeId> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { ThemeId.fromName(it[THEME_ID]) }
        .distinctUntilChanged()

    val orientationMode: Flow<OrientationMode> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { OrientationMode.fromName(it[ORIENTATION]) }
        .distinctUntilChanged()

    suspend fun setThemeId(id: ThemeId) {
        store.edit { it[THEME_ID] = id.name }
    }

    suspend fun setOrientationMode(mode: OrientationMode) {
        store.edit { it[ORIENTATION] = mode.name }
    }

    /** Back to the default theme and automatic orientation; other settings are kept. */
    suspend fun resetThemeSettings() {
        store.edit {
            it.remove(THEME_ID)
            it.remove(ORIENTATION)
        }
    }

    val visualizer: Flow<VisualizerSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            VisualizerSettings(
                enabled = it[VISUALIZER_ENABLED] ?: false,
                style = VisualizerStyle.fromName(it[VISUALIZER_STYLE]),
                color = VisualizerColor.fromName(it[VISUALIZER_COLOR]),
                intensity = it[VISUALIZER_INTENSITY] ?: 1f,
                speed = it[VISUALIZER_SPEED] ?: 1f,
                sensitivity = it[VISUALIZER_SENSITIVITY] ?: 1f,
            ).clamped()
        }
        .distinctUntilChanged()

    suspend fun setVisualizer(settings: VisualizerSettings) {
        val safe = settings.clamped()
        store.edit {
            it[VISUALIZER_ENABLED] = safe.enabled
            it[VISUALIZER_STYLE] = safe.style.name
            it[VISUALIZER_COLOR] = safe.color.name
            it[VISUALIZER_INTENSITY] = safe.intensity
            it[VISUALIZER_SPEED] = safe.speed
            it[VISUALIZER_SENSITIVITY] = safe.sensitivity
        }
    }

    private companion object {
        val VISUALIZER_ENABLED = booleanPreferencesKey("visualizer_enabled")
        val VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        val VISUALIZER_COLOR = stringPreferencesKey("visualizer_color")
        val VISUALIZER_INTENSITY = floatPreferencesKey("visualizer_intensity")
        val VISUALIZER_SPEED = floatPreferencesKey("visualizer_speed")
        val VISUALIZER_SENSITIVITY = floatPreferencesKey("visualizer_sensitivity")
        val THEME_ID = stringPreferencesKey("theme_id")
        val ORIENTATION = stringPreferencesKey("orientation_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
