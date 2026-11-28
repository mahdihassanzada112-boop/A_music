package com.amusic.core.theming

import android.graphics.Camera
import android.graphics.Matrix
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.amusic.core.common.BackgroundLayer
import com.amusic.core.common.BackgroundStyle
import com.amusic.core.common.LookSetting
import com.amusic.core.common.BackgroundLook
import com.amusic.core.common.RenderBudget
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TAU = (PI * 2).toFloat()

/** The cover, ready to draw: sharp for the 3D card and blurred for the backdrop. */
internal class ArtworkImages(val sharp: ImageBitmap?, val blurred: ImageBitmap?)

/** What one layer is drawn with. [colors] always holds at least four, already adjusted. */
internal class LayerPaint(
    val layer: BackgroundLayer,
    val colors: List<Color>,
    /** Layer opacity after the overall transparency. */
    val alpha: Float,
    val look: BackgroundLook,
    val budget: RenderBudget,
    val filter: ColorFilter?,
    /** The color to wash artwork with, when the layer has its own palette. */
    val tint: Color?,
) {
    val intensity get() = layer.intensity
    val glow get() = if (budget.glow) look[LookSetting.GLOW] else 0f

    fun color(index: Int, alpha: Float = 1f) =
        colors[index.mod(colors.size)].copy(alpha = (alpha * this.alpha).coerceIn(0f, 1f))

    /** How many of [normal] things to draw. */
    fun count(normal: Int) = (normal * look[LookSetting.DENSITY] * budget.detail).roundToInt().coerceAtLeast(1)
}

/** A fixed pseudo-random number in 0..1, so particles need no stored state. */
private fun noise(index: Int, salt: Int): Float {
    var x = index * 374_761_393 + salt * 668_265_263
    x = (x xor (x ushr 13)) * 1_274_126_177
    return ((x xor (x ushr 16)) and 0xFFFF) / 65_535f
}

private fun wrap(value: Float) = value - kotlin.math.floor(value)

private fun DrawScope.glowSpot(center: Offset, radius: Float, color: Color, blend: BlendMode = BlendMode.SrcOver) {
    if (radius <= 0f) return
    drawCircle(
        Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius),
        radius, center, blendMode = blend,
    )
}

internal fun DrawScope.drawBackgroundLayer(
    paint: LayerPaint,
    motion: BackgroundMotion,
    art: ArtworkImages,
    path: Path,
) {
    val t = motion.phase(paint.layer.style)
    when (paint.layer.style) {
        BackgroundStyle.DYNAMIC -> dynamicColors(paint)
        BackgroundStyle.BLUR -> blurredArt(paint, art)
        BackgroundStyle.SOLID -> solid(paint)
        BackgroundStyle.ANIMATED -> animated(paint, motion, t)
        BackgroundStyle.MESH -> mesh(paint, motion, t)
        BackgroundStyle.PARTICLES -> particles(paint, motion, t)
        BackgroundStyle.AURORA -> aurora(paint, motion, t, path)
        BackgroundStyle.LIQUID -> liquid(paint, motion, t, path)
        BackgroundStyle.SPECTRUM -> spectrum(paint, motion)
        BackgroundStyle.WAVEFORM -> waveform(paint, motion, t, path)
        BackgroundStyle.GEOMETRIC -> geometric(paint, motion, t, path)
        BackgroundStyle.CINEMATIC -> cinematic(paint, motion, t, path)
        BackgroundStyle.ALBUM_3D -> album3d(paint, motion, t, art)
        BackgroundStyle.SYNTHWAVE -> synthwave(paint, motion, t, path)
    }
}

private fun DrawScope.dynamicColors(p: LayerPaint) {
    val dim = (0.75f - p.intensity * 0.3f).coerceIn(0.2f, 0.75f)
    drawRect(
        Brush.verticalGradient(
            listOf(
                lerp(p.color(0), Color.Black, dim * 0.6f).copy(alpha = p.alpha),
                lerp(p.color(1), Color.Black, dim).copy(alpha = p.alpha),
                lerp(p.color(2), Color.Black, 0.85f).copy(alpha = p.alpha),
            ),
        ),
    )
    val reach = size.maxDimension * 0.55f
    glowSpot(Offset(size.width * 0.85f, size.height * 0.12f), reach, p.color(1, 0.45f * p.intensity))
    glowSpot(Offset(size.width * 0.1f, size.height * 0.7f), reach, p.color(3, 0.3f * p.intensity))
}

