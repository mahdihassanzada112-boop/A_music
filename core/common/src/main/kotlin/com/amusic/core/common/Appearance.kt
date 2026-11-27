package com.amusic.core.common

/** Player themes. The first entry is the default. */
enum class ThemeId {
    DARK_MODERN, GLASS;

    companion object {
        fun fromName(name: String?): ThemeId = entries.firstOrNull { it.name == name } ?: DARK_MODERN
    }
}

enum class OrientationMode {
    AUTO, PORTRAIT, LANDSCAPE;

    companion object {
        fun fromName(name: String?): OrientationMode = entries.firstOrNull { it.name == name } ?: AUTO
    }

    /** Which player layout to draw in a window that is currently wide or tall. */
    fun usesLandscape(windowIsWide: Boolean): Boolean = when (this) {
        AUTO -> windowIsWide
        PORTRAIT -> false
        LANDSCAPE -> true
    }
}
