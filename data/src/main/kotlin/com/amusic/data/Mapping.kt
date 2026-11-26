package com.amusic.data

import com.amusic.core.common.Song
import com.amusic.core.database.SongEntity

fun SongEntity.toSong() = Song(
    id, contentUri, title, artist, album, albumId, genre, folder,
    durationMs, sizeBytes, dateAddedEpochSeconds, track, mimeType,
)
