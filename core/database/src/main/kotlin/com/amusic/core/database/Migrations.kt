package com.amusic.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds lyrics; everything the user already has is kept. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `lyrics` (`songId` INTEGER NOT NULL, " +
                "`content` TEXT NOT NULL, `offsetMs` INTEGER NOT NULL, PRIMARY KEY(`songId`))",
        )
    }
}
