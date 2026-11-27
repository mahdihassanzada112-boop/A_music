package com.amusic.features.player

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.audio.PlayerController
import com.amusic.core.audio.RepeatSetting
import com.amusic.core.audio.SleepTimer
import com.amusic.core.common.OrientationMode
import com.amusic.core.storage.SettingsRepository
import com.amusic.core.theming.PlayerUi
import com.amusic.core.theming.QueueEntry
import com.amusic.core.theming.RepeatUi
import com.amusic.data.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val player: PlayerController,
    private val sleepTimer: SleepTimer,
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val sleepMinutes = sleepTimer.endsAt.flatMapLatest { end ->
        if (end == null) {
            flowOf(null)
        } else {
            flow {
                while (true) {
                    val left = end - SystemClock.elapsedRealtime()
                    emit(((left.coerceAtLeast(0) + 59_999) / 60_000).toInt())
                    delay(1_000)
                }
            }
        }
    }.distinctUntilChanged()

    /** null while nothing is queued. */
    val ui = combine(
        player.state, library.favoriteIds, sleepMinutes, settings.orientationMode,
    ) { state, favorites, sleep, orientation ->
        val current = state.current ?: return@combine null
        PlayerUi(
            title = current.title,
            artist = current.artist,
            album = state.album,
            artworkUri = state.artworkUri,
            durationMs = state.durationMs,
            isPlaying = state.isPlaying,
            showPause = state.showPause,
            shuffle = state.shuffle,
            repeat = when (state.repeat) {
                RepeatSetting.OFF -> RepeatUi.OFF
                RepeatSetting.ALL -> RepeatUi.ALL
                RepeatSetting.ONE -> RepeatUi.ONE
            },
            speed = state.speed,
            favorite = current.songId in favorites,
            hasError = state.hasError,
            queue = state.queue.map { QueueEntry(it.title, it.artist) },
            queueIndex = state.index,
            sleepMinutesLeft = sleep,
            orientationMode = orientation,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun positionMs() = player.positionMs()

    fun togglePlay() = player.togglePlay()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()
    fun setSpeed(speed: Float) = player.setSpeed(speed)
    fun jumpTo(index: Int) = player.jumpTo(index)
    fun removeAt(index: Int) = player.removeAt(index)
    fun move(from: Int, to: Int) = player.move(from, to)
    fun clearQueue() = player.clearQueue()

    fun setSleepTimer(minutes: Int?) {
        if (minutes == null) sleepTimer.cancel() else sleepTimer.start(minutes)
    }

    fun toggleFavorite() {
        val id = player.state.value.current?.songId ?: return
        val favorite = ui.value?.favorite ?: return
        viewModelScope.launch { library.setFavorite(listOf(id), !favorite) }
    }

    fun setOrientationMode(mode: OrientationMode) {
        viewModelScope.launch { settings.setOrientationMode(mode) }
    }
}