private fun DrawScope.cover(image: ImageBitmap, zoom: Float, alpha: Float, filter: ColorFilter?) {
    val scale = max(size.width / image.width, size.height / image.height) * zoom
    val width = (image.width * scale).roundToInt()
    val height = (image.height * scale).roundToInt()
    drawImage(
        image,
        dstOffset = IntOffset(((size.width - width) / 2).roundToInt(), ((size.height - height) / 2).roundToInt()),
        dstSize = IntSize(width, height),
        alpha = alpha,
        colorFilter = filter,
    )
}

private fun DrawScope.blurredArt(p: LayerPaint, art: ArtworkImages) {
    val image = art.blurred
    // Without a cover there is still a soft backdrop.
    if (image == null) {
        dynamicColors(p)
        return
    }
    clipRect {
        // Intensity is depth: how far the picture is pushed in and how dark its edges are.
        cover(image, 1f + p.intensity * 0.2f, p.alpha, p.filter)
    }
    p.tint?.let { drawRect(it.copy(alpha = 0.35f * p.alpha)) }
    drawRect(
        Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = (0.5f * p.intensity * p.alpha).coerceIn(0f, 1f))),
            center, size.maxDimension * 0.75f,
        ),
    )
}

private fun DrawScope.solid(p: LayerPaint) {
    val base = p.color(0)
    val shade = (p.intensity - 1f)
    val adjusted = if (shade >= 0f) lerp(base, Color.White, shade * 0.25f) else lerp(base, Color.Black, -shade)
    drawRect(adjusted.copy(alpha = p.alpha))
}

private fun DrawScope.animated(p: LayerPaint, m: BackgroundMotion, t: Float) {
    // Colors hand over to one another while the light direction slowly turns.
    val step = t * 0.12f
    val from = step.toInt()
    val mix = wrap(step)
    fun blend(i: Int) = lerp(p.color(from + i), p.color(from + i + 1), mix)
    val angle = t * 0.09f
    val reach = size.maxDimension * 0.7f
    val direction = Offset(cos(angle) * reach, sin(angle) * reach)
    drawRect(
        Brush.linearGradient(
            listOf(
                lerp(blend(0), Color.Black, 0.25f).copy(alpha = p.alpha),
                lerp(blend(1), Color.Black, 0.55f).copy(alpha = p.alpha),
                lerp(blend(2), Color.Black, 0.3f).copy(alpha = p.alpha),
            ),
            center - direction, center + direction,
        ),
    )
    val drift = Offset(
        size.width * (0.5f + 0.4f * sin(t * 0.17f)),
        size.height * (0.5f + 0.35f * cos(t * 0.13f)),
    )
    glowSpot(drift, size.minDimension * (0.6f + m.bass * 0.2f), blend(3).copy(alpha = (0.4f * p.intensity * p.alpha).coerceIn(0f, 1f)), BlendMode.Screen)
}

private fun DrawScope.mesh(p: LayerPaint, m: BackgroundMotion, t: Float) {
    for (i in 0 until 6) {
        val speed = 0.12f + noise(i, 1) * 0.18f
        val center = Offset(
            size.width * (0.5f + 0.55f * sin(t * speed + noise(i, 2) * TAU)),
            size.height * (0.5f + 0.52f * cos(t * speed * 0.8f + noise(i, 3) * TAU)),
        )
        val radius = size.maxDimension * (0.28f + 0.18f * noise(i, 4)) * (1f + m.bass * 0.18f)
        glowSpot(center, radius, p.color(i, 0.75f * p.intensity))
    }
}

