package com.amusic.themes.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amusic.core.common.ThemeId
import com.amusic.core.theming.HomeActions
import com.amusic.core.theming.HomeUi
import com.amusic.core.theming.LocalDarkTheme
import com.amusic.core.theming.NavItem
import com.amusic.core.theming.PlayerActions
import com.amusic.core.theming.PlayerTheme
import com.amusic.core.theming.PlayerUi
import com.amusic.core.theming.Position

/** Translucent layers over blurred pools of colour, large round corners, light type. */
object GlassTheme : PlayerTheme {
    override val id = ThemeId.GLASS
    override val name = R.string.theme_glass
    override val description = R.string.theme_glass_description

    private val base = Typography()
    override val typography = base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Light),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium),
    )

    override val shapes = Shapes(
        extraSmall = RoundedCornerShape(16.dp),
        small = RoundedCornerShape(18.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(32.dp),
    )

    // Containers used by dialogs and menus stay opaque so their text is always readable.
    private val dark = darkColorScheme(
        primary = Color(0xFFFFFFFF),
        onPrimary = Color(0xFF32276E),
        secondaryContainer = Color(0x40FFFFFF),
        onSecondaryContainer = Color.White,
        background = Color(0xFF26234F),
        onBackground = Color.White,
        surface = Color(0xFF2E2A60),
        onSurface = Color.White,
        surfaceVariant = Color(0x26FFFFFF),
        onSurfaceVariant = Color(0xFFDAD7F5),
        surfaceContainer = Color(0xFF38337A),
        surfaceContainerHigh = Color(0xFF3D3884),
        surfaceContainerHighest = Color(0xFF46408F),
    )

    private val light = lightColorScheme(
        primary = Color(0xFF4A3AA8),
        onPrimary = Color.White,
        secondaryContainer = Color(0x334A3AA8),
        onSecondaryContainer = Color(0xFF1B1446),
        background = Color(0xFFEDEBFB),
        onBackground = Color(0xFF1E1A3C),
        surface = Color(0xFFF6F4FF),
        onSurface = Color(0xFF1E1A3C),
        surfaceVariant = Color(0x80FFFFFF),
        onSurfaceVariant = Color(0xFF4B4770),
        surfaceContainer = Color(0xFFF1EEFF),
    )

    override fun colors(dark: Boolean) = if (dark) this.dark else light

    @Composable
    override fun Background(modifier: Modifier) {
        val dark = LocalDarkTheme.current
        val base = colors(dark).background
        val pools = if (dark) {
            listOf(Color(0xCCD9579F), Color(0xB32FA9C4), Color(0xCC5B4BE0))
        } else {
            listOf(Color(0xCCFFB7DC), Color(0xCCA8ECF5), Color(0xB3C6BCFF))
        }
        Canvas(modifier) {
            drawRect(base)
            val centers = listOf(
                Offset(size.width * 0.12f, size.height * 0.1f),
                Offset(size.width * 0.95f, size.height * 0.45f),
                Offset(size.width * 0.25f, size.height * 0.95f),
            )
            centers.zip(pools).forEach { (center, color) ->
                val radius = size.maxDimension * 0.45f
                drawCircle(
                    Brush.radialGradient(listOf(color, Color.Transparent), center, radius),
                    radius,
                    center,
                )
            }
        }
    }

    @Composable
    override fun NavigationBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit) =
        GlassNavigationBar(items, selected, onSelect)

    @Composable
    override fun MiniPlayer(ui: PlayerUi, position: Position, actions: PlayerActions, onOpen: () -> Unit) =
        GlassMiniPlayer(ui, position, actions, onOpen)

    @Composable
    override fun Home(ui: HomeUi, actions: HomeActions) = GlassHome(ui, actions)

    @Composable
    override fun PortraitPlayer(ui: PlayerUi, position: Position, actions: PlayerActions) =
        GlassPortraitPlayer(ui, position, actions)

    @Composable
    override fun LandscapePlayer(ui: PlayerUi, position: Position, actions: PlayerActions) =
        GlassLandscapePlayer(ui, position, actions)
}
