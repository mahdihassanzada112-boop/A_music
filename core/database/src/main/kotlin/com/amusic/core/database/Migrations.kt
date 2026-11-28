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

/** Adds landscape layout profiles and equalizer presets. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `layout_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`theme` TEXT NOT NULL, `name` TEXT NOT NULL, `layout` TEXT NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `eq_presets` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `bands` TEXT NOT NULL)",
        )
    }
}
