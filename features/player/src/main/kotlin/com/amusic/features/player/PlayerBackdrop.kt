package com.amusic.features.player

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.theming.ApplyTheme
import com.amusic.core.theming.BackgroundStage
import com.amusic.core.theming.LocalAudioFeed
import com.amusic.core.theming.LocalPlayerTheme

/**
 * The studio background behind the whole player screen, system bars included. Draws nothing
 * while the studio is switched off, leaving the theme's own background.
 */
@Composable
fun PlayerBackdrop(viewModel: PlayerViewModel = hiltViewModel()) {
    val background by viewModel.background.collectAsStateWithLifecycle()
    val settings = background?.takeIf { it.enabled } ?: return
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LightSystemBarIcons()
    val theme = LocalPlayerTheme.current
    ApplyTheme(theme, dark = true) {
        CompositionLocalProvider(LocalAudioFeed provides viewModel.audioFeed) {
            Box(Modifier.fillMaxSize()) {
                theme.Background(Modifier.fillMaxSize())
                BackgroundStage(settings, ui?.artworkUri, ui?.isPlaying == true, Modifier.fillMaxSize())
            }
        }
    }
}

/** Keeps the status and navigation bar icons visible on a dark picture. */
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    DisposableEffect(window) {
        val bars = WindowCompat.getInsetsController(window, view)
        val status = bars.isAppearanceLightStatusBars
        val navigation = bars.isAppearanceLightNavigationBars
        bars.isAppearanceLightStatusBars = false
        bars.isAppearanceLightNavigationBars = false
        onDispose {
            bars.isAppearanceLightStatusBars = status
            bars.isAppearanceLightNavigationBars = navigation
        }
    }
}
