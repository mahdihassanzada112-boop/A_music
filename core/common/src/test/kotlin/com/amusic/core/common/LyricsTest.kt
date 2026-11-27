package com.amusic.core.common

import java.nio.charset.Charset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTest {
    @Test fun readsTimesRepeatsAndWordTags() {
        val lyrics = parseLyrics(
            "﻿[ar:Someone]\r\n[00:12.5]<00:12.50>hello <00:13.00>world\r\n[00:30.00][00:05]again\r\n",
        )
        assertTrue(lyrics.synced)
        assertEquals(listOf(5_000L, 12_500L, 30_000L), lyrics.lines.map { it.timeMs })
        assertEquals(listOf("again", "hello world", "again"), lyrics.lines.map { it.text })
    }

    @Test fun fileOffsetMovesLinesEarlier() {
        val lyrics = parseLyrics("[offset:+500]\n[00:10.00]a")
        assertEquals(9_500L, lyrics.lines.single().timeMs)
    }

    @Test fun findsTheActiveLine() {
        val lyrics = parseLyrics("[00:10]a\n[00:20]b\n[00:30]c")
        assertEquals(-1, lyrics.activeIndex(9_999))
        assertEquals(0, lyrics.activeIndex(10_000))
        assertEquals(1, lyrics.activeIndex(29_999))
        assertEquals(2, lyrics.activeIndex(500_000))
        // Delayed by one second.
        assertEquals(0, lyrics.activeIndex(20_500, offsetMs = 1_000))
    }

    @Test fun plainTextStaysPlain() {
        val lyrics = parseLyrics("\nخط اول\n\n[Chorus]\n")
        assertFalse(lyrics.synced)
        assertEquals(listOf("خط اول", "", "[Chorus]"), lyrics.lines.map { it.text })
        assertEquals(-1, lyrics.activeIndex(1_000))
        assertTrue(parseLyrics("   ").lines.isEmpty())
    }

    @Test fun decodesUnicodeAndLegacyFiles() {
        val text = "[00:01]سلام"
        assertEquals(text, decodeLyricsFile(text.toByteArray()))
        assertEquals(text, decodeLyricsFile(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + text.toByteArray()))
        assertEquals(text, decodeLyricsFile(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)))
        assertEquals(text, decodeLyricsFile(text.toByteArray(Charset.forName("windows-1256"))))
    }
}