private fun DrawScope.particles(p: LayerPaint, m: BackgroundMotion, t: Float) {
    val unit = 1.dp.toPx()
    for (i in 0 until p.count(70)) {
        val rise = 0.015f + noise(i, 1) * 0.05f
        val y = wrap(noise(i, 2) - t * rise) * size.height
        val x = wrap(noise(i, 3) + 0.04f * sin(t * (0.2f + noise(i, 4)) + i)) * size.width
        val twinkle = 0.55f + 0.45f * sin(t * (0.6f + noise(i, 5) * 1.6f) + i)
        val radius = unit * (1f + noise(i, 6) * 3f) * p.intensity * (1f + m.beat * 0.8f)
        if (p.glow > 0f && i % 2 == 0) {
            glowSpot(Offset(x, y), radius * (3f + 3f * p.glow), p.color(i, 0.35f * twinkle * p.glow), BlendMode.Plus)
        }
        drawCircle(lerp(p.color(i), Color.White, 0.45f).copy(alpha = (twinkle * p.alpha).coerceIn(0f, 1f)), radius, Offset(x, y))
    }
}

private fun DrawScope.aurora(p: LayerPaint, m: BackgroundMotion, t: Float, path: Path) {
    val steps = (28 * p.budget.detail).toInt().coerceAtLeast(10)
    for (ribbon in 0 until 4) {
        val top = size.height * (0.08f + ribbon * 0.11f)
        val height = size.height * (0.3f + 0.05f * ribbon) * (1f + m.bass * 0.25f)
        val wave = size.height * 0.07f * p.intensity
        path.reset()
        fun edge(i: Int): Float {
            val x = i / steps.toFloat()
            return top + wave * sin(x * (3.1f + ribbon) + t * (0.35f + ribbon * 0.08f)) +
                wave * 0.6f * sin(x * (7.3f - ribbon) - t * 0.22f + ribbon * 2f)
        }
        path.moveTo(0f, edge(0))
        for (i in 1..steps) path.lineTo(size.width * i / steps, edge(i))
        for (i in steps downTo 0) {
            path.lineTo(size.width * i / steps, edge(i) + height * (0.7f + 0.3f * sin(i * 0.5f + t * 0.5f + ribbon)))
        }
        path.close()
        val strength = (0.5f + 0.25f * sin(t * 0.3f + ribbon * 1.7f)) * p.intensity
        drawPath(
            path,
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.25f to p.color(ribbon, 0.65f * strength),
                1f to Color.Transparent,
                startY = top - wave * 1.6f,
                endY = top + height + wave,
            ),
            blendMode = BlendMode.Screen,
        )
    }
}

private fun DrawScope.liquid(p: LayerPaint, m: BackgroundMotion, t: Float, path: Path) {
    val steps = (36 * p.budget.detail).toInt().coerceAtLeast(12)
    for (wave in 0 until 4) {
        val base = size.height * (0.45f + wave * 0.14f)
        val swing = size.height * (0.035f + 0.045f * m.bass) * p.intensity
        path.reset()
        path.moveTo(0f, size.height)
        for (i in 0..steps) {
            val x = i / steps.toFloat()
            val y = base +
                swing * sin(x * TAU * (0.8f + wave * 0.25f) + t * (0.7f - wave * 0.1f) + wave) +
                swing * 0.55f * sin(x * TAU * (1.9f + wave * 0.15f) - t * (0.5f + wave * 0.12f))
            path.lineTo(x * size.width, y)
        }
        path.lineTo(size.width, size.height)
        path.close()
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(p.color(wave, 0.6f), p.color(wave + 1, 0.25f)),
                startY = base - swing * 2f, endY = size.height,
            ),
        )
        // Little bubbles riding under the surface.
        for (i in 0 until p.count(5)) {
            val seed = wave * 31 + i
            val rise = wrap(noise(seed, 1) + t * (0.03f + noise(seed, 2) * 0.05f))
            val center = Offset(noise(seed, 3) * size.width, size.height - rise * (size.height - base))
            drawCircle(p.color(wave + 2, 0.35f * (1f - rise)), (2f + noise(seed, 4) * 6f).dp.toPx(), center)
        }
    }
}

