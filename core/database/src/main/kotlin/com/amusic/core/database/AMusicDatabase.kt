package com.amusic.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class, FavoriteEntity::class, PlaylistEntity::class,
        PlaylistSongEntity::class, PlayHistoryEntity::class,
        LyricsEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun libraryDao(): LibraryDao
    abstract fun lyricsDao(): LyricsDao
}
