package com.amusic.core.audio

import android.net.Uri
import androidx.media3.common.Player

data class QueueItem(val songId: Long, val title: String, val artist: String)

enum class RepeatSetting { OFF, ALL, ONE }

data class PlayerState(
    val queue: List<QueueItem> = emptyList(),
    val index: Int = 0,
    val album: String = "",
    val artworkUri: Uri? = null,
    val durationMs: Long = 0,
    val isPlaying: Boolean = false,
    /** True when the next tap on play/pause would pause. */
    val showPause: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: RepeatSetting = RepeatSetting.OFF,
    val speed: Float = 1f,
    val hasError: Boolean = false,
) {
    val current: QueueItem? get() = queue.getOrNull(index)
}

internal fun repeatSettingOf(mode: Int) = when (mode) {
    Player.REPEAT_MODE_ALL -> RepeatSetting.ALL
    Player.REPEAT_MODE_ONE -> RepeatSetting.ONE
    else -> RepeatSetting.OFF
}
