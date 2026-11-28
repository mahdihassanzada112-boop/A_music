package com.amusic.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AMusicDatabase =
        Room.databaseBuilder(context, AMusicDatabase::class.java, "amusic.db")
            // Version 1 shipped before scanning existed, so it never held any data.
            .fallbackToDestructiveMigrationFrom(true, 1)
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun songDao(db: AMusicDatabase): SongDao = db.songDao()

    @Provides
    fun libraryDao(db: AMusicDatabase): LibraryDao = db.libraryDao()

    @Provides
    fun lyricsDao(db: AMusicDatabase): LyricsDao = db.lyricsDao()

    @Provides
    fun presetDao(db: AMusicDatabase): PresetDao = db.presetDao()
}
