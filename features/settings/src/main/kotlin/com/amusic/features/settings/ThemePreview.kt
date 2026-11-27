package com.amusic.features.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.amusic.core.common.OrientationMode
import com.amusic.core.common.VisualizerSettings
import com.amusic.core.theming.ApplyTheme
import com.amusic.core.theming.LocalDarkTheme
import com.amusic.core.theming.PlayerActions
import com.amusic.core.theming.PlayerPanel
import com.amusic.core.theming.PlayerTheme
import com.amusic.core.theming.PlayerUi
import com.amusic.core.theming.RepeatUi

private const val SCALE = 0.36f

/** The theme's real player drawn small with sample data; touches and screen readers skip it. */
@Composable
internal fun ThemePreview(theme: PlayerTheme, landscape: Boolean) {
    val width = if (landscape) 760 else 360
    val height = if (landscape) 360 else 720
    val title = stringResource(R.string.preview_song)
    val artist = stringResource(R.string.preview_artist)
    val ui = remember(title, artist) {
        PlayerUi(
            title = title, artist = artist, album = "", artworkUri = null, durationMs = 241_000,
            isPlaying = false, showPause = true, shuffle = false, repeat = RepeatUi.ALL,
            speed = 1f, favorite = true, hasError = false,
            queue = List(4) { com.amusic.core.theming.QueueEntry("$title ${it + 1}", artist) },
            queueIndex = 0, sleepMinutesLeft = null, orientationMode = OrientationMode.AUTO,
            panel = PlayerPanel.NONE, lyrics = null, visualizer = VisualizerSettings(),
        )
    }
    val position = remember { mutableLongStateOf(92_000) }
    val density = LocalDensity.current
    val dark = LocalDarkTheme.current
    Box(
        Modifier.size((width * SCALE).dp, (height * SCALE).dp).clipToBounds()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    // Swallow taps before the miniature buttons see them; scrolling still works.
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes
                            .filter { it.pressed != it.previousPressed }
                            .forEach { it.consume() }
                    }
                }
            }
            .clearAndSetSemantics {},
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(density.density * SCALE, density.fontScale),
        ) {
            ApplyTheme(theme, dark) {
                Surface(
                    Modifier.fillMaxSize(),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    theme.Background(Modifier.fillMaxSize())
                    if (landscape) theme.LandscapePlayer(ui, position, NoActions)
                    else theme.PortraitPlayer(ui, position, NoActions)
                }
            }
        }
    }
}

private object NoActions : PlayerActions {
    override fun back() = Unit
    override fun togglePlay() = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun toggleShuffle() = Unit
    override fun cycleRepeat() = Unit
    override fun toggleFavorite() = Unit
    override fun setSpeed(speed: Float) = Unit
    override fun setSleepTimer(minutes: Int?) = Unit
    override fun jumpTo(index: Int) = Unit
    override fun removeAt(index: Int) = Unit
    override fun move(from: Int, to: Int) = Unit
    override fun clearQueue() = Unit
    override fun setOrientationMode(mode: OrientationMode) = Unit
    override fun showPanel(panel: PlayerPanel) = Unit
    override fun setVisualizer(settings: VisualizerSettings) = Unit
    override fun openVisualizerOptions() = Unit
    override fun importLyrics() = Unit
    override fun editLyrics() = Unit
    override fun removeLyrics() = Unit
    override fun setLyricsOffset(offsetMs: Long) = Unit
}
