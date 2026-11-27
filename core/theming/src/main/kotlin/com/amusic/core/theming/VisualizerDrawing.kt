package com.amusic.core.theming

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.amusic.core.common.VisualizerColor
import com.amusic.core.common.VisualizerSettings
import com.amusic.core.common.VisualizerStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val TURN = (2 * PI).toFloat()

internal fun DrawScope.drawVisualizer(motion: VisualizerMotion, settings: VisualizerSettings, accent: Color) {
    val paint = Paint(settings, accent)
    when (settings.style) {
        VisualizerStyle.BARS -> bars(motion, paint)
        VisualizerStyle.CIRCULAR -> circular(motion, paint)
        VisualizerStyle.WAVEFORM -> waveform(motion, paint)
        VisualizerStyle.NEON -> neon(motion, paint)
        VisualizerStyle.PARTICLES -> particles(motion, paint)
        VisualizerStyle.FLUID -> fluid(motion, paint)
        VisualizerStyle.DEPTH -> depth(motion, paint)
        VisualizerStyle.RETRO -> retro(motion, paint)
        VisualizerStyle.MINIMAL -> minimal(motion, paint)
    }
}

private class Paint(val settings: VisualizerSettings, private val accent: Color) {
    /** The color at [place], 0 to 1 across the drawing. */
    fun at(place: Float): Color = when (settings.color) {
        VisualizerColor.THEME -> accent
        VisualizerColor.AMBER -> Color(0xFFFFA726)
        VisualizerColor.CYAN -> Color(0xFF00BCD4)
        VisualizerColor.MAGENTA -> Color(0xFFE040A0)
        VisualizerColor.GREEN -> Color(0xFF43C463)
        VisualizerColor.RAINBOW -> Color.hsv(place.coerceIn(0f, 1f) * 300f, 0.75f, 0.95f)
    }

    /** A level after the intensity setting. */
    fun scale(level: Float) = (level * settings.intensity).coerceIn(0f, 1f)

    fun alpha(base: Float) = (base * settings.intensity).coerceIn(0f, 1f)
}

private fun DrawScope.bars(motion: VisualizerMotion, paint: Paint) {
    val count = motion.levels.size
    val slot = size.width / count
    val thickness = slot * 0.68f
    for (i in 0 until count) {
        val height = maxOf(thickness / 2, size.height * 0.92f * paint.scale(motion.levels[i]))
        drawRoundRect(
            paint.at(i / (count - 1f)),
            Offset(i * slot + (slot - thickness) / 2, size.height - height),
            Size(thickness, height),
            CornerRadius(thickness / 2),
        )
    }
}

