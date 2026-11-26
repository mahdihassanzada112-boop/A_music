package com.amusic.core.common

data class Song(
    val id: Long,
    val contentUri: String,
    val title: String,
    /** Empty when the file has no tag; the UI shows a localized "unknown". */
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String,
    /** Relative or absolute folder path, without a trailing slash. */
    val folder: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedEpochSeconds: Long,
    val track: Int,
    val mimeType: String,
)
