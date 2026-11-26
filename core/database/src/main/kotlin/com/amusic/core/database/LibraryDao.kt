package com.amusic.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class LibraryDao {
    // Favorites
    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    abstract fun observeFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addFavorites(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    abstract suspend fun removeFavorite(songId: Long)

    // History
    @Query("SELECT songId FROM play_history ORDER BY lastPlayedAt DESC LIMIT 100")
    abstract fun observeRecentIds(): Flow<List<Long>>

    @Query("UPDATE play_history SET lastPlayedAt = :at, playCount = playCount + 1 WHERE songId = :songId")
    abstract suspend fun bumpHistory(songId: Long, at: Long): Int

    @Insert
    abstract suspend fun insertHistory(entry: PlayHistoryEntity)

    @Transaction
    open suspend fun recordPlay(songId: Long, at: Long) {
        if (bumpHistory(songId, at) == 0) insertHistory(PlayHistoryEntity(songId, at, 1))
    }

    // Playlists
    @Query(
        "SELECT p.id AS id, p.name AS name, COUNT(s.id) AS songCount FROM playlists p " +
            "LEFT JOIN playlist_songs ps ON ps.playlistId = p.id " +
            "LEFT JOIN songs s ON s.id = ps.songId " +
            "GROUP BY p.id ORDER BY p.createdAt DESC",
    )
    abstract fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query(
        "SELECT ps.entryId, ps.position, s.* FROM playlist_songs ps " +
            "INNER JOIN songs s ON s.id = ps.songId " +
            "WHERE ps.playlistId = :playlistId ORDER BY ps.position",
    )
    abstract fun observePlaylistEntries(playlistId: Long): Flow<List<PlaylistEntry>>

    @Insert
    abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    abstract suspend fun renamePlaylist(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun deletePlaylist(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    abstract suspend fun maxPosition(playlistId: Long): Int

    @Insert
    abstract suspend fun insertEntries(entries: List<PlaylistSongEntity>)

    @Transaction
    open suspend fun appendToPlaylist(playlistId: Long, songIds: List<Long>) {
        val start = maxPosition(playlistId) + 1
        insertEntries(songIds.mapIndexed { i, id -> PlaylistSongEntity(0, playlistId, id, start + i) })
    }

    @Query("DELETE FROM playlist_songs WHERE entryId = :entryId")
    abstract suspend fun removeEntry(entryId: Long)

    @Query("UPDATE playlist_songs SET position = :position WHERE entryId = :entryId")
    abstract suspend fun setPosition(entryId: Long, position: Int)

    @Transaction
    open suspend fun swapEntries(first: PlaylistEntry, second: PlaylistEntry) {
        setPosition(first.entryId, second.position)
        setPosition(second.entryId, first.position)
    }
}
