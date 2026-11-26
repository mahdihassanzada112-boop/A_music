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
        Room.databaseBuilder(context, AMusicDatabase::class.java, "amusic.db").build()

    @Provides
    fun songDao(db: AMusicDatabase): SongDao = db.songDao()
}
