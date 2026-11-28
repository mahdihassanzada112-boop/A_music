package com.amusic.core.common

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/** One second-order filter (formulas from the Audio EQ Cookbook). */
internal class Biquad {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    fun flat() = set(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

    fun peaking(frequency: Double, sampleRate: Int, gainDb: Double, q: Double) {
        val a = 10.0.pow(gainDb / 40)
        val w = 2 * PI * frequency / sampleRate
        val alpha = sin(w) / (2 * q)
        set(1 + alpha * a, -2 * cos(w), 1 - alpha * a, 1 + alpha / a, -2 * cos(w), 1 - alpha / a)
    }

    fun lowShelf(frequency: Double, sampleRate: Int, gainDb: Double) {
        val a = 10.0.pow(gainDb / 40)
        val w = 2 * PI * frequency / sampleRate
        val c = cos(w)
        val beta = 2 * sqrt(a) * (sin(w) / 2 * sqrt(2.0))
        set(
            a * ((a + 1) - (a - 1) * c + beta),
            2 * a * ((a - 1) - (a + 1) * c),
            a * ((a + 1) - (a - 1) * c - beta),
            (a + 1) + (a - 1) * c + beta,
            -2 * ((a - 1) + (a + 1) * c),
            (a + 1) + (a - 1) * c - beta,
        )
    }

    private fun set(nb0: Double, nb1: Double, nb2: Double, na0: Double, na1: Double, na2: Double) {
        b0 = nb0 / na0
        b1 = nb1 / na0
        b2 = nb2 / na0
        a1 = na1 / na0
        a2 = na2 / na0
    }

    /** [state] holds this filter's two memories for one channel, starting at [at]. */
    fun process(input: Double, state: DoubleArray, at: Int): Double {
        val output = b0 * input + state[at]
        state[at] = b1 * input - a1 * output + state[at + 1]
        state[at + 1] = b2 * input - a2 * output
        return output
    }

    /** How much a tone at [w] radians per sample is amplified, in dB. */
    fun responseDb(w: Double): Double {
        fun power(c0: Double, c1: Double, c2: Double) =
            c0 * c0 + c1 * c1 + c2 * c2 + 2 * (c0 * c1 + c1 * c2) * cos(w) + 2 * c0 * c2 * cos(2 * w)
        return 10 * log10(power(b0, b1, b2) / power(1.0, a1, a2))
    }
}

/**
 * The app's own equalizer: ten bands, a bass shelf and stereo widening, on interleaved samples
 * in -1..1. It sounds the same on every device. Boosts are balanced by lowering the overall
 * level, so they do not distort. Changes glide in over a fraction of a second.
 *
 * Not thread safe: [target] may be called from anywhere, everything else from one thread.
 */
class EqualizerDsp(private val sampleRate: Int, private val channels: Int) {
    private class Target(val bands: FloatArray, val bass: Float, val width: Float)

    @Volatile private var wanted = flatTarget()

    // What is being applied now, moving toward [wanted].
    private val bands = FloatArray(EQ_FREQUENCIES.size)
    private var bass = 0f
    private var width = 0f
    private var preamp = 1.0
    private var moving = false

    private val filters = List(EQ_FREQUENCIES.size + 1) { Biquad() }
    private val state = DoubleArray(channels * filters.size * 2)
    private var untilUpdate = 0

    /** True while nothing is changed, so samples can be passed through untouched. */
    val bypassed: Boolean
        get() = !moving && wanted.isFlat() && bass == 0f && width == 0f && bands.all { it == 0f }

    fun target(settings: EqualizerSettings, active: Boolean) {
        val safe = settings.clamped()
        wanted = if (active) Target(safe.bands.toFloatArray(), safe.bass, safe.width) else flatTarget()
    }

    /** Jumps straight to the target, for a new stream. */
    fun reset() {
        state.fill(0.0)
        val goal = wanted
        goal.bands.copyInto(bands)
        bass = goal.bass
        width = goal.width
        moving = false
        design()
        untilUpdate = BLOCK
    }

    /** Processes [count] samples of whole frames in place. */
    fun process(samples: FloatArray, count: Int) {
        var frame = 0
        while (frame < count) {
            if (untilUpdate <= 0) {
                glide()
                untilUpdate = BLOCK
            }
            for (channel in 0 until channels) {
                var value = samples[frame + channel].toDouble() * preamp
                for (i in filters.indices) {
                    value = filters[i].process(value, state, (channel * filters.size + i) * 2)
                }
                samples[frame + channel] = value.toFloat()
            }
            if (channels == 2 && width > 0f) {
                val mid = (samples[frame] + samples[frame + 1]) / 2
                val side = (samples[frame] - samples[frame + 1]) / 2 * (1 + width)
                samples[frame] = mid + side
                samples[frame + 1] = mid - side
            }
            for (channel in 0 until channels) samples[frame + channel] = soften(samples[frame + channel])
            frame += channels
            untilUpdate--
        }
    }

    private fun glide() {
        val goal = wanted
        fun step(from: Float, to: Float, size: Float) =
            if (abs(to - from) <= size) to else from + if (to > from) size else -size
        var changed = false
        for (i in bands.indices) {
            val next = step(bands[i], goal.bands[i], 0.5f)
            if (next != bands[i]) changed = true
            bands[i] = next
        }
        val nextBass = step(bass, goal.bass, 0.04f)
        val nextWidth = step(width, goal.width, 0.04f)
        if (nextBass != bass) changed = true
        bass = nextBass
        width = nextWidth
        moving = changed
        if (changed) design()
    }

    private fun design() {
        EQ_FREQUENCIES.forEachIndexed { i, frequency ->
            // A band too close to half the sample rate cannot be built.
            if (bands[i] == 0f || frequency > sampleRate * 0.45) filters[i].flat()
            else filters[i].peaking(frequency.toDouble(), sampleRate, bands[i].toDouble(), Q)
        }
        val shelf = filters.last()
        if (bass == 0f) shelf.flat() else shelf.lowShelf(BASS_HZ, sampleRate, (bass * BASS_DB).toDouble())
        preamp = 10.0.pow(-max(0.0, peakDb()) / 20)
    }

    /** The largest amplification of any frequency before the level is lowered. */
    private fun peakDb(): Double {
        var peak = 0.0
        val top = min(20_000.0, sampleRate * 0.49)
        for (step in 0..POINTS) {
            val frequency = 20.0 * (top / 20).pow(step / POINTS.toDouble())
            val w = 2 * PI * frequency / sampleRate
            peak = max(peak, filters.sumOf { it.responseDb(w) })
        }
        return peak
    }

    /** Rounds off the rare peak instead of cutting it flat. */
    private fun soften(value: Float): Float {
        val size = abs(value)
        if (size <= KNEE) return value
        val rounded = KNEE + (1 - KNEE) * tanh((size - KNEE) / (1 - KNEE))
        return if (value < 0) -rounded else rounded
    }

    private fun Target.isFlat() = bass == 0f && width == 0f && bands.all { it == 0f }

    private fun flatTarget() = Target(FloatArray(EQ_FREQUENCIES.size), 0f, 0f)

    private companion object {
        const val BLOCK = 256
        const val Q = 1.41
        const val BASS_HZ = 120.0
        const val BASS_DB = 10f
        const val POINTS = 96
        const val KNEE = 0.95f
    }
}
