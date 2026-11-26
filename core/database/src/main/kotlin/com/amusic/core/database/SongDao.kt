package com.amusic.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SongDao {
    @Query("SELECT * FROM songs")
    abstract fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT id FROM songs")
    abstract suspend fun allIds(): List<Long>

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    abstract suspend fun byIdsChunk(ids: List<Long>): List<SongEntity>

    @Upsert
    abstract suspend fun upsert(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    abstract suspend fun deleteChunk(ids: List<Long>)

    /** SQLite limits bound parameters, so ids are queried in chunks. Order is not defined. */
    open suspend fun byIds(ids: List<Long>): List<SongEntity> =
        ids.distinct().chunked(900).flatMap { byIdsChunk(it) }

    /** Makes the table match [scanned] exactly. */
    @Transaction
    open suspend fun replaceAll(scanned: List<SongEntity>) {
        val keep = scanned.mapTo(HashSet()) { it.id }
        allIds().filterNot { it in keep }.chunked(900).forEach { deleteChunk(it) }
        upsert(scanned)
    }
}
