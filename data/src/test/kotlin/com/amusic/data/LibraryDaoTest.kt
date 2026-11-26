package com.amusic.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amusic.core.database.AMusicDatabase
import com.amusic.core.database.PlaylistEntity
import com.amusic.core.database.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryDaoTest {
    private lateinit var db: AMusicDatabase

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AMusicDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After fun close() = db.close()

    private fun song(id: Long) =
        SongEntity(id, "content://$id", "t$id", "a", "al", 1, "", "Music", 1000, 1, id, 0, "audio/mpeg")

    @Test fun replaceAllRemovesMissingSongsIncludingLargeSets() = runBlocking {
        db.songDao().replaceAll((1L..2000L).map(::song))
        db.songDao().replaceAll(listOf(song(5), song(3000)))
        assertEquals(listOf(5L, 3000L), db.songDao().allIds().sorted())
    }

    @Test fun playlistKeepsEntriesWhenSongTemporarilyDisappears() = runBlocking {
        val dao = db.libraryDao()
        db.songDao().replaceAll(listOf(song(1), song(2)))
        val id = dao.insertPlaylist(PlaylistEntity(name = "p", createdAt = 1))
        dao.appendToPlaylist(id, listOf(1, 2, 1))
        assertEquals(3, dao.observePlaylists().first().single().songCount)

        db.songDao().replaceAll(listOf(song(2)))
        assertEquals(1, dao.observePlaylists().first().single().songCount)
        db.songDao().replaceAll(listOf(song(1), song(2)))
        assertEquals(listOf(1L, 2L, 1L), dao.observePlaylistEntries(id).first().map { it.song.id })
    }

    @Test fun reorderAndDeletePlaylist() = runBlocking {
        val dao = db.libraryDao()
        db.songDao().replaceAll(listOf(song(1), song(2)))
        val id = dao.insertPlaylist(PlaylistEntity(name = "p", createdAt = 1))
        dao.appendToPlaylist(id, listOf(1, 2))
        val entries = dao.observePlaylistEntries(id).first()
        dao.swapEntries(entries[0], entries[1])
        assertEquals(listOf(2L, 1L), dao.observePlaylistEntries(id).first().map { it.song.id })
        dao.deletePlaylist(id)
        assertEquals(0, dao.observePlaylistEntries(id).first().size)
    }

    @Test fun historyOrdersByLastPlay() = runBlocking {
        val dao = db.libraryDao()
        dao.recordPlay(1, 10)
        dao.recordPlay(2, 20)
        dao.recordPlay(1, 30)
        assertEquals(listOf(1L, 2L), dao.observeRecentIds().first())
    }
}
