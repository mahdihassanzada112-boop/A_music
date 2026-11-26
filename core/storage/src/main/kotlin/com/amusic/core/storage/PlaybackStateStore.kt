package com.amusic.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class SavedPlayback(
    val songIds: List<Long> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0,
    val repeatMode: Int = 0,
    val shuffle: Boolean = false,
    val speed: Float = 1f,
)

@Singleton
class PlaybackStateStore @Inject constructor(private val store: DataStore<Preferences>) {
    suspend fun load(): SavedPlayback {
        val p = store.data.first()
        return SavedPlayback(
            songIds = p[QUEUE].orEmpty().split(',').mapNotNull { it.toLongOrNull() },
            index = p[INDEX] ?: 0,
            positionMs = p[POSITION] ?: 0,
            repeatMode = p[REPEAT] ?: 0,
            shuffle = p[SHUFFLE] == 1,
            speed = p[SPEED] ?: 1f,
        )
    }

    suspend fun save(state: SavedPlayback) {
        store.edit {
            it[QUEUE] = state.songIds.joinToString(",")
            it[INDEX] = state.index
            it[POSITION] = state.positionMs
            it[REPEAT] = state.repeatMode
            it[SHUFFLE] = if (state.shuffle) 1 else 0
            it[SPEED] = state.speed
        }
    }

    private companion object {
        val QUEUE = stringPreferencesKey("playback_queue")
        val INDEX = intPreferencesKey("playback_index")
        val POSITION = longPreferencesKey("playback_position")
        val REPEAT = intPreferencesKey("playback_repeat")
        val SHUFFLE = intPreferencesKey("playback_shuffle")
        val SPEED = floatPreferencesKey("playback_speed")
    }
}
