package com.amusic.themes.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.amusic.core.theming.LocalDarkTheme

/** A frosted pane: a soft diagonal sheen with a bright edge. */
@Composable
internal fun Modifier.glass(shape: Shape = MaterialTheme.shapes.large): Modifier {
    val dark = LocalDarkTheme.current
    val high = Color.White.copy(alpha = if (dark) 0.22f else 0.75f)
    val low = Color.White.copy(alpha = if (dark) 0.08f else 0.45f)
    val edge = Color.White.copy(alpha = if (dark) 0.32f else 0.9f)
    return clip(shape).background(Brush.linearGradient(listOf(high, low))).border(1.dp, edge, shape)
}
