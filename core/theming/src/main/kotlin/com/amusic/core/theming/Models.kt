package com.amusic.core.theming

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import com.amusic.core.common.EqualizerSettings
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.ParsedLyrics
import com.amusic.core.common.Song
import com.amusic.core.common.VisualizerSettings

enum class RepeatUi { OFF, ALL, ONE }

/** What the player shows besides its controls. */
enum class PlayerPanel { NONE, QUEUE, LYRICS }

@Immutable
data class LyricsUi(val parsed: ParsedLyrics, val offsetMs: Long)

@Immutable
data class QueueEntry(val title: String, val artist: String)

/** Everything a theme may draw. Themes never see the playback engine. */
@Immutable
data class PlayerUi(
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: Uri?,
    val durationMs: Long,
    val isPlaying: Boolean,
    val showPause: Boolean,
    val shuffle: Boolean,
    val repeat: RepeatUi,
    val speed: Float,
    val favorite: Boolean,
    val hasError: Boolean,
    val queue: List<QueueEntry>,
    val queueIndex: Int,
    /** Minutes until playback pauses, or null when the sleep timer is off. */
    val sleepMinutesLeft: Int?,
    val orientationMode: OrientationMode,
    val panel: PlayerPanel,
    /** null when the current song has no lyrics. */
    val lyrics: LyricsUi?,
    val visualizer: VisualizerSettings,
    val equalizer: EqualizerSettings,
)

/** The only ways a theme can change anything. */
interface PlayerActions {
    fun back()
    fun togglePlay()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun cycleRepeat()
    fun toggleFavorite()
    fun setSpeed(speed: Float)
    /** null switches the timer off. */
    fun setSleepTimer(minutes: Int?)
    fun jumpTo(index: Int)
    fun removeAt(index: Int)
    fun move(from: Int, to: Int)
    fun clearQueue()
    fun setOrientationMode(mode: OrientationMode)
    fun showPanel(panel: PlayerPanel)
    fun setVisualizer(settings: VisualizerSettings)
    fun openVisualizerOptions()
    fun openBackgroundStudio()
    fun setEqualizer(settings: EqualizerSettings)
    fun openEqualizer()
    /** The editor for the current theme's landscape arrangement. */
    fun openLayoutEditor()
    fun importLyrics()
    fun editLyrics()
    /** Asks for confirmation first. */
    fun removeLyrics()
    fun setLyricsOffset(offsetMs: Long)
}

/** Position changes twice a second, so it is separate from [PlayerUi]. */
typealias Position = State<Long>

enum class HomeStatus { IDLE, SCANNING, FAILED }

@Immutable
data class HomeUi(
    val songCount: Int,
    val status: HomeStatus,
    val recentlyPlayed: List<Song>,
    val recentlyAdded: List<Song>,
)

interface HomeActions {
    fun shuffleAll()
    fun rescan()
    fun play(songs: List<Song>, index: Int)
}

@Immutable
data class NavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
