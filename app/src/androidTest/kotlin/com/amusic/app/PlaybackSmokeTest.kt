package com.amusic.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Launch, scan, play, skip and persist on a real Android system. */
@RunWith(AndroidJUnit4::class)
class PlaybackSmokeTest {
    @Test
    fun scansAndPlaysLocalFiles() = runBlocking {
        val songs = DeviceFixture.prepareSongs()
        val access = DeviceFixture.access
        val library = access.library()
        val player = access.player()

        ActivityScenario.launch(MainActivity::class.java).use {
            withContext(Dispatchers.Main) { player.play(songs, 0) }
            withTimeout(30_000) { player.state.first { it.isPlaying } }
            waitUntil { withContext(Dispatchers.Main) { player.positionMs() } > 1_000 }
            assertEquals(2, player.state.value.queue.size)

            withContext(Dispatchers.Main) { player.next() }
            withTimeout(10_000) { player.state.first { it.current?.songId == songs[1].id } }
            // A song enters the history only once it is audible, so let it start before pausing.
            waitUntil {
                player.state.value.isPlaying &&
                    withContext(Dispatchers.Main) { player.positionMs() } > 500
            }
            withTimeout(10_000) { library.recentlyPlayed.first { it.size >= 2 } }

            withContext(Dispatchers.Main) { player.togglePlay() }
            withTimeout(10_000) { player.state.first { !it.isPlaying } }

            waitUntil { access.playbackStore().load().index == 1 }
            assertEquals(songs.map { it.id }, access.playbackStore().load().songIds)
            val recent = withTimeout(10_000) { library.recentlyPlayed.first { it.size >= 2 } }
            assertTrue(recent.map { it.id }.containsAll(songs.map { it.id }))
        }
    }

    private suspend fun waitUntil(condition: suspend () -> Boolean) = withTimeout(30_000) {
        while (!condition()) delay(200)
    }
}
