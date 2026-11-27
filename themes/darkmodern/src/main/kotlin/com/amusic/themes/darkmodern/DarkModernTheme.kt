package com.amusic.themes.darkmodern

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amusic.core.common.ThemeId
import com.amusic.core.theming.HomeActions
import com.amusic.core.theming.HomeUi
import com.amusic.core.theming.NavItem
import com.amusic.core.theming.PlayerActions
import com.amusic.core.theming.PlayerTheme
import com.amusic.core.theming.PlayerUi
import com.amusic.core.theming.Position

/** Flat, solid surfaces, one amber accent, bold headings, tight corners. */
object DarkModernTheme : PlayerTheme {
    override val id = ThemeId.DARK_MODERN
    override val name = R.string.theme_dark_modern
    override val description = R.string.theme_dark_modern_description

    private val base = Typography()
    override val typography = base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    )

    override val shapes = Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
    )

    private val dark = darkColorScheme(
        primary = Color(0xFFFFB454),
        onPrimary = Color(0xFF1C1100),
        secondaryContainer = Color(0xFF3A2C16),
        onSecondaryContainer = Color(0xFFFFDDB0),
        background = Color(0xFF0E1016),
        onBackground = Color(0xFFF2F0EC),
        surface = Color(0xFF0E1016),
        onSurface = Color(0xFFF2F0EC),
        surfaceVariant = Color(0xFF1C2029),
        onSurfaceVariant = Color(0xFFA7A9B3),
        surfaceContainer = Color(0xFF181B23),
        surfaceContainerHigh = Color(0xFF1F232D),
        surfaceContainerHighest = Color(0xFF262A35),
    )

    private val light = lightColorScheme(
        primary = Color(0xFF8A5100),
        onPrimary = Color.White,
        secondaryContainer = Color(0xFFFFDDB0),
        onSecondaryContainer = Color(0xFF2B1700),
        background = Color(0xFFFBF8F4),
        surface = Color(0xFFFBF8F4),
        surfaceVariant = Color(0xFFEEE7DD),
        surfaceContainer = Color(0xFFF3EDE4),
    )

    override fun colors(dark: Boolean) = if (dark) this.dark else light

    @Composable
    override fun Background(modifier: Modifier) =
        Box(modifier.background(MaterialTheme.colorScheme.background))

    @Composable
    override fun NavigationBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit) =
        DarkNavigationBar(items, selected, onSelect)

    @Composable
    override fun MiniPlayer(ui: PlayerUi, position: Position, actions: PlayerActions, onOpen: () -> Unit) =
        DarkMiniPlayer(ui, position, actions, onOpen)

    @Composable
    override fun Home(ui: HomeUi, actions: HomeActions) = DarkHome(ui, actions)

    @Composable
    override fun PortraitPlayer(ui: PlayerUi, position: Position, actions: PlayerActions) =
        DarkPortraitPlayer(ui, position, actions)

    @Composable
    override fun LandscapePlayer(ui: PlayerUi, position: Position, actions: PlayerActions) =
        DarkLandscapePlayer(ui, position, actions)
}
