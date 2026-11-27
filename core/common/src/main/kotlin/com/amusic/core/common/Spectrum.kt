package com.amusic.core.common

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Turns [SIZE] mono samples into an [AudioFrame]. Not thread safe. */
class SpectrumAnalyzer(sampleRate: Int) {
    private val window = FloatArray(SIZE) { (0.5 - 0.5 * cos(2 * PI * it / (SIZE - 1))).toFloat() }
    private val real = FloatArray(SIZE)
    private val imaginary = FloatArray(SIZE)
    private val cosines = FloatArray(SIZE / 2) { cos(-2 * PI * it / SIZE).toFloat() }
    private val sines = FloatArray(SIZE / 2) { sin(-2 * PI * it / SIZE).toFloat() }

    /** First FFT bin of each band, spaced logarithmically as hearing is. */
    private val edges: IntArray = run {
        val top = minOf(16_000.0, sampleRate / 2.0)
        val result = IntArray(AudioFrame.BANDS + 1)
        for (i in result.indices) {
            val hz = LOWEST_HZ * (top / LOWEST_HZ).pow(i.toDouble() / AudioFrame.BANDS)
            val bin = (hz * SIZE / sampleRate).toInt()
            result[i] = if (i == 0) maxOf(1, bin) else maxOf(result[i - 1] + 1, bin)
        }
        result
    }

    fun analyze(samples: FloatArray): AudioFrame {
        for (i in 0 until SIZE) {
            real[i] = samples[i] * window[i]
            imaginary[i] = 0f
        }
        transform()
        val bands = FloatArray(AudioFrame.BANDS) { band ->
            var peak = 0f
            for (bin in edges[band] until minOf(edges[band + 1], SIZE / 2)) {
                peak = maxOf(peak, sqrt(real[bin] * real[bin] + imaginary[bin] * imaginary[bin]))
            }
            // A full-scale tone under a Hann window peaks at SIZE / 4.
            val decibels = 20 * log10(peak / (SIZE / 4f) + 1e-9f)
            ((decibels + RANGE_DB) / RANGE_DB).coerceIn(0f, 1f)
        }
        val step = SIZE / AudioFrame.WAVE
        return AudioFrame(bands, FloatArray(AudioFrame.WAVE) { samples[it * step].coerceIn(-1f, 1f) })
    }

    /** In-place radix-2 FFT. */
    private fun transform() {
        var j = 0
        for (i in 1 until SIZE) {
            var bit = SIZE shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                real[i] = real[j].also { real[j] = real[i] }
                imaginary[i] = imaginary[j].also { imaginary[j] = imaginary[i] }
            }
        }
        var length = 2
        while (length <= SIZE) {
            val half = length / 2
            val stride = SIZE / length
            for (start in 0 until SIZE step length) {
                for (k in 0 until half) {
                    val a = start + k
                    val b = a + half
                    val c = cosines[k * stride]
                    val s = sines[k * stride]
                    val re = real[b] * c - imaginary[b] * s
                    val im = real[b] * s + imaginary[b] * c
                    real[b] = real[a] - re
                    imaginary[b] = imaginary[a] - im
                    real[a] += re
                    imaginary[a] += im
                }
            }
            length *= 2
        }
    }

    companion object {
        const val SIZE = 1024
        private const val LOWEST_HZ = 40.0
        private const val RANGE_DB = 60f
    }
}
