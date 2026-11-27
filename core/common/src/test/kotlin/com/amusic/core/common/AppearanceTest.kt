package com.amusic.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceTest {
    @Test fun unknownThemeIsDefault() = assertEquals(ThemeId.DARK_MODERN, ThemeId.fromName("removed"))
    @Test fun autoFollowsWindow() {
        assertEquals(true, OrientationMode.AUTO.usesLandscape(true))
        assertEquals(false, OrientationMode.AUTO.usesLandscape(false))
    }
    @Test fun locksIgnoreWindow() {
        assertEquals(false, OrientationMode.PORTRAIT.usesLandscape(true))
        assertEquals(true, OrientationMode.LANDSCAPE.usesLandscape(false))
    }
}