/** The level at [position] 0..1 across the bands, smoothed between neighbours. */
private fun level(levels: FloatArray, position: Float): Float {
    val at = position.coerceIn(0f, 1f) * (levels.size - 1)
    val low = at.toInt()
    val high = min(low + 1, levels.size - 1)
    return levels[low] + (levels[high] - levels[low]) * (at - low)
}

private fun DrawScope.spectrum(p: LayerPaint, m: BackgroundMotion) {
    val bars = (48 * p.budget.detail).toInt().coerceAtLeast(16)
    val slot = size.width / bars
    val tallest = size.height * 0.42f * p.intensity
    for (i in 0 until bars) {
        val value = level(m.levels, i / (bars - 1f))
        val height = max(slot * 0.25f, value * tallest)
        val color = lerp(p.color(0), p.color(1), i / (bars - 1f)).copy(alpha = p.alpha)
        val left = i * slot + slot * 0.18f
        val width = slot * 0.64f
        val round = CornerRadius(width / 2, width / 2)
        if (p.glow > 0f) {
            drawRoundRect(
                color.copy(alpha = (0.18f * p.glow * p.alpha).coerceIn(0f, 1f)),
                Offset(left - width * 0.4f, size.height - height - width * 0.4f),
                Size(width * 1.8f, height + width * 0.8f), round,
            )
        }
        drawRoundRect(
            Brush.verticalGradient(
                listOf(color, color.copy(alpha = color.alpha * 0.15f)),
                startY = size.height - height, endY = size.height,
            ),
            Offset(left, size.height - height), Size(width, height), round,
        )
        // A faint reflection hanging from the top.
        drawRoundRect(
            color.copy(alpha = (0.18f * p.alpha).coerceIn(0f, 1f)),
            Offset(left, 0f), Size(width, height * 0.35f), round,
        )
    }
}

private fun DrawScope.waveform(p: LayerPaint, m: BackgroundMotion, t: Float, path: Path) {
    val points = m.wave.size
    for (line in 0 until 3) {
        val middle = size.height * (0.5f + (line - 1) * 0.07f)
        val reach = size.height * (0.22f - line * 0.04f) * p.intensity
        path.reset()
        for (i in 0 until points) {
            val x = i / (points - 1f)
            val envelope = sin(x * PI.toFloat())
            val sample = m.wave[(i + line * 5) % points]
            // A slow swell keeps the line alive in quiet passages.
            val y = middle + envelope * (sample * reach + sin(x * 9f + t * 1.2f + line * 2f) * reach * 0.1f)
            if (i == 0) path.moveTo(0f, y) else path.lineTo(x * size.width, y)
        }
        val width = (2.5f - line * 0.6f).dp.toPx()
        if (p.glow > 0f) {
            drawPath(path, p.color(line, 0.22f * p.glow), style = Stroke(width * 5f, cap = StrokeCap.Round))
        }
        drawPath(path, p.color(line, 1f - line * 0.2f), style = Stroke(width, cap = StrokeCap.Round))
    }
}

