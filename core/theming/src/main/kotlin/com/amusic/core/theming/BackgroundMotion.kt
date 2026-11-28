package com.amusic.core.theming

import com.amusic.core.common.AudioFrame
import com.amusic.core.common.BackgroundScene
import com.amusic.core.common.BackgroundStyle
import com.amusic.core.common.BeatTracker
import com.amusic.core.common.LookSetting
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Everything that changes between frames: each layer's clock and the state of the music. */
internal class BackgroundMotion {
    /** Seconds of motion per style, already scaled by the speed settings. */
    private val phases = FloatArray(BackgroundStyle.entries.size) { it * 7.3f }
    private val tracker = BeatTracker()
    val levels = FloatArray(AudioFrame.BANDS)
    val wave = FloatArray(AudioFrame.WAVE)

    /** Bass and beat after the user's sensitivity settings. */
    var bass = 0f
        private set
    var beat = 0f
        private set

    /** Eases motion in and out: 1 while running, falling to 0 after a pause. */
    private var drive = 0f

    fun phase(style: BackgroundStyle) = phases[style.ordinal]

    /** True once nothing is left moving, so drawing can stop. */
    val settled: Boolean
        get() = drive <= 0f && bass < 0.005f && beat == 0f &&
            levels.all { it < 0.005f } && wave.all { abs(it) < 0.005f }

    fun advance(frame: AudioFrame, scene: BackgroundScene, running: Boolean, seconds: Float) {
        drive = if (running) min(1f, drive + seconds * 3f) else max(0f, drive - seconds * 1.2f)
        val look = scene.look
        for (layer in scene.layers) {
            phases[layer.style.ordinal] +=
                seconds * drive * layer.speed * look[LookSetting.SPEED] * look[LookSetting.MOTION]
        }
        tracker.advance(frame.bands, seconds)
        bass = (tracker.bass * look[LookSetting.BASS]).coerceIn(0f, 1f)
        beat = (tracker.beat * look[LookSetting.BEAT]).coerceIn(0f, 1f)
        for (i in levels.indices) {
            val target = (frame.bands[i] * look[LookSetting.BASS].coerceAtLeast(0.5f)).coerceIn(0f, 1f)
            levels[i] = if (target > levels[i]) {
                levels[i] + (target - levels[i]) * min(1f, seconds * 18f)
            } else {
                max(target, levels[i] - seconds * 1.6f)
            }
        }
        for (i in wave.indices) wave[i] += (frame.wave[i] - wave[i]) * min(1f, seconds * 15f)
    }
}
