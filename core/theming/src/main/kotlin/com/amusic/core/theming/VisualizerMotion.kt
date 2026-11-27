package com.amusic.core.theming

import com.amusic.core.common.AudioFrame
import com.amusic.core.common.VisualizerSettings
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Smooths raw audio frames into what is drawn: fast attack, gentle fall. */
internal class VisualizerMotion {
    val levels = FloatArray(AudioFrame.BANDS)
    val peaks = FloatArray(AudioFrame.BANDS)
    val wave = FloatArray(AudioFrame.WAVE)

    /** Recent [levels], newest first, for the depth view. */
    val history = ArrayDeque<FloatArray>()
    private var sinceHistory = 0f

    /** Seconds of motion, already scaled by the speed setting and the loudness. */
    var phase = 0f
        private set

    class Particle(var angle: Float, var distance: Float, val band: Int, val pace: Float)

    private val random = Random(7)
    val particles = List(90) {
        Particle(random.nextFloat() * 6.2832f, random.nextFloat(), it % AudioFrame.BANDS, 0.5f + random.nextFloat())
    }

    /** True once nothing is left moving, so drawing can stop. */
    val settled: Boolean
        get() = peaks.all { it < 0.005f } && wave.all { abs(it) < 0.005f } &&
            history.all { row -> row.all { it < 0.005f } }

    fun advance(frame: AudioFrame, settings: VisualizerSettings, seconds: Float) {
        val speed = settings.speed
        var energy = 0f
        for (i in levels.indices) {
            val target = (frame.bands[i] * settings.sensitivity).coerceIn(0f, 1f)
            levels[i] = if (target > levels[i]) {
                levels[i] + (target - levels[i]) * min(1f, seconds * 25f * speed)
            } else {
                max(target, levels[i] - seconds * 1.8f * speed)
            }
            peaks[i] = max(levels[i], peaks[i] - seconds * 0.5f * speed)
            energy += levels[i]
        }
        energy /= levels.size
        val blend = min(1f, seconds * 20f * speed)
        for (i in wave.indices) {
            val target = (frame.wave[i] * settings.sensitivity).coerceIn(-1f, 1f)
            wave[i] += (target - wave[i]) * blend
        }
        phase += seconds * speed * (0.4f + energy * 3f)
        for (particle in particles) {
            particle.distance += seconds * speed * particle.pace * (0.05f + levels[particle.band])
            if (particle.distance > 1f) {
                particle.distance -= 1f
                particle.angle = random.nextFloat() * 6.2832f
            }
        }
        sinceHistory += seconds * speed
        if (sinceHistory >= 0.07f) {
            sinceHistory = 0f
            history.addFirst(levels.copyOf())
            if (history.size > 9) history.removeLast()
        }
    }
}
