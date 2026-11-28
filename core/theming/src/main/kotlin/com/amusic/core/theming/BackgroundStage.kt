package com.amusic.core.theming

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.amusic.core.common.BackgroundLayer
import com.amusic.core.common.BackgroundPalette
import com.amusic.core.common.BackgroundSettings
import com.amusic.core.common.BackgroundStyle
import com.amusic.core.common.LookSetting
import com.amusic.core.common.applyColorMatrix
import com.amusic.core.common.boxBlur
import com.amusic.core.common.colorMatrix
import com.amusic.core.common.dominantColors
import com.amusic.core.common.renderBudget
import com.amusic.core.design.loadArtwork
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class Cover(val uri: Uri, val bitmap: Bitmap, val colors: List<Int>)

private val Backdrop = Color(0xFF07070C)

/**
 * The studio background: the scene's layers drawn over a dark base, filling [modifier].
 * It moves while [running] and this screen is visible, and stands still otherwise. Whatever
 * is behind it shows through as far as the transparency setting allows.
 */
@Composable
fun BackgroundStage(
    settings: BackgroundSettings,
    artworkUri: Uri?,
    running: Boolean,
    modifier: Modifier = Modifier,
) {
    val scene = settings.scene
    val look = scene.look
    val context = LocalContext.current
    val lowEnd = remember { context.isLowEnd() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var powerSave by remember { mutableStateOf(context.isPowerSaving()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { powerSave = context.isPowerSaving() }
    }
    val budget = renderBudget(settings.fpsLimit, settings.batterySaver || powerSave, lowEnd)

    val visible = scene.visibleLayers
    val needsArtwork = visible.any { it.style.usesArtwork } ||
        (listOf(scene.palette) + visible.map { it.palette }).any {
            it == BackgroundPalette.ARTWORK || it == BackgroundPalette.AUTO
        }
    val cover = rememberCover(artworkUri.takeIf { needsArtwork })
    // 0 is nearly sharp, 10 is very soft; steps avoid recomputing on every slider movement.
    val blurStep = (look[LookSetting.BLUR] * 10).roundToInt()
    val wantsBlur = visible.any { it.style == BackgroundStyle.BLUR }
    val blurred by produceState<Bitmap?>(null, cover, blurStep, wantsBlur) {
        value = if (cover == null || !wantsBlur) null else withContext(Dispatchers.Default) {
            blur(cover.bitmap, blurStep)
        }
    }
    val images = remember(cover, blurred) {
        ArtworkImages(cover?.bitmap?.asImageBitmap(), blurred?.asImageBitmap())
    }

    // Colors, already passed through brightness, saturation and contrast.
    val matrix = remember(look) {
        colorMatrix(look[LookSetting.BRIGHTNESS], look[LookSetting.SATURATION], look[LookSetting.CONTRAST])
    }
    val filter = remember(matrix) { ColorFilter.colorMatrix(ColorMatrix(matrix)) }
    val scheme = MaterialTheme.colorScheme
    val themeColors = listOf(
        scheme.primary, scheme.tertiary,
        lerp(scheme.background, scheme.primary, 0.25f), scheme.secondary,
    ).map { it.toArgb() or 0xFF000000.toInt() }
    val artColors = cover?.colors.orEmpty().let { found ->
        if (found.isEmpty()) themeColors else completed(found)
    }
    fun paletteColors(layer: BackgroundLayer): List<Int> {
        var palette = layer.palette
        if (palette == BackgroundPalette.AUTO) palette = scene.palette
        return when (palette) {
            BackgroundPalette.ARTWORK, BackgroundPalette.AUTO -> artColors
            BackgroundPalette.THEME -> themeColors
            BackgroundPalette.CUSTOM -> completed(listOf(layer.color))
            else -> palette.colors
        }
    }
    val colors = visible.associate { layer ->
        layer.style to paletteColors(layer).map { Color(applyColorMatrix(it, matrix)) }
    }

    val motion = remember { BackgroundMotion() }
    var frameTime by remember { mutableLongStateOf(0) }
    val latestScene by rememberUpdatedState(scene)
    val feed = LocalAudioFeed.current
    val animated = visible.any { it.style.animated }
    LaunchedEffect(animated, running, budget.fps, feed, lifecycle) {
        if (!animated) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val watch = if (running) feed.watch() else null
            try {
                val interval = 1_000L / budget.fps
                var last = 0L
                while (running || !motion.settled) {
                    withInfiniteAnimationFrameMillis { now ->
                        // A few milliseconds of slack keeps the rate steady on 60 Hz screens.
                        if (last == 0L || now - last >= interval - 4) {
                            val seconds = if (last == 0L) 0.016f else ((now - last) / 1_000f).coerceIn(0f, 0.1f)
                            last = now
                            motion.advance(feed.frame(), latestScene, running, seconds)
                            frameTime = now
                        }
                    }
                }
            } finally {
                watch?.close()
            }
        }
    }

    // A new combination fades in instead of appearing at once.
    val fade = remember { Animatable(1f) }
    val combination = visible.map { it.style }
    var previous by remember { mutableStateOf(combination) }
    LaunchedEffect(combination) {
        if (previous != combination) {
            previous = combination
            fade.snapTo(0.2f)
            fade.animateTo(1f, tween(700))
        }
    }

    val strength by animateFloatAsState(1f - look[LookSetting.TRANSPARENCY], label = "background")
    val path = remember { Path() }
    Box(modifier.testTag("background_stage")) {
        Canvas(Modifier.matchParentSize().clipToBounds().graphicsLayer { alpha = fade.value }) {
            frameTime // reading it redraws the canvas on every frame
            drawRect(Backdrop.copy(alpha = strength))
            for (layer in visible) {
                val layerColors = colors.getValue(layer.style)
                drawBackgroundLayer(
                    LayerPaint(
                        layer = layer,
                        colors = layerColors,
                        alpha = layer.opacity * strength,
                        look = look,
                        budget = budget,
                        filter = filter,
                        tint = layerColors.first().takeIf { layer.palette != BackgroundPalette.AUTO },
                    ),
                    motion, images, path,
                )
            }
            // Keeps the player's white text readable over bright combinations.
            drawRect(Color.Black.copy(alpha = 0.24f * strength))
        }
    }
}

/** Keeps the previous cover until the next one has loaded, so songs blend into each other. */
@Composable
private fun rememberCover(uri: Uri?): Cover? {
    val context = LocalContext.current
    val cover by produceState<Cover?>(null, uri) {
        if (value?.uri == uri) return@produceState
        value = if (uri == null) null else loadArtwork(context, uri, 384)?.let { bitmap ->
            withContext(Dispatchers.Default) { Cover(uri, bitmap, colorsOf(bitmap)) }
        }
    }
    return cover
}

/** Some covers are decoded as hardware bitmaps, whose pixels cannot be read directly. */
private fun readable(bitmap: Bitmap): Bitmap =
    if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)