private fun DrawScope.geometric(p: LayerPaint, m: BackgroundMotion, t: Float, path: Path) {
    val unit = size.minDimension
    for (i in 0 until p.count(18)) {
        val sides = 3 + i % 4
        val center = Offset(
            wrap(noise(i, 1) + t * (noise(i, 2) - 0.5f) * 0.03f) * size.width,
            wrap(noise(i, 3) - t * (0.01f + noise(i, 4) * 0.025f)) * size.height,
        )
        val radius = unit * (0.03f + noise(i, 5) * 0.11f) * p.intensity * (1f + m.beat * 0.25f)
        val turn = t * (noise(i, 6) - 0.5f) * 0.8f + i
        path.reset()
        for (k in 0 until sides) {
            val angle = turn + k * TAU / sides
            val x = center.x + cos(angle) * radius
            val y = center.y + sin(angle) * radius
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (i % 3 == 0) drawPath(path, p.color(i, 0.16f))
        if (p.glow > 0f) drawPath(path, p.color(i, 0.18f * p.glow), style = Stroke(6.dp.toPx()))
        drawPath(path, p.color(i, 0.75f), style = Stroke(1.5.dp.toPx()))
    }
}

private fun DrawScope.cinematic(p: LayerPaint, m: BackgroundMotion, t: Float, path: Path) {
    // Slow beams from above.
    for (beam in 0 until 3) {
        val source = Offset(size.width * (0.2f + beam * 0.3f), -size.height * 0.08f)
        val sway = sin(t * (0.14f + beam * 0.05f) + beam * 2.1f) * size.width * 0.3f
        val spread = size.width * (0.12f + 0.04f * beam)
        val end = source.x + sway
        path.reset()
        path.moveTo(source.x - 6.dp.toPx(), source.y)
        path.lineTo(source.x + 6.dp.toPx(), source.y)
        path.lineTo(end + spread, size.height)
        path.lineTo(end - spread, size.height)
        path.close()
        val strength = (0.22f + 0.1f * m.bass + 0.12f * m.beat) * p.intensity
        drawPath(
            path,
            Brush.verticalGradient(listOf(p.color(beam, strength * (0.6f + p.glow * 0.4f)), Color.Transparent)),
            blendMode = BlendMode.Screen,
        )
    }
    // Dust drifting through the light.
    for (i in 0 until p.count(30)) {
        val center = Offset(
            wrap(noise(i, 1) + t * 0.008f * (noise(i, 2) - 0.3f)) * size.width,
            wrap(noise(i, 3) + t * 0.006f) * size.height,
        )
        drawCircle(Color.White.copy(alpha = (0.35f * noise(i, 4) * p.alpha).coerceIn(0f, 1f)), 1.2.dp.toPx(), center)
    }
    // Shadows closing in from the edges.
    drawRect(
        Brush.radialGradient(
            0.35f to Color.Transparent,
            1f to Color.Black.copy(alpha = (0.75f * p.intensity * p.alpha).coerceIn(0f, 1f)),
            center = center, radius = size.maxDimension * 0.7f,
        ),
    )
}

private fun DrawScope.album3d(p: LayerPaint, m: BackgroundMotion, t: Float, art: ArtworkImages) {
    val side = size.minDimension * (0.62f + 0.18f * p.intensity) * (1f + m.beat * 0.025f)
    val turn = sin(t * 0.35f) * 26f
    val tilt = cos(t * 0.27f) * 12f
    val middle = Offset(size.width / 2, size.height * 0.45f)
    glowSpot(middle, side * (0.95f + m.bass * 0.2f), p.color(0, 0.35f + 0.2f * p.glow))
    val camera = Camera()
    val matrix = Matrix()
    val corner = CornerRadius(side * 0.05f, side * 0.05f)
    // Back to front: a shadow, the thickness of the card, then the cover.
    for (depth in 4 downTo 0) {
        drawIntoCanvas { canvas ->
            camera.save()
            camera.translate(0f, 0f, depth * 14f)
            camera.rotateX(tilt)
            camera.rotateY(turn)
            camera.getMatrix(matrix)
            camera.restore()
            // Keeps the perspective the same on every screen density.
            val values = FloatArray(9)
            matrix.getValues(values)
            values[Matrix.MPERSP_0] /= density
            values[Matrix.MPERSP_1] /= density
            matrix.setValues(values)
            matrix.preTranslate(-middle.x, -middle.y)
            matrix.postTranslate(middle.x, middle.y)
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.concat(matrix)
        }
        val topLeft = Offset(middle.x - side / 2, middle.y - side / 2)
        val cardSize = Size(side, side)
        val image = art.sharp
        when {
            depth == 4 -> drawRoundRect(
                Color.Black.copy(alpha = 0.35f * p.alpha), topLeft + Offset(side * 0.04f, side * 0.07f), cardSize, corner,
            )
            depth > 0 -> drawRoundRect(
                lerp(p.color(depth), Color.Black, 0.45f).copy(alpha = p.alpha * 0.8f), topLeft, cardSize, corner,
            )
            image != null -> drawImage(
                image,
                dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()),
                dstSize = IntSize(side.roundToInt(), side.roundToInt()),
                alpha = p.alpha,
                colorFilter = p.filter,
            )
            else -> {
                drawRoundRect(
                    Brush.linearGradient(
                        listOf(p.color(0), p.color(1)), topLeft, topLeft + Offset(side, side),
                    ),
                    topLeft, cardSize, corner,
                )
                drawCircle(Color.White.copy(alpha = 0.5f * p.alpha), side * 0.22f, middle, style = Stroke(side * 0.03f))
                drawCircle(Color.White.copy(alpha = 0.5f * p.alpha), side * 0.05f, middle)
            }
        }
        drawIntoCanvas { it.nativeCanvas.restore() }
    }
}

