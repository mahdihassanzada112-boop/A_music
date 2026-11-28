package com.amusic.core.common

import kotlin.random.Random

/** Ready-made combinations that ship with the app. */
enum class BuiltInPreset(val scene: BackgroundScene) {
    /** The layered example: artwork, gradient mesh, particles, spectrum and cinema light. */
    CONCERT(
        scene(
            BackgroundStyle.BLUR, BackgroundStyle.MESH, BackgroundStyle.PARTICLES,
            BackgroundStyle.SPECTRUM, BackgroundStyle.CINEMATIC,
        ),
    ),
    NORTHERN_NIGHT(scene(BackgroundStyle.ANIMATED, BackgroundStyle.AURORA, palette = BackgroundPalette.FOREST)),
    NEON_DRIVE(scene(BackgroundStyle.SYNTHWAVE, BackgroundStyle.PARTICLES, palette = BackgroundPalette.NEON)),
    DEEP_WATER(scene(BackgroundStyle.LIQUID, BackgroundStyle.WAVEFORM, palette = BackgroundPalette.OCEAN)),
    GALLERY(scene(BackgroundStyle.DYNAMIC, BackgroundStyle.ALBUM_3D)),
    SHAPES(scene(BackgroundStyle.DYNAMIC, BackgroundStyle.GEOMETRIC, BackgroundStyle.CINEMATIC)),
    CALM(scene(BackgroundStyle.SOLID)),
}

private fun scene(vararg styles: BackgroundStyle, palette: BackgroundPalette = BackgroundPalette.ARTWORK) =
    BackgroundScene(styles.map(BackgroundScene::defaultLayer), palette = palette)

/**
 * The scene for the next song, chosen from [pool] and never equal to [current] unless the
 * pool offers nothing else.
 */
fun nextScene(
    pool: List<BackgroundScene>,
    current: BackgroundScene,
    mode: AutoBackground,
    random: Random = Random,
): BackgroundScene {
    if (mode == AutoBackground.OFF || pool.isEmpty()) return current
    val others = pool.distinct().filter { it != current }
    if (others.isEmpty()) return current
    if (mode == AutoBackground.SHUFFLE) return others[random.nextInt(others.size)]
    val distinct = pool.distinct()
    return distinct[(distinct.indexOf(current) + 1) % distinct.size]
}

/**
 * The user's own presets rotate once at least two of them differ; until then the built-in
 * ones join in.
 */
fun rotationPool(saved: List<BackgroundScene>): List<BackgroundScene> =
    if (saved.distinct().size >= 2) saved else saved + BuiltInPreset.entries.map { it.scene }
