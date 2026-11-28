package com.amusic.core.theming

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.amusic.core.common.LandscapeLayout
import com.amusic.core.common.LandscapePanel
import com.amusic.core.common.ThemeId

/**
 * One complete visual identity. Library, search, playlists and settings share logic and take
 * their look from [colors], [typography], [shapes] and [Background]; the rest is theme-drawn.
 */
interface PlayerTheme {
    val id: ThemeId
    val name: Int
    val description: Int
    val typography: Typography
    val shapes: Shapes

    fun colors(dark: Boolean): ColorScheme

    /** Drawn behind every screen. */
    @Composable
    fun Background(modifier: Modifier)

    @Composable
    fun NavigationBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit)

    @Composable
    fun MiniPlayer(ui: PlayerUi, position: Position, actions: PlayerActions, onOpen: () -> Unit)

    @Composable
    fun Home(ui: HomeUi, actions: HomeActions)

    @Composable
    fun PortraitPlayer(ui: PlayerUi, position: Position, actions: PlayerActions)

    @Composable
    fun LandscapePlayer(ui: PlayerUi, position: Position, actions: PlayerActions)

    /**
     * One part of a listener's own landscape arrangement, drawn in the theme's look. Asked
     * for ARTWORK, INFO, PROGRESS and CONTROLS; the other panels are shared.
     */
    @Composable
    fun LandscapeBlock(
        panel: LandscapePanel,
        ui: PlayerUi,
        position: Position,
        actions: PlayerActions,
        layout: LandscapeLayout,
        modifier: Modifier,
    )

    /** The surface behind a shared panel in such an arrangement. */
    @Composable
    fun LandscapePane(modifier: Modifier, content: @Composable () -> Unit)
}

val LocalDarkTheme = staticCompositionLocalOf { true }

val LocalPlayerTheme = staticCompositionLocalOf<PlayerTheme> { error("No player theme provided") }

@Composable
fun ApplyTheme(theme: PlayerTheme, dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPlayerTheme provides theme, LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = theme.colors(dark),
            typography = theme.typography,
            shapes = theme.shapes,
            content = content,
        )
    }
}
