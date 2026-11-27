package com.amusic.core.theming

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.amusic.core.design.Artwork

/** Album art that fades between songs. */
@Composable
fun FadingArtwork(ui: PlayerUi, modifier: Modifier = Modifier) {
    Crossfade(ui.artworkUri, animationSpec = tween(500), label = "artwork") { uri ->
        Artwork(uri, modifier)
    }
}
