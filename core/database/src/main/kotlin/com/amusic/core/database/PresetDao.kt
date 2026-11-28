package com.amusic.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A background the user saved; [scene] is the encoded combination of layers and settings. */
@Entity(tableName = "background_presets")
data class BackgroundPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val scene: String,
)

@Dao
interface PresetDao {
    @Query("SELECT * FROM background_presets ORDER BY id")
    fun observe(): Flow<List<BackgroundPresetEntity>>

    @Insert
    suspend fun insert(preset: BackgroundPresetEntity): Long

    @Query("DELETE FROM background_presets WHERE id = :id")
    suspend fun delete(id: Long)
}