private fun DrawScope.synthwave(p: LayerPaint, m: BackgroundMotion, t: Float, path: Path) {
    val horizon = size.height * 0.58f
    drawRect(
        Brush.verticalGradient(
            listOf(
                lerp(p.color(2), Color.Black, 0.75f).copy(alpha = p.alpha),
                lerp(p.color(0), Color.Black, 0.5f).copy(alpha = p.alpha),
            ),
            endY = horizon,
        ),
        size = Size(size.width, horizon),
    )
    for (i in 0 until p.count(40)) {
        val star = Offset(noise(i, 1) * size.width, noise(i, 2) * horizon * 0.75f)
        val light = 0.4f + 0.5f * sin(t * (0.5f + noise(i, 3)) + i)
        drawCircle(Color.White.copy(alpha = (light * p.alpha).coerceIn(0f, 1f)), 1.dp.toPx(), star)
    }
    // The striped sun, resting on the horizon.
    val radius = size.minDimension * 0.24f * (1f + m.bass * 0.08f)
    val sun = Offset(size.width / 2, horizon - radius * 0.55f)
    glowSpot(sun, radius * (1.6f + p.glow * 0.5f), p.color(0, 0.4f * p.glow + 0.1f))
    var top = sun.y - radius
    var band = 0
    while (top < horizon) {
        val lower = top > sun.y - radius * 0.1f
        val height = radius * if (lower) 0.16f else 1.1f
        val gap = if (lower) radius * (0.025f + 0.012f * band++) else 0f
        clipRect(top = top, bottom = min(horizon, top + height - gap)) {
            drawCircle(
                Brush.verticalGradient(
                    listOf(p.color(3), p.color(0)), startY = sun.y - radius, endY = sun.y + radius,
                ),
                radius, sun,
            )
        }
        top += height
    }
    // Ground and a grid running toward the viewer.
    drawRect(
        Brush.verticalGradient(
            listOf(
                lerp(p.color(2), Color.Black, 0.6f).copy(alpha = p.alpha),
                Color.Black.copy(alpha = p.alpha),
            ),
            startY = horizon, endY = size.height,
        ),
        topLeft = Offset(0f, horizon), size = Size(size.width, size.height - horizon),
    )
    val line = p.color(1, (0.8f * p.intensity).coerceAtMost(1f))
    val ground = size.height - horizon
    val rows = 12
    val shift = wrap(t * 0.55f)
    for (i in 0 until rows) {
        val near = (i + shift) / rows
        val y = horizon + ground * near * near
        drawLine(line.copy(alpha = line.alpha * (0.25f + 0.75f * near)), Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx())
    }
    for (i in -8..8) {
        drawLine(
            line.copy(alpha = line.alpha * 0.6f),
            Offset(size.width / 2 + i * size.width * 0.03f, horizon),
            Offset(size.width / 2 + i * size.width * 0.19f, size.height),
            1.5.dp.toPx(),
        )
    }
    // The skyline rises with the music.
    path.reset()
    path.moveTo(0f, horizon)
    val peaks = 24
    for (i in 0..peaks) {
        val x = i / peaks.toFloat()
        val away = kotlin.math.abs(x - 0.5f) * 2f
        val height = size.height * (0.02f + 0.09f * away * (noise(i, 9) + level(m.levels, away) * 0.8f))
        path.lineTo(x * size.width, horizon - height * if (away < 0.3f) 0.2f else 1f)
    }
    path.lineTo(size.width, horizon)
    drawPath(path, Color.Black.copy(alpha = 0.75f * p.alpha))
    drawPath(path, p.color(1, 0.7f), style = Stroke(1.dp.toPx()))
}
