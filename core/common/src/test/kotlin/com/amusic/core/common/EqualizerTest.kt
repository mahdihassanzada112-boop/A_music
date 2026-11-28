package com.amusic.core.common

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerTest {
    private val rate = 48_000

    private fun tone(frequency: Int, level: Float = 0.2f, frames: Int = rate) =
        FloatArray(frames * 2) { (sin(2 * PI * frequency * (it / 2) / rate) * level).toFloat() }

    /** Level change in dB of a tone, measured after the filter has settled. */
    private fun gain(settings: EqualizerSettings, frequency: Int, level: Float = 0.2f): Double {
        val dsp = EqualizerDsp(rate, 2)
        dsp.target(settings, active = true)
        dsp.reset()
        val samples = tone(frequency, level)
        val before = rms(samples)
        dsp.process(samples, samples.size)
        return 20 * log10(rms(samples) / before)
    }

    private fun rms(samples: FloatArray): Double {
        val from = samples.size / 2
        var sum = 0.0
        for (i in from until samples.size) sum += samples[i] * samples[i]
        return sqrt(sum / (samples.size - from))
    }

    @Test fun flatSettingsLeaveSoundAlone() {
        val dsp = EqualizerDsp(rate, 2)
        dsp.target(EqualizerSettings(), active = true)
        dsp.reset()
        assertTrue(dsp.bypassed)
        val samples = tone(1_000, frames = 500)
        val copy = samples.copyOf()
        dsp.process(samples, samples.size)
        assertTrue(samples.indices.all { abs(samples[it] - copy[it]) < 1e-5f })
    }

    @Test fun aBandMovesItsOwnFrequency() {
        val cut = EqualizerSettings().withBand(5, -12f)
        assertEquals(-12.0, gain(cut, 1_000), 0.5)
        assertEquals(0.0, gain(cut, 62), 0.5)
        assertEquals(0.0, gain(cut, 12_000), 0.5)
    }

    @Test fun boostsRaiseABandAgainstTheRestWithoutDistorting() {
        val boost = EqualizerSettings().withBand(5, 12f)
        // The whole level drops so the boosted band stays where it was.
        assertEquals(0.0, gain(boost, 1_000, 0.9f), 0.6)
        assertEquals(-12.0, gain(boost, 62), 0.8)
    }

    @Test fun bassShelfLiftsLowTonesOnly() {
        val bass = EqualizerSettings(bass = 1f)
        assertTrue(gain(bass, 40) - gain(bass, 4_000) > 8.0)
    }

    @Test fun wideningSpreadsStereoButNotMono() {
        val dsp = EqualizerDsp(rate, 2)
        dsp.target(EqualizerSettings(width = 1f), active = true)
        dsp.reset()
        val mono = floatArrayOf(0.2f, 0.2f)
        dsp.process(mono, 2)
        assertEquals(mono[0], mono[1])
        val stereo = floatArrayOf(0.3f, 0.1f)
        dsp.process(stereo, 2)
        assertEquals(0.4f, stereo[0], 1e-4f)
        assertEquals(0.0f, stereo[1], 1e-4f)
    }

    @Test fun outputNeverLeavesTheRange() {
        val dsp = EqualizerDsp(rate, 2)
        dsp.target(EqPreset.ROCK.let { EqualizerSettings().choose(it).copy(bass = 1f, width = 1f) }, true)
        dsp.reset()
        val samples = FloatArray(rate) { if (it % 3 == 0) 1f else -1f }
        dsp.process(samples, samples.size)
        assertTrue(samples.all { abs(it) <= 1f })
    }

    @Test fun changesGlideAndSwitchingOffReturnsToBypass() {
        val dsp = EqualizerDsp(rate, 2)
        dsp.reset()
        val settings = EqualizerSettings().choose(EqPreset.ELECTRONIC).copy(bass = 0.5f)
        dsp.target(settings, active = true)
        dsp.process(tone(500), rate * 2)
        assertTrue(!dsp.bypassed)
        dsp.target(settings, active = false)
        repeat(2) { dsp.process(tone(500), rate * 2) }
        assertTrue(dsp.bypassed)
    }

    @Test fun lowSampleRatesStayStable() {
        val dsp = EqualizerDsp(8_000, 1)
        dsp.target(EqualizerSettings().choose(EqPreset.ROCK), active = true)
        dsp.reset()
        val samples = FloatArray(8_000) { (sin(it * 0.3) * 0.5).toFloat() }
        dsp.process(samples, samples.size)
        assertTrue(samples.all { it.isFinite() && abs(it) <= 1f })
    }

    @Test fun presetsAndBandsBehave() {
        assertEquals(9, EqPreset.entries.size)
        assertTrue(EqPreset.entries.all { it.bands.size == EQ_FREQUENCIES.size })
        val changed = EqualizerSettings().choose(EqPreset.JAZZ).withBand(0, 99f)
        assertEquals(EqPreset.CUSTOM, changed.preset)
        assertEquals(EQ_LIMIT, changed.bands[0])
        assertEquals(EqPreset.JAZZ.bands[1], changed.bands[1])
        assertEquals(changed.bands, changed.choose(EqPreset.CUSTOM).bands)
        assertEquals(changed.bands, decodeBands(encodeBands(changed.bands)))
        assertNull(decodeBands("1,2"))
        assertNull(decodeBands("a,b,c,d,e,f,g,h,i,j"))
    }
}
