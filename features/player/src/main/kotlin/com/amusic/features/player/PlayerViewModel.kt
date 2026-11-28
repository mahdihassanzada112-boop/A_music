package com.amusic.features.player

import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.audio.AudioAnalyzer
import com.amusic.core.audio.PlayerController
import com.amusic.core.audio.RepeatSetting
import com.amusic.core.audio.SleepTimer
import com.amusic.core.common.AudioFeed
import com.amusic.core.common.EqualizerSettings
import com.amusic.core.common.LandscapeLayout
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.ThemeId
import com.amusic.core.common.VisualizerSettings
import com.amusic.core.common.parseLyrics
import com.amusic.core.storage.SettingsRepository
import com.amusic.core.theming.LyricsUi
import com.amusic.core.theming.PlayerPanel
import com.amusic.core.theming.PlayerUi
import com.amusic.core.theming.QueueEntry
import com.amusic.core.theming.RepeatUi
import com.amusic.data.ImportResult
import com.amusic.data.LibraryRepository
import com.amusic.data.LyricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Text being edited, kept here so it survives rotation. */
data class LyricsDraft(val songId: Long, val text: String)

/** [layout] is null while [theme] uses its own landscape design. */
data class Arrangement(val theme: ThemeId, val layout: LandscapeLayout?)

private class SongLyrics(val songId: Long, val text: String, val ui: LyricsUi)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val player: PlayerController,
    private val sleepTimer: SleepTimer,
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
    private val lyricsRepository: LyricsRepository,
    analyzer: AudioAnalyzer,
) : ViewModel() {
    val audioFeed: AudioFeed = analyzer

    /** null until read, so the player does not flash the wrong background. */
    val background = settings.background
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The current theme's own landscape arrangement; null until read. */
    val arrangement = settings.themeId
        .flatMapLatest { theme -> settings.landscapeLayout(theme).map { Arrangement(theme, it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var draft by mutableStateOf<LyricsDraft?>(null)
        private set

    /** The song whose lyrics are about to be removed, once confirmed. */
    var removing by mutableStateOf<Long?>(null)
        private set
    var showVisualizerOptions by mutableStateOf(false)
    var importResult by mutableStateOf<ImportResult?>(null)
        private set

    private val panel = MutableStateFlow(PlayerPanel.NONE)

    private val storedLyrics = player.state
        .map { it.current?.songId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) {
                flowOf<SongLyrics?>(null)
            } else {
                lyricsRepository.observe(id).distinctUntilChanged().map { stored ->
                    stored?.let {
                        SongLyrics(id, it.content, LyricsUi(parseLyrics(it.content), it.offsetMs))
                    }
                }.flowOn(Dispatchers.Default)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val sleepMinutes = sleepTimer.endsAt.flatMapLatest { end ->
        if (end == null) {
            flowOf<Int?>(null)
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

    private val playback = combine(
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
            panel = PlayerPanel.NONE,
            lyrics = null,
            visualizer = VisualizerSettings(),
            equalizer = EqualizerSettings(),
        ) to current.songId
    }

    /** null while nothing is queued. */
    val ui = combine(
        playback, storedLyrics, panel, settings.visualizer, settings.equalizer,
    ) { base, lyrics, shown, visualizer, equalizer ->
        base?.let { (ui, songId) ->
            ui.copy(
                panel = shown,
                // A song change may arrive before its lyrics do.
                lyrics = lyrics?.takeIf { it.songId == songId }?.ui,
                visualizer = visualizer,
                equalizer = equalizer,
            )
        }
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

    fun showPanel(shown: PlayerPanel) {
        panel.value = shown
    }

    fun setVisualizer(value: VisualizerSettings) {
        viewModelScope.launch { settings.setVisualizer(value) }
    }

    fun setEqualizer(value: EqualizerSettings) {
        viewModelScope.launch { settings.updateEqualizer { value } }
    }

    fun currentSongId(): Long? = player.state.value.current?.songId

    fun editLyrics() {
        val id = currentSongId() ?: return
        draft = LyricsDraft(id, storedLyrics.value?.takeIf { it.songId == id }?.text.orEmpty())
    }

    fun changeDraft(text: String) {
        draft = draft?.copy(text = text.take(MAX_LYRICS_CHARS))
    }

    fun saveDraft() {
        val edited = draft ?: return
        draft = null
        // Saving unchanged text would only reset the timing adjustment.
        if (edited.text == storedLyrics.value?.takeIf { it.songId == edited.songId }?.text) return
        viewModelScope.launch { lyricsRepository.save(edited.songId, edited.text) }
    }

    fun discardDraft() {
        draft = null
    }

    fun importLyrics(songId: Long, file: Uri) {
        viewModelScope.launch { importResult = lyricsRepository.import(songId, file) }
    }

    fun importResultShown() {
        importResult = null
    }

    fun askToRemoveLyrics() {
        removing = currentSongId()
    }

    fun finishRemoving(confirmed: Boolean) {
        val id = removing ?: return
        removing = null
        if (confirmed) viewModelScope.launch { lyricsRepository.delete(id) }
    }

    fun setLyricsOffset(offsetMs: Long) {
        val id = currentSongId() ?: return
        viewModelScope.launch { lyricsRepository.setOffset(id, offsetMs) }
    }

    private companion object {
        const val MAX_LYRICS_CHARS = 100_000
    }
}
