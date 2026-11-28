package com.amusic.core.theming

import com.amusic.core.common.AudioFrame
import com.amusic.core.common.BackgroundScene
import com.amusic.core.common.BackgroundStyle
import com.amusic.core.common.LookSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundMotionTest {
    private val scene = BackgroundScene().add(BackgroundStyle.MESH)
    private val loud = AudioFrame(FloatArray(AudioFrame.BANDS) { 0.8f }, FloatArray(AudioFrame.WAVE) { 0.5f })

    private fun BackgroundMotion.run(frames: Int, running: Boolean, of: BackgroundScene = scene) =
        repeat(frames) { advance(if (running) loud else AudioFrame.SILENCE, of, running, 0.033f) }

    @Test fun movesWithMusicAndComesToRestAfterIt() {
        val motion = BackgroundMotion()
        val start = motion.phase(BackgroundStyle.MESH)
        motion.run(60, running = true)
        assertTrue(motion.phase(BackgroundStyle.MESH) > start + 1f)
        assertTrue(motion.levels.all { it > 0.5f })
        assertFalse(motion.settled)

        motion.run(120, running = false)
        assertTrue(motion.settled)
        val rest = motion.phase(BackgroundStyle.MESH)
        motion.run(10, running = false)
        assertEquals(rest, motion.phase(BackgroundStyle.MESH))
    }

    @Test fun motionSettingAtZeroHoldsLayersStill() {
        val still = scene.copy(look = scene.look.with(LookSetting.MOTION, 0f))
        val motion = BackgroundMotion()
        val start = motion.phase(BackgroundStyle.MESH)
        motion.run(60, running = true, of = still)
        assertEquals(start, motion.phase(BackgroundStyle.MESH))
    }

    @Test fun bassSensitivityAtZeroSilencesThePulse() {
        val deaf = scene.copy(look = scene.look.with(LookSetting.BASS, 0f).with(LookSetting.BEAT, 0f))
        val motion = BackgroundMotion()
        motion.run(30, running = true, of = deaf)
        assertEquals(0f, motion.bass)
        assertEquals(0f, motion.beat)
    }
}
