package com.amusic.app

import com.amusic.core.common.ThemeId
import com.amusic.core.theming.PlayerTheme
import com.amusic.themes.darkmodern.DarkModernTheme
import com.amusic.themes.glass.GlassTheme

/** The only place that knows every theme; add new ones here. */
internal val PlayerThemes: List<PlayerTheme> = listOf(DarkModernTheme, GlassTheme)

internal fun themeFor(id: ThemeId): PlayerTheme = PlayerThemes.first { it.id == id }
