package com.amusic.app

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.ThemeId
import com.amusic.core.theming.R as ThemingR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Draws all four themed players and checks that playback survives themes and rotation. */
@RunWith(AndroidJUnit4::class)
class ThemeOrientationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun playbackSurvivesThemeAndOrientationChanges() = runBlocking {
        val songs = DeviceFixture.prepareSongs()
        val access = DeviceFixture.access
        val settings = access.settings()
        val player = access.player()
        settings.resetThemeSettings()

        ActivityScenario.launch(MainActivity::class.java).use {
            withContext(Dispatchers.Main) { player.play(songs, 0) }
            var last = 0L
            for (theme in ThemeId.entries) {
                settings.setOrientationMode(OrientationMode.PORTRAIT)
                settings.setThemeId(theme)
                compose.waitForTag("mini_player")
                compose.onNodeWithTag("mini_player").performClick()
                compose.waitForTag("player_portrait")

                compose.onNodeWithContentDescription(text(ThemingR.string.orientation_switch)).performClick()
                compose.waitForTag("player_landscape")
                compose.waitForDescription(text(ThemingR.string.player_pause))

                compose.onNodeWithContentDescription(text(ThemingR.string.orientation_switch)).performClick()
                compose.waitForTag("player_portrait")

                // The same engine keeps going: same queue, same song, position moving forward.
                delay(700)
                val position = withContext(Dispatchers.Main) { player.positionMs() }
                assertTrue("position went from $last to $position", position > last)
                last = position
                assertTrue(player.state.value.isPlaying)
                assertEquals(songs.map { it.id }, player.state.value.queue.map { it.songId })
                assertEquals(0, player.state.value.index)

                compose.onNodeWithContentDescription(text(ThemingR.string.player_back)).performClick()
                compose.waitForTag("mini_player")
            }
            withContext(Dispatchers.Main) { player.togglePlay() }
        }
        settings.resetThemeSettings()
    }

    private fun text(id: Int) = DeviceFixture.context.getString(id)

    private fun ComposeTestRule.waitForTag(tag: String) = waitUntil(20_000) {
        onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).size == 1
    }

    private fun ComposeTestRule.waitForDescription(description: String) = waitUntil(20_000) {
        onAllNodesWithContentDescription(description)
            .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }
}