private fun colorsOf(bitmap: Bitmap): List<Int> {
    val small = Bitmap.createScaledBitmap(readable(bitmap), 48, 48, true)
    val pixels = IntArray(48 * 48)
    small.getPixels(pixels, 0, 48, 0, 0, 48, 48)
    return dominantColors(pixels)
}

private fun blur(bitmap: Bitmap, step: Int): Bitmap {
    // Softer settings use a smaller picture, which blurs further for the same work.
    val side = 240 - step * 17
    val small = Bitmap.createScaledBitmap(readable(bitmap), side, side, true)
    val pixels = IntArray(side * side)
    small.getPixels(pixels, 0, side, 0, 0, side, side)
    return Bitmap.createBitmap(boxBlur(pixels, side, side, step * 6 / 10), side, side, Bitmap.Config.ARGB_8888)
}

/** At least four colors, filling in lighter and darker relatives. */
private fun completed(colors: List<Int>): List<Int> {
    val result = colors.map { Color(it) }.toMutableList()
    val first = result.first()
    val relatives = listOf(lerp(first, Color.White, 0.3f), lerp(first, Color.Black, 0.45f), lerp(first, Color.White, 0.15f))
    var next = 0
    while (result.size < 4) result += relatives[next++]
    return result.map { it.toArgb() }
}

private fun Context.isPowerSaving() =
    (getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode

private fun Context.isLowEnd(): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.isLowRamDevice || manager.memoryClass <= 128
}
