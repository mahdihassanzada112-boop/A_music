package com.amusic.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amusic.core.common.AutoBackground
import com.amusic.core.common.BackgroundScene
import com.amusic.core.common.BackgroundSettings
import com.amusic.core.common.BackgroundStyle
import com.amusic.core.common.BuiltInPreset
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.ThemeId
import com.amusic.core.theming.R as ThemingR
import com.amusic.features.studio.R as StudioR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every style draws on a real screen, in both themes and orientations, with presets. */
@RunWith(AndroidJUnit4::class)
class BackgroundStudioTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun studioDrawsEveryStyleAndFollowsThePlayer() = runBlocking {
        val songs = DeviceFixture.prepareSongs()
        val access = DeviceFixture.access
        val settings = access.settings()
        val player = access.player()
        val presets = access.presets()
        settings.resetThemeSettings()
        settings.updateBackground { BackgroundSettings() }
        presets.presets.first().forEach { presets.delete(it.id) }

        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                withContext(Dispatchers.Main) { player.play(songs, 0) }
                for (theme in ThemeId.entries) {
                    settings.setOrientationMode(OrientationMode.PORTRAIT)
                    settings.setThemeId(theme)
                    settings.updateBackground { it.copy(enabled = true, scene = BackgroundScene()) }
                    compose.waitForTag("mini_player")
                    compose.onNodeWithTag("mini_player").performClick()
                    compose.waitForTag("player_portrait")
                    compose.waitForTag("background_stage")

                    compose.click(ThemingR.string.player_studio)
                    compose.waitForTag("studio")
                    compose.waitForTag("studio_preview")
                    delay(1_000)
                    val empty = compose.preview()
                    for (style in BackgroundStyle.entries) {
                        settings.updateBackground { it.copy(scene = BackgroundScene().add(style)) }
                        var changed = 0
                        for (attempt in 1..20) {
                            delay(300)
                            changed = difference(empty, compose.preview())
                            if (changed > 100) break
                        }
                        assertTrue("$style in $theme changed only $changed pixels", changed > 100)
                    }
                    // All the layers of the richest preset together.
                    settings.updateBackground { it.copy(scene = BuiltInPreset.CONCERT.scene) }
                    delay(500)
                    assertTrue(difference(empty, compose.preview()) > 100)

                    // Saving a preset through the screen.
                    val name = "Test $theme"
                    compose.onNodeWithText(text(StudioR.string.studio_save_preset)).performScrollTo().performClick()
                    compose.onNodeWithTag("preset_name").performTextInput(name)
                    compose.onNodeWithText(text(ThemingR.string.action_save)).performClick()
                    val saved = withTimeout(10_000) {
                        presets.presets.first { all -> all.any { it.name == name } }
                    }.first { it.name == name }
                    assertEquals(BuiltInPreset.CONCERT.scene, saved.scene)

                    compose.click(ThemingR.string.player_back)
                    compose.waitForTag("player_portrait")
                    compose.waitForTag("background_stage")
                    compose.click(ThemingR.string.orientation_switch)
                    compose.waitForTag("player_landscape")
                    compose.waitForTag("background_stage")
                    assertEquals(0, player.state.value.index)

                    // A new song brings a new background when asked to.
                    settings.updateBackground { it.copy(auto = AutoBackground.IN_ORDER) }
                    val before = settings.background.first().scene
                    withContext(Dispatchers.Main) { player.next() }
                    withTimeout(15_000) { settings.background.first { it.scene != before } }
                    compose.waitForTag("background_stage")
                    settings.updateBackground { it.copy(auto = AutoBackground.OFF) }
                    withContext(Dispatchers.Main) { player.jumpTo(0) }

                    compose.click(ThemingR.string.player_back)
                    compose.waitForTag("mini_player")
                }
                withContext(Dispatchers.Main) { player.togglePlay() }
            }
        } finally {
            presets.presets.first().forEach { presets.delete(it.id) }
            settings.updateBackground { BackgroundSettings() }
            settings.resetThemeSettings()
        }
    }

    private fun text(id: Int) = DeviceFixture.context.getString(id)

    private fun ComposeTestRule.click(description: Int) {
        val node = onNodeWithContentDescription(text(description))
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    private fun ComposeTestRule.preview(): Bitmap =
        onNodeWithTag("studio_preview").captureToImage().asAndroidBitmap()

    /** How many pixels differ noticeably. */
    private fun difference(a: Bitmap, b: Bitmap): Int {
        var count = 0
        for (y in 0 until minOf(a.height, b.height)) {
            for (x in 0 until minOf(a.width, b.width)) {
                val one = a.getPixel(x, y)
                val two = b.getPixel(x, y)
                val apart = (0..16 step 8).sumOf { kotlin.math.abs((one shr it and 0xFF) - (two shr it and 0xFF)) }
                if (apart > 12) count++
            }
        }
        return count
    }

    private fun ComposeTestRule.waitForTag(tag: String) {
        try {
            waitUntil(20_000) {
                onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).size == 1
            }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            val theme = runBlocking { DeviceFixture.access.settings().themeId.first() }
            throw AssertionError("No tag $tag in $theme", e)
        }
    }
}
