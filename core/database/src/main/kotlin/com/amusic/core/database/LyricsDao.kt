package com.amusic.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE songId = :songId")
    fun observe(songId: Long): Flow<LyricsEntity?>

    @Upsert
    suspend fun upsert(lyrics: LyricsEntity)

    @Query("UPDATE lyrics SET offsetMs = :offsetMs WHERE songId = :songId")
    suspend fun setOffset(songId: Long, offsetMs: Long)

    @Query("DELETE FROM lyrics WHERE songId = :songId")
    suspend fun delete(songId: Long)
}
