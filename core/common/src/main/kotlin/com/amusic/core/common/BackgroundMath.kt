package com.amusic.core.common

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** How much work the background may do. */
data class RenderBudget(val fps: Int, val detail: Float, val glow: Boolean)

fun renderBudget(fpsLimit: Int, batterySaver: Boolean, lowEnd: Boolean): RenderBudget {
    var fps = fpsLimit.coerceIn(10, 60)
    var detail = 1f
    if (lowEnd) {
        fps = min(fps, 30)
        detail = 0.6f
    }
    if (batterySaver) {
        fps = min(fps, 20)
        detail = min(detail, 0.5f)
    }
    return RenderBudget(fps, detail, glow = !batterySaver && !lowEnd)
}

/** Bass level and beat pulses of the playing audio, both in 0..1. */
class BeatTracker {
    private var average = 0f
    private var sinceBeat = 1f

    var bass = 0f
        private set

    /** Jumps on a beat and dies away quickly. */
    var beat = 0f
        private set

    fun advance(bands: FloatArray, seconds: Float) {
        var low = 0f
        val count = min(LOW_BANDS, bands.size)
        for (i in 0 until count) low += bands[i]
        low = if (count == 0) 0f else low / count
        bass = if (low > bass) bass + (low - bass) * min(1f, seconds * 20f) else max(low, bass - seconds * 1.5f)
        sinceBeat += seconds
        beat = max(0f, beat - seconds * 3.5f)
        if (low > 0.2f && low > average * 1.25f + 0.04f && sinceBeat > 0.22f) {
            beat = 1f
            sinceBeat = 0f
        }
        average += (low - average) * min(1f, seconds * 4f)
    }

    private companion object {
        const val LOW_BANDS = 6
    }
}

/**
 * A 4x5 color matrix, row by row, for brightness, saturation and contrast. The last column is
 * in 0..255, as Android expects.
 */
fun colorMatrix(brightness: Float, saturation: Float, contrast: Float): FloatArray {
    val luma = floatArrayOf(0.2126f, 0.7152f, 0.0722f)
    val gain = brightness * contrast
    val shift = 127.5f * (1f - contrast) * brightness
    val matrix = FloatArray(20)
    for (row in 0..2) {
        for (column in 0..2) {
            val own = if (row == column) saturation else 0f
            matrix[row * 5 + column] = (luma[column] * (1f - saturation) + own) * gain
        }
        matrix[row * 5 + 4] = shift
    }
    matrix[18] = 1f
    return matrix
}

/** [argb] seen through a [colorMatrix]; transparency is unchanged. */
fun applyColorMatrix(argb: Int, matrix: FloatArray): Int {
    val r = (argb shr 16 and 0xFF).toFloat()
    val g = (argb shr 8 and 0xFF).toFloat()
    val b = (argb and 0xFF).toFloat()
    fun channel(row: Int) = (matrix[row] * r + matrix[row + 1] * g + matrix[row + 2] * b + matrix[row + 4])
        .toInt().coerceIn(0, 255)
    return (argb and 0xFF000000.toInt()) or (channel(0) shl 16) or (channel(5) shl 8) or channel(10)
}

/**
 * Up to [count] clearly different colors that stand out in a picture, most prominent first.
 * Vivid colors count for more than grey, black and white. Empty input gives an empty list.
 */
fun dominantColors(pixels: IntArray, count: Int = 4): List<Int> {
    class Bucket(var weight: Float = 0f, var r: Long = 0, var g: Long = 0, var b: Long = 0, var n: Int = 0)

    val buckets = HashMap<Int, Bucket>()
    for (pixel in pixels) {
        if (pixel ushr 24 < 128) continue
        val r = pixel shr 16 and 0xFF
        val g = pixel shr 8 and 0xFF
        val b = pixel and 0xFF
        val high = maxOf(r, g, b)
        val saturation = if (high == 0) 0f else (high - minOf(r, g, b)) / high.toFloat()
        val bucket = buckets.getOrPut((r shr 4 shl 8) or (g shr 4 shl 4) or (b shr 4)) { Bucket() }
        bucket.weight += 0.15f + saturation * (high / 255f)
        bucket.r += r.toLong()
        bucket.g += g.toLong()
        bucket.b += b.toLong()
        bucket.n++
    }
    val chosen = mutableListOf<IntArray>()
    for (bucket in buckets.values.sortedByDescending { it.weight }) {
        val color = intArrayOf((bucket.r / bucket.n).toInt(), (bucket.g / bucket.n).toInt(), (bucket.b / bucket.n).toInt())
        if (chosen.all { other -> abs(other[0] - color[0]) + abs(other[1] - color[1]) + abs(other[2] - color[2]) > 70 }) {
            chosen += color
            if (chosen.size == count) break
        }
    }
    return chosen.map { 0xFF000000.toInt() or (it[0] shl 16) or (it[1] shl 8) or it[2] }
}

/**
 * A soft blur: three box passes in each direction, which looks close to Gaussian. Meant for a
 * small picture that is then stretched. Returns a new array; the result is opaque.
 */
fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
    require(pixels.size == width * height)
    var source = pixels.copyOf()
    if (radius <= 0) return source
    var target = IntArray(source.size)
    repeat(3) {
        blurLines(source, target, lines = height, length = width, step = 1, lineStep = width, radius = radius)
        blurLines(target, source, lines = width, length = height, step = width, lineStep = 1, radius = radius)
    }
    return source
}

private fun blurLines(
    from: IntArray,
    to: IntArray,
    lines: Int,
    length: Int,
    step: Int,
    lineStep: Int,
    radius: Int,
) {
    val size = radius * 2 + 1
    for (line in 0 until lines) {
        val start = line * lineStep
        var r = 0
        var g = 0
        var b = 0
        fun add(position: Int, sign: Int) {
            val pixel = from[start + position.coerceIn(0, length - 1) * step]
            r += sign * (pixel shr 16 and 0xFF)
            g += sign * (pixel shr 8 and 0xFF)
            b += sign * (pixel and 0xFF)
        }
        for (i in -radius..radius) add(i, 1)
        for (i in 0 until length) {
            to[start + i * step] = 0xFF000000.toInt() or (r / size shl 16) or (g / size shl 8) or (b / size)
            add(i + radius + 1, 1)
            add(i - radius, -1)
        }
    }
}
