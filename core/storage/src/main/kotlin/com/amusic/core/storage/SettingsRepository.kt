package com.amusic.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.amusic.core.common.SortOrder
import com.amusic.core.common.ThemeMode
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

    private companion object {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
