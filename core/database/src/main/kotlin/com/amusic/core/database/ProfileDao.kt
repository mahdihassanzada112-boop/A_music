package com.amusic.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A saved landscape arrangement, belonging to one theme. */
@Entity(tableName = "layout_profiles")
data class LayoutProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val theme: String,
    val name: String,
    val layout: String,
)

/** A saved equalizer curve. */
@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bands: String,
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM layout_profiles WHERE theme = :theme ORDER BY id")
    fun observeLayouts(theme: String): Flow<List<LayoutProfileEntity>>

    @Insert
    suspend fun insert(profile: LayoutProfileEntity): Long

    @Query("DELETE FROM layout_profiles WHERE id = :id")
    suspend fun deleteLayout(id: Long)

    @Query("SELECT * FROM eq_presets ORDER BY id")
    fun observeCurves(): Flow<List<EqPresetEntity>>

    @Insert
    suspend fun insert(preset: EqPresetEntity): Long

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun deleteCurve(id: Long)
}