private fun DrawScope.circular(motion: VisualizerMotion, paint: Paint) {
    val radius = size.minDimension * 0.26f
    val reach = size.minDimension * 0.22f
    val count = motion.levels.size * 2
    for (i in 0 until count) {
        // Mirrored, so the ring joins without a seam.
        val band = if (i < motion.levels.size) i else count - 1 - i
        val angle = TURN * i / count - TURN / 4
        val direction = Offset(cos(angle), sin(angle))
        drawLine(
            paint.at(band / (motion.levels.size - 1f)),
            center + direction * radius,
            center + direction * (radius + 2.dp.toPx() + reach * paint.scale(motion.levels[band])),
            strokeWidth = TURN * radius / count * 0.55f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.waveform(motion: VisualizerMotion, paint: Paint) {
    val path = Path()
    val last = motion.wave.size - 1
    for (i in 0..last) {
        val x = size.width * i / last
        val y = center.y - motion.wave[i] * paint.settings.intensity * size.height * 0.46f
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, paint.at(0.55f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
}

private fun average(values: FloatArray, from: Int, to: Int): Float {
    var sum = 0f
    for (i in from until to) sum += values[i]
    return sum / (to - from)
}

private fun DrawScope.neon(motion: VisualizerMotion, paint: Paint) {
    val third = motion.levels.size / 3
    for (layer in 0 until 3) {
        val level = paint.scale(average(motion.levels, layer * third, (layer + 1) * third))
        val path = Path()
        for (step in 0..60) {
            val t = step / 60f
            // Pinned at both ends.
            val envelope = sin(t * PI.toFloat())
            val y = center.y + sin(t * TURN * (layer + 1.5f) + motion.phase * (layer + 2)) *
                envelope * size.height * (0.02f + 0.4f * level)
            if (step == 0) path.moveTo(0f, y) else path.lineTo(t * size.width, y)
        }
        val color = paint.at(layer / 2f)
        drawPath(path, color.copy(alpha = 0.22f), style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun DrawScope.particles(motion: VisualizerMotion, paint: Paint) {
    for (particle in motion.particles) {
        val level = paint.scale(motion.levels[particle.band])
        val distance = size.minDimension * (0.06f + 0.44f * particle.distance)
        val angle = particle.angle + motion.phase * 0.1f
        drawCircle(
            paint.at(particle.band / (motion.levels.size - 1f)),
            radius = 2.dp.toPx() + 7.dp.toPx() * level,
            center = center + Offset(cos(angle), sin(angle)) * distance,
            alpha = paint.alpha((0.25f + 0.75f * level) * (1f - particle.distance * 0.7f)),
        )
    }
}

private fun DrawScope.fluid(motion: VisualizerMotion, paint: Paint) {
    val points = 12
    val perPoint = motion.levels.size.toFloat() / points
    for (layer in 0 until 3) {
        val path = Path()
        path.moveTo(0f, size.height)
        var previous = Offset.Zero
        for (i in 0..points) {
            val band = min((i * perPoint).toInt(), motion.levels.size - 1)
            val swell = sin(motion.phase * (1f + layer * 0.4f) + i * 0.8f + layer * 2f) * 0.04f
            val height = 0.12f + paint.scale(motion.levels[band]) * (0.75f - layer * 0.18f) + swell
            val point = Offset(size.width * i / points, size.height * (1f - height.coerceIn(0.02f, 0.96f)))
            if (i == 0) {
                path.lineTo(point.x, point.y)
            } else {
                val middle = (previous.x + point.x) / 2
                path.cubicTo(middle, previous.y, middle, point.y, point.x, point.y)
            }
            previous = point
        }
        path.lineTo(size.width, size.height)
        path.close()
        drawPath(path, paint.at(layer / 2f), alpha = paint.alpha(0.35f + layer * 0.12f))
    }
}

/** A perspective view: older moments recede towards the back. */
private fun DrawScope.depth(motion: VisualizerMotion, paint: Paint) {
    val rows = 9
    for (row in motion.history.indices.reversed()) {
        val levels = motion.history[row]
        val near = 1f - row / rows.toFloat()
        val width = size.width * (0.45f + 0.55f * near)
        val base = size.height * (0.38f + 0.6f * near)
        val slot = width / (levels.size / 2)
        for (i in 0 until levels.size / 2) {
            val level = paint.scale((levels[i * 2] + levels[i * 2 + 1]) / 2)
            val height = 2.dp.toPx() + size.height * 0.34f * near * level
            drawRect(
                paint.at(i / (levels.size / 2 - 1f)),
                Offset((size.width - width) / 2 + i * slot + slot * 0.15f, base - height),
                Size(slot * 0.7f, height),
                alpha = paint.alpha(0.15f + 0.85f * near * near),
            )
        }
    }
}

/** Segmented meters with falling peak marks. */
private fun DrawScope.retro(motion: VisualizerMotion, paint: Paint) {
    val columns = 16
    val segments = 14
    val width = size.width / columns
    val height = size.height / segments
    for (column in 0 until columns) {
        val lit = (paint.scale(average(motion.levels, column * 2, column * 2 + 2)) * segments).toInt()
        val peak = (paint.scale(maxOf(motion.peaks[column * 2], motion.peaks[column * 2 + 1])) * segments)
            .toInt().coerceAtMost(segments - 1)
        for (segment in 0 until segments) {
            val color = if (paint.settings.color != VisualizerColor.THEME) {
                paint.at(column / (columns - 1f))
            } else {
                // The classic meter colors.
                when {
                    segment >= segments - 2 -> Color(0xFFFF5252)
                    segment >= segments - 5 -> Color(0xFFFFC107)
                    else -> Color(0xFF4CD964)
                }
            }
            val on = segment < lit || (peak > 0 && segment == peak)
            drawRect(
                color,
                Offset(column * width + width * 0.1f, size.height - (segment + 1) * height + height * 0.15f),
                Size(width * 0.8f, height * 0.7f),
                alpha = if (on) 1f else 0.12f,
            )
        }
    }
}

private fun DrawScope.minimal(motion: VisualizerMotion, paint: Paint) {
    val count = 5
    val thickness = 6.dp.toPx()
    val gap = thickness * 2.2f
    val start = center.x - gap * (count - 1) / 2
    val perBar = motion.levels.size / count
    for (i in 0 until count) {
        val height = thickness + size.height * 0.4f * paint.scale(average(motion.levels, i * perBar, (i + 1) * perBar))
        drawLine(
            paint.at(i / (count - 1f)),
            Offset(start + i * gap, center.y - height / 2),
            Offset(start + i * gap, center.y + height / 2),
            strokeWidth = thickness,
            cap = StrokeCap.Round,
        )
    }
}
