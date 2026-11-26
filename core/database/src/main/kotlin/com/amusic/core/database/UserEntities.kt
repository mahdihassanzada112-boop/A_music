package com.amusic.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val songId: Long, val addedAt: Long)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** No foreign key to songs: a temporarily missing file must not drop it from playlists. */
@Entity(
    tableName = "playlist_songs",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId")],
)
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(@PrimaryKey val songId: Long, val lastPlayedAt: Long, val playCount: Int)

data class PlaylistSummary(val id: Long, val name: String, val songCount: Int)

data class PlaylistEntry(val entryId: Long, val position: Int, @Embedded val song: SongEntity)
