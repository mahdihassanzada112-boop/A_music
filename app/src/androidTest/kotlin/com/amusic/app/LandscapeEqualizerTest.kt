package com.amusic.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amusic.core.common.EqPreset
import com.amusic.core.common.EqualizerSettings
import com.amusic.core.common.LandscapeLayout
import com.amusic.core.common.LandscapePanel
import com.amusic.core.common.LayoutColumn
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.PanelSlot
import com.amusic.core.common.ThemeId
import com.amusic.core.theming.R as ThemingR
import com.amusic.features.customize.R as CustomizeR
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

/** The listener's own landscape layouts, and the equalizer heard on the real playback. */
@RunWith(AndroidJUnit4::class)
class LandscapeEqualizerTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val access = DeviceFixture.access
    private val settings = access.settings()

    @Test
    fun layoutsAreEditedSavedAndShownPerTheme() = runBlocking {
        val songs = DeviceFixture.prepareSongs()
        val player = access.player()
        reset()
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                withContext(Dispatchers.Main) { player.play(songs, 0) }
                for (theme in ThemeId.entries) {
                    // Another theme's layout must not appear here.
                    assertEquals(null, settings.landscapeLayout(theme).first())
                    settings.setOrientationMode(OrientationMode.PORTRAIT)
                    settings.setThemeId(theme)
                    withContext(Dispatchers.Main) { player.seekTo(0) }
                    compose.waitForTag("mini_player")
                    compose.onNodeWithTag("mini_player").performClick()
                    compose.waitForTag("player_portrait")
                    compose.click(ThemingR.string.orientation_switch)
                    compose.waitForTag("player_landscape")
                    compose.waitForNoTag("custom_landscape")

                    // From the landscape player to the editor.
                    val menu = compose.onNodeWithText(text(ThemingR.string.orientation_landscape))
                    // Only some themes keep this button in a scrolling column.
                    runCatching { menu.performScrollTo() }
                    menu.performClick()
                    compose.onNodeWithText(text(ThemingR.string.layout_customize)).performClick()
                    compose.waitForTag("layout_editor")
                    compose.onNodeWithText(text(CustomizeR.string.layout_three_columns))
                        .performScrollTo().performClick()
                    awaitLayout(theme) { it == LandscapeLayout.ThreeColumns }

                    // The menu hides one panel and shows another.
                    compose.waitForTag("editor_card_QUEUE")
                    compose.menu(LandscapePanel.QUEUE, text(CustomizeR.string.layout_hide))
                    awaitLayout(theme) { LandscapePanel.QUEUE in it.hidden }
                    compose.menu(LandscapePanel.LYRICS, text(CustomizeR.string.layout_move_to, 3))
                    awaitLayout(theme) { it.columnOf(LandscapePanel.LYRICS) == 2 }

                    // Press, hold and drag the artwork to the top of the middle column.
                    compose.drag(LandscapePanel.ARTWORK, column = 1)
                    val edited = awaitLayout(theme) { it.columnOf(LandscapePanel.ARTWORK) == 1 }
                    assertEquals(LandscapePanel.ARTWORK, edited.columns[1].panels.first().panel)

                    val name = "Mine $theme"
                    compose.openNameDialog(text(CustomizeR.string.layout_save_profile))
                    compose.onNodeWithTag("profile_name").performTextInput(name)
                    compose.onNodeWithText(text(ThemingR.string.action_save)).performClick()
                    withTimeout(10_000) {
                        access.profiles().layouts(theme).first { all -> all.any { it.name == name } }
                    }

                    // Restoring the theme's design and bringing the saved one back.
                    compose.onNodeWithText(text(CustomizeR.string.layout_restore))
                        .performScrollTo().performClick()
                    withTimeout(10_000) { settings.landscapeLayout(theme).first { it == null } }
                    compose.onNodeWithText(name).performScrollTo().performClick()
                    awaitLayout(theme) { it == edited }

                    compose.click(ThemingR.string.player_back)
                    compose.waitForTag("custom_landscape")
                    compose.waitForTag("panel_LYRICS")
                    compose.waitForTag("panel_ARTWORK")
                    compose.waitForNoTag("panel_QUEUE")

                    // Its buttons drive the same playback.
                    compose.click(ThemingR.string.player_pause)
                    withTimeout(10_000) { player.state.first { !it.isPlaying } }
                    compose.click(ThemingR.string.player_play)
                    withTimeout(10_000) { player.state.first { it.isPlaying } }

                    compose.click(ThemingR.string.orientation_switch)
                    compose.waitForTag("player_portrait")
                    compose.click(ThemingR.string.orientation_switch)
                    compose.waitForTag("custom_landscape")

                    // Every panel can be on screen at once.
                    settings.updateLandscapeLayout(theme) { everything }
                    LandscapePanel.entries.forEach { compose.waitForTag("panel_${it.name}") }
                    assertEquals(0, player.state.value.index)

                    compose.click(ThemingR.string.player_back)
                    compose.waitForTag("mini_player")
                }
                withContext(Dispatchers.Main) { player.togglePlay() }
            }
        } finally {
            reset()
        }
    }

    @Test
    fun equalizerChangesWhatIsHeard() = runBlocking {
        val songs = DeviceFixture.prepareSongs()
        val player = access.player()
        reset()
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                withContext(Dispatchers.Main) { player.play(songs, 0) }
                compose.waitForTag("mini_player")
                compose.onNodeWithTag("mini_player").performClick()
                compose.waitForTag("player_portrait")
                compose.click(ThemingR.string.player_equalizer)
                compose.waitForTag("equalizer_screen")
                compose.onNodeWithText(text(ThemingR.string.eq_rock)).performScrollTo().performClick()
                withTimeout(10_000) {
                    settings.equalizer.first { it.enabled && it.preset == EqPreset.ROCK }
                }
                compose.onNodeWithText(text(CustomizeR.string.equalizer_save))
                    .performScrollTo().performClick()
                compose.onNodeWithTag("profile_name").performTextInput("Mine")
                compose.onNodeWithText(text(ThemingR.string.action_save)).performClick()
                val curve = withTimeout(10_000) { access.profiles().curves.first { it.isNotEmpty() } }
                assertEquals(EqPreset.ROCK.bands, curve.first().bands)

                // The level of the 440 Hz test tone, taken after the equalizer.
                access.analyzer().watch().use {
                    settings.updateEqualizer { EqualizerSettings() }
                    withContext(Dispatchers.Main) { player.seekTo(0) }
                    val plain = level { it > 0.5f }
                    val cut = EqualizerSettings(
                        enabled = true, preset = EqPreset.CUSTOM, bands = List(10) { -12f },
                    )
                    settings.updateEqualizer { cut }
                    level { it > 0f && it < plain - 0.1f }

                    // Without headphones, "only with headphones" leaves the sound alone.
                    if (!access.equalizer().headphones.value) {
                        settings.updateEqualizer { cut.copy(headphonesOnly = true) }
                        level { it > plain - 0.03f }
                    }
                    settings.updateEqualizer { cut }
                    level { it > 0f && it < plain - 0.1f }
                    settings.updateEqualizer { cut.copy(enabled = false) }
                    level { it > plain - 0.03f }
                }
                withContext(Dispatchers.Main) { player.togglePlay() }
            }
        } finally {
            reset()
        }
    }

    private val everything = LandscapeLayout(
        LandscapePanel.entries.chunked(3).map { column -> LayoutColumn(column.map { PanelSlot(it) }) },
    )

    /** Waits until the loudest band satisfies [wanted] and returns it. */
    private suspend fun level(wanted: (Float) -> Boolean): Float {
        var heard = 0f
        try {
            withTimeout(20_000) {
                while (true) {
                    heard = withContext(Dispatchers.Main) { access.analyzer().frame() }.bands.max()
                    if (wanted(heard)) break
                    delay(100)
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw AssertionError("Level stayed at $heard with ${settings.equalizer.first()}", e)
        }
        return heard
    }

    private suspend fun reset() {
        settings.resetThemeSettings()
        settings.updateEqualizer { EqualizerSettings() }
        val profiles = access.profiles()
        for (theme in ThemeId.entries) {
            settings.updateLandscapeLayout(theme) { null }
            profiles.layouts(theme).first().forEach { profiles.deleteLayout(it.id) }
        }
        profiles.curves.first().forEach { profiles.deleteCurve(it.id) }
    }

    private suspend fun awaitLayout(theme: ThemeId, wanted: (LandscapeLayout) -> Boolean): LandscapeLayout {
        var last: LandscapeLayout? = null
        return try {
            withTimeout(10_000) {
                settings.landscapeLayout(theme).first { last = it; it != null && wanted(it) }!!
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw AssertionError("Layout of $theme stayed $last", e)
        }
    }

    private fun text(id: Int, vararg values: Any) = DeviceFixture.context.getString(id, *values)

    private fun ComposeTestRule.click(description: Int) {
        val node = onNodeWithContentDescription(text(description))
        // On small screens some controls sit in a scrolling column.
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    private fun ComposeTestRule.openNameDialog(label: String) {
        onNodeWithText(label).performScrollTo().performClick()
        waitForTag("profile_name")
    }

    private fun ComposeTestRule.menu(panel: LandscapePanel, choice: String) {
        onNodeWithTag("editor_menu_${panel.name}", useUnmergedTree = true).performScrollTo().performClick()
        onNodeWithText(choice).performClick()
        waitForIdle()
    }

    private suspend fun ComposeTestRule.drag(panel: LandscapePanel, column: Int) {
        val card = onNodeWithTag("editor_card_${panel.name}")
        card.performScrollTo()
        waitForIdle()
        val from = card.fetchSemanticsNode().boundsInRoot
        val to = onNodeWithTag("editor_column_$column", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // Just below the column's top edge, above every card in it.
        val target = Offset(to.center.x, to.top + 6f) - from.topLeft
        val start = Offset(from.width * 0.3f, from.height / 2)
        card.performTouchInput { down(start) }
        mainClock.advanceTimeBy(1_000)
        delay(1_000)
        card.performTouchInput { moveTo((start + target) / 2f) }
        card.performTouchInput { moveTo(target) }
        waitForIdle()
        card.performTouchInput { up() }
        waitForIdle()
    }

    private fun ComposeTestRule.waitForTag(tag: String) = waitFor("tag $tag") {
        onAllNodesWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false).size == 1
    }

    private fun ComposeTestRule.waitForNoTag(tag: String) = waitFor("end of tag $tag") {
        onAllNodesWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty()
    }

    private fun ComposeTestRule.waitFor(what: String, condition: () -> Boolean) {
        try {
            waitUntil(20_000, condition)
        } catch (e: ComposeTimeoutException) {
            // The test log shows only the first line of a message.
            val theme = runBlocking { settings.themeId.first() }
            throw AssertionError("No $what in $theme", e)
        }
    }
}
