package com.amusic.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB454),
    onPrimary = Color(0xFF2A1800),
    background = Color(0xFF101218),
    surface = Color(0xFF101218),
    surfaceVariant = Color(0xFF1C2029),
    surfaceContainer = Color(0xFF171A22),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5100),
    background = Color(0xFFFBF8F4),
    surface = Color(0xFFFBF8F4),
)

/** Shell theme only. The ten player themes arrive in Phase 3 with their own components. */
@Composable
fun AMusicTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
