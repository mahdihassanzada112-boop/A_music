package com.amusic.data

import com.amusic.core.common.BackgroundCodec
import com.amusic.core.common.BackgroundScene
import com.amusic.core.database.BackgroundPresetEntity
import com.amusic.core.database.PresetDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BackgroundPreset(val id: Long, val name: String, val scene: BackgroundScene)

@Singleton
class PresetRepository @Inject constructor(private val dao: PresetDao) {
    /** Oldest first; a row that cannot be read is left out. */
    val presets: Flow<List<BackgroundPreset>> = dao.observe().map { rows ->
        rows.mapNotNull { row ->
            BackgroundCodec.decode(row.scene)?.let { BackgroundPreset(row.id, row.name, it) }
        }
    }

    /** A blank name saves nothing. */
    suspend fun save(name: String, scene: BackgroundScene) {
        val clean = name.trim().take(MAX_NAME)
        if (clean.isNotEmpty()) {
            dao.insert(BackgroundPresetEntity(name = clean, scene = BackgroundCodec.encode(scene)))
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        const val MAX_NAME = 40
    }
}
