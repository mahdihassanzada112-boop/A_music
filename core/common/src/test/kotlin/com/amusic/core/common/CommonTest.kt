package com.amusic.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class CommonTest {
    @Test fun formatsMinutes() = assertEquals("3:05", formatDuration(185_000))
    @Test fun formatsHours() = assertEquals("1:01:01", formatDuration(3_661_000))
    @Test fun clampsNegative() = assertEquals("0:00", formatDuration(-5))
    @Test fun unknownThemeFallsBack() = assertEquals(ThemeMode.SYSTEM, ThemeMode.fromName("x"))
    @Test fun knownThemeParses() = assertEquals(ThemeMode.DARK, ThemeMode.fromName("DARK"))
}
