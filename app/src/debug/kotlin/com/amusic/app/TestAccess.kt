package com.amusic.app

import com.amusic.core.audio.AudioAnalyzer
import com.amusic.core.audio.PlayerController
import com.amusic.core.storage.PlaybackStateStore
import com.amusic.core.storage.SettingsRepository
import com.amusic.data.LibraryRepository
import com.amusic.data.LyricsRepository
import com.amusic.data.PresetRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug builds only: lets instrumented tests reach the app's singletons. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TestAccess {
    fun library(): LibraryRepository
    fun player(): PlayerController
    fun playbackStore(): PlaybackStateStore
    fun settings(): SettingsRepository
    fun lyrics(): LyricsRepository
    fun analyzer(): AudioAnalyzer
    fun presets(): PresetRepository
}
