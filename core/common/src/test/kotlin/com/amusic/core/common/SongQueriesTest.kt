package com.amusic.core.common

import java.nio.charset.Charset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongQueriesTest {
    private fun song(
        id: Long, title: String, artist: String = "A", duration: Long = 100_000,
        folder: String = "Music", added: Long = id, track: Int = 0, albumId: Long = 1,
    ) = Song(id, "content://$id", title, artist, "Al", albumId, "", folder, duration, 1, added, track, "audio/mpeg")

    @Test fun searchFoldsPersianVariants() {
        val songs = listOf(song(1, "علی کوچک"), song(2, "Other"))
        assertEquals(listOf(1L), songs.search("علي كوچك").map { it.id })
    }

    @Test fun searchNeedsEveryWord() {
        val songs = listOf(song(1, "Blue Night", "Sam"))
        assertEquals(1, songs.search("night sam").size)
        assertEquals(0, songs.search("night max").size)
    }

    @Test fun blankSearchReturnsAll() = assertEquals(1, listOf(song(1, "x")).search("  ").size)

    @Test fun sortsByTitleAndDate() {
        val songs = listOf(song(1, "b"), song(2, "A"))
        assertEquals(listOf(2L, 1L), songs.sortedBy(SortOrder.TITLE, Locale.US).map { it.id })
        assertEquals(listOf(2L, 1L), songs.sortedBy(SortOrder.DATE_ADDED, Locale.US).map { it.id })
    }

    @Test fun albumGroupFollowsTrackNumbers() {
        val songs = listOf(song(1, "a", track = 2), song(2, "b", track = 1))
        assertEquals(listOf(2L, 1L), songs.grouped(GroupKind.ALBUM).single().songs.map { it.id })
    }

    @Test fun duplicatesUseTitleArtistAndLength() {
        val songs = listOf(
            song(1, "Same", duration = 100_000), song(2, "same ", duration = 101_000),
            song(3, "Same", duration = 200_000), song(4, "Same", artist = "B"),
        )
        assertEquals(listOf(listOf(1L, 2L)), songs.duplicates().map { group -> group.map { it.id } })
    }

    @Test fun detectsDownloads() {
        assertTrue(song(1, "a", folder = "Download/new").isInDownloads())
        assertTrue(!song(1, "a", folder = "Music").isInDownloads())
    }

    @Test fun repairsMisdecodedPersianTags() {
        val original = "سلام دنیا".replace('ی', 'ي')
        val broken = String(original.toByteArray(Charset.forName("windows-1256")), Charsets.ISO_8859_1)
        assertEquals(original, repairLegacyArabic(broken))
    }

    @Test fun leavesLatinAndUnicodeTagsAlone() {
        for (text in listOf("Beyoncé", "Déjà Vu", "Mötley Crüe", "سلام", "Hello", "")) {
            assertEquals(text, repairLegacyArabic(text))
        }
    }
}
