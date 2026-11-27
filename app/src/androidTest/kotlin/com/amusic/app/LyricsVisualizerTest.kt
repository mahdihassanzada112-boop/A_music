package com.amusic.app

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.ThemeId
import com.amusic.core.common.VisualizerSettings
import com.amusic.core.theming.R as ThemingR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Lyrics stay on screen across themes and rotation, and the audio tap hears the playback. */
@RunWith(AndroidJUnit4::class)
class LyricsVisualizerTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun lyricsAndVisualizerFollowPlayback() = runBlocking {
        val songs = DeviceFixture.prepareSongs()
        val access = DeviceFixture.access
        val settings = access.settings()
        val player = access.player()
        settings.resetThemeSettings()
        settings.setVisualizer(VisualizerSettings())
        access.lyrics().save(songs[0].id, "[00:00.00]first test line\n[00:50.00]second test line")

        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                withContext(Dispatchers.Main) { player.play(songs, 0) }
                for (theme in ThemeId.entries) {
                    settings.setOrientationMode(OrientationMode.PORTRAIT)
                    settings.setThemeId(theme)
                    // The test song is a minute long; each theme starts it again.
                    withContext(Dispatchers.Main) { player.seekTo(0) }
                    compose.waitForTag("mini_player")
                    compose.onNodeWithTag("mini_player").performClick()
                    compose.waitForTag("player_portrait")

                    compose.click(ThemingR.string.player_lyrics)
                    compose.waitForText("first test line")
                    compose.click(ThemingR.string.orientation_switch)
                    compose.waitForTag("player_landscape")
                    compose.waitForText("first test line")

                    compose.click(ThemingR.string.player_visualizer)
                    compose.waitForTag("visualizer")

                    // The tap receives the 440 Hz test tone from the app's own playback.
                    access.analyzer().watch().use {
                        withTimeout(20_000) {
                            while (withContext(Dispatchers.Main) { access.analyzer().frame() }
                                    .bands.max() < 0.3f
                            ) {
                                delay(100)
                            }
                        }
                    }

                    compose.click(ThemingR.string.orientation_switch)
                    compose.waitForTag("player_portrait")
                    compose.waitForTag("visualizer")
                    assertEquals(0, player.state.value.index)

                    // Leave the panel and visualizer off for the next theme.
                    compose.click(ThemingR.string.player_visualizer)
                    compose.click(ThemingR.string.player_back)
                    compose.waitForTag("mini_player")
                }
                withContext(Dispatchers.Main) { player.togglePlay() }
            }
        } finally {
            access.lyrics().delete(songs[0].id)
            settings.setVisualizer(VisualizerSettings())
            settings.resetThemeSettings()
        }
    }

    private fun ComposeTestRule.click(description: Int) {
        val node = onNodeWithContentDescription(DeviceFixture.context.getString(description))
        // On small screens some controls sit in a scrolling column.
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    private fun ComposeTestRule.waitForTag(tag: String) = waitFor("tag $tag") {
        onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).size == 1
    }

    private fun ComposeTestRule.waitForText(text: String) = waitFor("text $text") {
        onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }

    /** Says what was missing, and what was on screen instead, when a wait times out. */
    private fun ComposeTestRule.waitFor(what: String, condition: () -> Boolean) {
        try {
            waitUntil(20_000, condition)
        } catch (e: ComposeTimeoutException) {
            // The test log shows only the first line of a message.
            val screen = runCatching {
                onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .flatMap { describe(it) }.joinToString(" | ")
            }.getOrElse { it.toString() }
            val settings = runBlocking { DeviceFixture.access.settings().visualizer.first() }
            val theme = runBlocking { DeviceFixture.access.settings().themeId.first() }
            throw AssertionError("No $what in $theme, $settings. On screen: $screen", e)
        }
    }

    private fun describe(node: SemanticsNode): List<String> {
        val own = listOfNotNull(
            node.config.getOrNull(SemanticsProperties.TestTag),
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(),
            node.config.getOrNull(SemanticsProperties.Text)?.joinToString(),
        )
        val line = if (own.isEmpty()) emptyList() else listOf("${own.joinToString("/")} ${node.boundsInRoot}")
        return line + node.children.flatMap { describe(it) }
    }
}
