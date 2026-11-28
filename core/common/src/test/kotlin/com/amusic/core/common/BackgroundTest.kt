package com.amusic.core.common

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test fun allFourteenStylesExist() {
        assertEquals(14, BackgroundStyle.entries.size)
    }

    @Test fun layersAreAddedOnceUpToTheLimit() {
        var scene = BackgroundScene()
        scene = scene.add(BackgroundStyle.BLUR).add(BackgroundStyle.BLUR)
        assertEquals(1, scene.layers.size)
        BackgroundStyle.entries.forEach { scene = scene.add(it) }
        assertEquals(BackgroundScene.MAX_LAYERS, scene.layers.size)
        assertTrue(!scene.canAdd)
        assertEquals(BackgroundScene.MAX_LAYERS - 1, scene.remove(BackgroundStyle.BLUR).layers.size)
    }

    @Test fun layersMoveButNotPastTheEnds() {
        val scene = BackgroundScene().add(BackgroundStyle.BLUR).add(BackgroundStyle.MESH)
        val moved = scene.move(BackgroundStyle.BLUR, up = true)
        assertEquals(listOf(BackgroundStyle.MESH, BackgroundStyle.BLUR), moved.layers.map { it.style })
        assertEquals(moved, moved.move(BackgroundStyle.BLUR, up = true))
        assertEquals(scene, scene.move(BackgroundStyle.BLUR, up = false))
        assertEquals(scene, scene.move(BackgroundStyle.AURORA, up = true))
    }

    @Test fun valuesStayInRange() {
        val look = BackgroundLook().with(LookSetting.BLUR, 9f).with(LookSetting.SPEED, Float.NaN)
        assertEquals(1f, look[LookSetting.BLUR])
        assertEquals(1f, look[LookSetting.SPEED])
        assertEquals(BackgroundLook(), look.with(LookSetting.BLUR, LookSetting.BLUR.default))
        val layer = BackgroundLayer(BackgroundStyle.MESH, opacity = -1f, speed = 100f, color = 0x123456).clamped()
        assertEquals(0.05f, layer.opacity)
        assertEquals(2.5f, layer.speed)
        assertEquals(0xFF123456.toInt(), layer.color)
    }

    @Test fun scenesSurviveStorage() {
        val scene = BackgroundScene(
            listOf(
                BackgroundLayer(BackgroundStyle.SOLID, color = red, palette = BackgroundPalette.CUSTOM),
                BackgroundLayer(BackgroundStyle.AURORA, enabled = false, opacity = 0.3f, intensity = 1.2f, speed = 2f),
            ),
            BackgroundLook().with(LookSetting.GLOW, 0.25f).with(LookSetting.CONTRAST, 1.4f),
            BackgroundPalette.NEON,
        )
        assertEquals(scene, BackgroundCodec.decode(BackgroundCodec.encode(scene)))
        BuiltInPreset.entries.forEach {
            assertEquals(it.scene, BackgroundCodec.decode(BackgroundCodec.encode(it.scene)))
        }
    }

    @Test fun damagedStorageIsForgiven() {
        assertNull(BackgroundCodec.decode(null))
        assertNull(BackgroundCodec.decode("nonsense"))
        val scene = BackgroundCodec.decode(
            "1;palette=GONE;layer=UNKNOWN,1;layer=BLUR,x;layer=BLUR,0;BLUR=abc;NEW=3;;SPEED=1.5",
        )!!
        assertEquals(listOf(BackgroundStyle.BLUR), scene.layers.map { it.style })
        assertEquals(BackgroundPalette.ARTWORK, scene.palette)
        assertEquals(1.5f, scene.look[LookSetting.SPEED])
        assertEquals(BackgroundScene(), BackgroundCodec.decode("1"))
    }

    @Test fun autoChangePicksAnotherScene() {
        val pool = listOf(BuiltInPreset.CALM.scene, BuiltInPreset.GALLERY.scene, BuiltInPreset.SHAPES.scene)
        assertEquals(pool[1], nextScene(pool, pool[0], AutoBackground.IN_ORDER))
        assertEquals(pool[0], nextScene(pool, pool[2], AutoBackground.IN_ORDER))
        assertEquals(pool[0], nextScene(pool, BackgroundScene(), AutoBackground.IN_ORDER))
        assertEquals(pool[0], nextScene(pool, pool[0], AutoBackground.OFF))
        val random = Random(1)
        repeat(30) { assertNotEquals(pool[0], nextScene(pool, pool[0], AutoBackground.SHUFFLE, random)) }
        assertEquals(pool[0], nextScene(listOf(pool[0]), pool[0], AutoBackground.SHUFFLE))
    }

    @Test fun savedPresetsRotateOnceThereAreTwo() {
        val mine = listOf(BackgroundScene(), BackgroundScene.Default)
        assertEquals(mine, rotationPool(mine))
        assertEquals(1 + BuiltInPreset.entries.size, rotationPool(mine.take(1)).size)
    }

    @Test fun budgetShrinksToSavePower() {
        assertEquals(RenderBudget(60, 1f, true), renderBudget(60, batterySaver = false, lowEnd = false))
        assertEquals(RenderBudget(30, 0.6f, false), renderBudget(60, batterySaver = false, lowEnd = true))
        assertEquals(RenderBudget(20, 0.5f, false), renderBudget(60, batterySaver = true, lowEnd = true))
    }

    @Test fun beatsFollowBassHitsAndFade() {
        val tracker = BeatTracker()
        val quiet = FloatArray(32)
        val hit = FloatArray(32) { if (it < 6) 0.9f else 0f }
        repeat(30) { tracker.advance(quiet, 0.033f) }
        assertEquals(0f, tracker.beat)
        tracker.advance(hit, 0.033f)
        assertEquals(1f, tracker.beat)
        assertTrue(tracker.bass > 0.3f)
        repeat(60) { tracker.advance(quiet, 0.033f) }
        assertEquals(0f, tracker.beat)
        assertEquals(0f, tracker.bass)
    }

    @Test fun colorMatrixAdjustsColors() {
        val same = colorMatrix(1f, 1f, 1f)
        assertEquals(red, applyColorMatrix(red, same))
        val grey = applyColorMatrix(red, colorMatrix(1f, 0f, 1f))
        assertEquals(grey shr 16 and 0xFF, grey and 0xFF)
        assertEquals(0xFF7F0000.toInt(), applyColorMatrix(red, colorMatrix(0.5f, 1f, 1f)))
        assertEquals(0x80000000.toInt(), applyColorMatrix(0x80000000.toInt(), same))
    }

    @Test fun dominantColorsPreferVividAndDistinct() {
        val pixels = IntArray(100) { if (it < 60) 0xFF101010.toInt() else if (it < 90) red else blue }
        val colors = dominantColors(pixels, 4)
        assertEquals(red, colors.first())
        assertEquals(3, colors.size)
        assertTrue(dominantColors(IntArray(0)).isEmpty())
    }

    @Test fun blurSpreadsColorAndKeepsFlatAreas() {
        val flat = IntArray(25) { red }
        assertTrue(boxBlur(flat, 5, 5, 2).all { it == red })
        val dot = IntArray(81) { if (it == 40) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() }
        val blurred = boxBlur(dot, 9, 9, 1)
        assertTrue(blurred[40] and 0xFF < 255)
        assertTrue(blurred[39] and 0xFF > 0)
        assertEquals(255, dot[40] and 0xFF)
    }
}
