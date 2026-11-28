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

/** Adds saved background presets. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `background_presets` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `scene` TEXT NOT NULL)",
        )
    }
}
