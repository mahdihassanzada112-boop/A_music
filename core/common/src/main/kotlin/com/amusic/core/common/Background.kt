package com.amusic.core.common

/**
 * The background styles, in the order they are offered. A new style is added here and given a
 * drawing in the theming module; playback is not involved.
 */
enum class BackgroundStyle(
    /** Moves over time, so it needs a frame loop. */
    val animated: Boolean = true,
    /** Draws the audio itself, not only a pulse from it. */
    val showsAudio: Boolean = false,
    val usesArtwork: Boolean = false,
) {
    DYNAMIC(animated = false, usesArtwork = true),
    BLUR(animated = false, usesArtwork = true),
    SOLID(animated = false),
    ANIMATED,
    MESH,
    PARTICLES,
    AURORA,
    LIQUID,
    SPECTRUM(showsAudio = true),
    WAVEFORM(showsAudio = true),
    GEOMETRIC,
    CINEMATIC,
    ALBUM_3D(usesArtwork = true),
    SYNTHWAVE,
}

/** AUTO follows the scene's palette; CUSTOM uses the layer's own color. */
enum class BackgroundPalette(val colors: List<Int> = emptyList()) {
    AUTO,
    ARTWORK,
    THEME,
    SUNSET(listOf(0xFFFF5E62.toInt(), 0xFFFF9966.toInt(), 0xFF8E2DE2.toInt(), 0xFF2B1055.toInt())),
    OCEAN(listOf(0xFF1CB5E0.toInt(), 0xFF0083B0.toInt(), 0xFF000C66.toInt(), 0xFF43E8D8.toInt())),
    FOREST(listOf(0xFF38EF7D.toInt(), 0xFF11998E.toInt(), 0xFF0B3D2E.toInt(), 0xFF7B61FF.toInt())),
    NEON(listOf(0xFFFF2BD6.toInt(), 0xFF00F0FF.toInt(), 0xFF7A2CFF.toInt(), 0xFFFFE45E.toInt())),
    EMBER(listOf(0xFFFF8008.toInt(), 0xFFB31217.toInt(), 0xFF3A0A0A.toInt(), 0xFFFFC837.toInt())),
    MONO(listOf(0xFFE8E8EE.toInt(), 0xFF8A8D99.toInt(), 0xFF2A2C34.toInt(), 0xFFB9BCC8.toInt())),
    CUSTOM,
}

data class BackgroundLayer(
    val style: BackgroundStyle,
    val enabled: Boolean = true,
    val opacity: Float = 1f,
    val intensity: Float = 1f,
    val speed: Float = 1f,
    val palette: BackgroundPalette = BackgroundPalette.AUTO,
    /** Opaque ARGB, used when [palette] is CUSTOM. */
    val color: Int = DEFAULT_COLOR,
) {
    fun clamped() = copy(
        opacity = opacity.within(OPACITY, 1f),
        intensity = intensity.within(INTENSITY, 1f),
        speed = speed.within(SPEED, 1f),
        color = color or 0xFF000000.toInt(),
    )

    companion object {
        val OPACITY = 0.05f..1f
        val INTENSITY = 0.2f..1.5f
        val SPEED = 0.25f..2.5f
        const val DEFAULT_COLOR = 0xFF2B2150.toInt()
    }
}

/** The advanced settings, which apply to every layer. */
enum class LookSetting(val range: ClosedFloatingPointRange<Float>, val default: Float) {
    BLUR(0f..1f, 0.55f),
    BRIGHTNESS(0.4f..1.3f, 1f),
    SATURATION(0f..1.8f, 1f),
    CONTRAST(0.6f..1.5f, 1f),
    SPEED(0.25f..2f, 1f),
    DENSITY(0.25f..2f, 1f),
    GLOW(0f..1.5f, 1f),
    /** How much of the theme's own background shows through. */
    TRANSPARENCY(0f..0.8f, 0f),
    BASS(0f..2f, 1f),
    BEAT(0f..2f, 1f),
    /** How far things travel; zero holds everything still. */
    MOTION(0f..1.5f, 1f),
}

data class BackgroundLook(private val changed: Map<LookSetting, Float> = emptyMap()) {
    operator fun get(setting: LookSetting): Float = changed[setting] ?: setting.default

    fun with(setting: LookSetting, value: Float): BackgroundLook {
        val safe = value.within(setting.range, setting.default)
        return BackgroundLook(if (safe == setting.default) changed - setting else changed + (setting to safe))
    }
}

/** A complete background: [layers] are drawn first to last, so the last one is on top. */
data class BackgroundScene(
    val layers: List<BackgroundLayer> = emptyList(),
    val look: BackgroundLook = BackgroundLook(),
    val palette: BackgroundPalette = BackgroundPalette.ARTWORK,
) {
    val visibleLayers get() = layers.filter { it.enabled }

    /** Styles not yet in the scene; each can be used once. */
    val unused get() = BackgroundStyle.entries.filter { style -> layers.none { it.style == style } }

    val canAdd get() = layers.size < MAX_LAYERS && unused.isNotEmpty()

    fun add(style: BackgroundStyle) =
        if (!canAdd || layers.any { it.style == style }) this
        else copy(layers = layers + defaultLayer(style))

    fun remove(style: BackgroundStyle) = copy(layers = layers.filterNot { it.style == style })

    fun update(layer: BackgroundLayer) =
        copy(layers = layers.map { if (it.style == layer.style) layer.clamped() else it })

    /** [up] moves the layer toward the top of the picture. */
    fun move(style: BackgroundStyle, up: Boolean): BackgroundScene {
        val from = layers.indexOfFirst { it.style == style }
        val to = if (up) from + 1 else from - 1
        if (from < 0 || to !in layers.indices) return this
        val moved = layers.toMutableList().apply { add(to, removeAt(from)) }
        return copy(layers = moved)
    }

    companion object {
        const val MAX_LAYERS = 6

        /** Layers meant to sit over others start partly transparent. */
        fun defaultLayer(style: BackgroundStyle) = BackgroundLayer(
            style,
            // A solid background is mostly about picking its color.
            palette = if (style == BackgroundStyle.SOLID) BackgroundPalette.CUSTOM else BackgroundPalette.AUTO,
            opacity = when (style) {
                BackgroundStyle.MESH -> 0.6f
                BackgroundStyle.SPECTRUM, BackgroundStyle.WAVEFORM -> 0.8f
                BackgroundStyle.CINEMATIC -> 0.85f
                else -> 1f
            },
        )

        val Default = BackgroundScene(listOf(defaultLayer(BackgroundStyle.DYNAMIC)))
    }
}

/** What happens to the background when the next song starts. */
enum class AutoBackground {
    OFF, IN_ORDER, SHUFFLE;

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: OFF
    }
}

data class BackgroundSettings(
    /** Off shows the theme's own background. */
    val enabled: Boolean = false,
    val scene: BackgroundScene = BackgroundScene.Default,
    val auto: AutoBackground = AutoBackground.OFF,
    val batterySaver: Boolean = false,
    val fpsLimit: Int = DEFAULT_FPS,
) {
    companion object {
        val FPS_CHOICES = listOf(24, 30, 45, 60)
        const val DEFAULT_FPS = 30
    }
}

private fun Float.within(range: ClosedFloatingPointRange<Float>, fallback: Float) =
    if (isNaN()) fallback else coerceIn(range)
