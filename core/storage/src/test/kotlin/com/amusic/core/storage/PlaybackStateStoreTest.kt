package com.amusic.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PlaybackStateStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun emptyThenRoundTrip() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.root.resolve("playback.preferences_pb")
        }
        val playback = PlaybackStateStore(store)
        assertEquals(SavedPlayback(), playback.load())
        val state = SavedPlayback(listOf(3, 1, 3), 2, 4500, 2, true, 1.25f)
        playback.save(state)
        assertEquals(state, playback.load())
    }
}
