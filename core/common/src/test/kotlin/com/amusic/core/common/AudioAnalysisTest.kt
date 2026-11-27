package com.amusic.core.common

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAnalysisTest {
    private fun tone(hz: Double, amplitude: Float = 0.8f) = FloatArray(SpectrumAnalyzer.SIZE) {
        (amplitude * sin(2 * PI * hz * it / 44_100)).toFloat()
    }

    private fun loudest(frame: AudioFrame) = frame.bands.indices.maxBy { frame.bands[it] }

    @Test fun higherTonesLightHigherBands() {
        val analyzer = SpectrumAnalyzer(44_100)
        val low = analyzer.analyze(tone(110.0))
        val high = analyzer.analyze(tone(6_000.0))
        assertTrue(loudest(low) < 10)
        assertTrue(loudest(high) > 22)
        assertTrue(low.bands[loudest(low)] > 0.8f)
        assertEquals(0f, high.bands[0], 0.05f)
    }

    @Test fun silenceIsFlat() {
        val frame = SpectrumAnalyzer(48_000).analyze(FloatArray(SpectrumAnalyzer.SIZE))
        assertTrue(frame.bands.all { it == 0f })
        assertTrue(frame.wave.all { it == 0f })
    }

    @Test fun worksAtLowSampleRates() {
        val frame = SpectrumAnalyzer(8_000).analyze(tone(440.0))
        assertEquals(AudioFrame.BANDS, frame.bands.size)
    }

    @Test fun itemsAppearWhenAudible() {
        val queue = PlayoutQueue<String>()
        queue.add("a", 100, nowUs = 0)
        queue.add("b", 100, nowUs = 0)
        queue.add("c", 100, nowUs = 0)
        assertEquals("a", queue.poll(0))
        assertEquals("a", queue.poll(99))
        assertEquals("b", queue.poll(150))
        assertEquals("c", queue.poll(1_000))
    }

    @Test fun pauseHoldsBufferedItemsBack() {
        val queue = PlayoutQueue<String>()
        queue.add("a", 100, nowUs = 0)
        queue.add("b", 100, nowUs = 0)
        queue.setPlaying(false, nowUs = 50)
        assertEquals("a", queue.poll(5_000))
        queue.setPlaying(true, nowUs = 1_050)
        assertEquals("a", queue.poll(1_090))
        assertEquals("b", queue.poll(1_100))
    }

    @Test fun clearForgetsEverything() {
        val queue = PlayoutQueue<String>()
        queue.add("a", 100, nowUs = 0)
        queue.poll(0)
        queue.clear()
        assertNull(queue.poll(1_000))
    }
}
