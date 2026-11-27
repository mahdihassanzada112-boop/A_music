package com.amusic.core.common

enum class VisualizerStyle {
    BARS, CIRCULAR, WAVEFORM, NEON, PARTICLES, FLUID, DEPTH, RETRO, MINIMAL;

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: BARS
    }
}

/** THEME follows the current theme's accent. */
enum class VisualizerColor {
    THEME, AMBER, CYAN, MAGENTA, GREEN, RAINBOW;

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: THEME
    }
}

data class VisualizerSettings(
    val enabled: Boolean = false,
    val style: VisualizerStyle = VisualizerStyle.BARS,
    val color: VisualizerColor = VisualizerColor.THEME,
    /** How tall and bright the drawing is. */
    val intensity: Float = 1f,
    /** How fast motion and falling bars are. */
    val speed: Float = 1f,
    /** Gain applied to the audio levels. */
    val sensitivity: Float = 1f,
) {
    fun clamped() = copy(
        intensity = intensity.coerceIn(INTENSITY),
        speed = speed.coerceIn(SPEED),
        sensitivity = sensitivity.coerceIn(SENSITIVITY),
    )

    companion object {
        val INTENSITY = 0.4f..1.5f
        val SPEED = 0.5f..2f
        val SENSITIVITY = 0.5f..2f
    }
}

/** One moment of the music: [bands] from low to high in 0..1, [wave] samples in -1..1. */
class AudioFrame(val bands: FloatArray, val wave: FloatArray) {
    companion object {
        const val BANDS = 32
        const val WAVE = 64
        val SILENCE = AudioFrame(FloatArray(BANDS), FloatArray(WAVE))
    }
}

/** What the speakers are playing now, taken from the app's own playback pipeline. */
interface AudioFeed {
    /** Analysis runs only between this call and closing the result. */
    fun watch(): AutoCloseable

    fun frame(): AudioFrame
}
