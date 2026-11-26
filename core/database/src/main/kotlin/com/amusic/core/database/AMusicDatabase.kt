package com.amusic.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SongEntity::class], version = 1, exportSchema = true)
abstract class AMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
