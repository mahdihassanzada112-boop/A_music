package com.amusic.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT COUNT(*) FROM songs")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun upsert(songs: List<SongEntity>)
}
