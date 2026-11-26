package com.amusic.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [id] is the MediaStore audio id, so favorites and playlists survive rescans. */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String,
    val folder: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedEpochSeconds: Long,
    val track: Int,
    val mimeType: String,
)
