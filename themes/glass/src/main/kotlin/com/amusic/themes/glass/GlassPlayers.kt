package com.amusic.themes.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.amusic.core.design.artistLabel
import com.amusic.core.theming.BackButton
import com.amusic.core.theming.FadingArtwork
import com.amusic.core.theming.FavoriteButton
import com.amusic.core.theming.LeftToRight
import com.amusic.core.theming.NextButton
import com.amusic.core.theming.LyricsButton
import com.amusic.core.theming.LyricsPanel
import com.amusic.core.theming.PlayerPanel
import com.amusic.core.theming.QueueButton
import com.amusic.core.theming.VisualizerButton
import com.amusic.core.theming.VisualizerStage
import com.amusic.core.theming.OrientationControls
import com.amusic.core.theming.PlayButton
import com.amusic.core.theming.PlayerActions
import com.amusic.core.theming.PlayerUi
import com.amusic.core.theming.Position
import com.amusic.core.theming.PreviousButton
import com.amusic.core.theming.QueuePanel
import com.amusic.core.theming.RepeatButton
import com.amusic.core.theming.SeekBar
import com.amusic.core.theming.ShuffleButton
import com.amusic.core.theming.SleepMenu
import com.amusic.core.theming.SpeedMenu
import com.amusic.core.theming.R as ThemingR

/** Portrait: a turning disc floating above one pane of information and controls. */
@Composable
internal fun GlassPortraitPlayer(ui: PlayerUi, position: Position, actions: PlayerActions) {
    Column(Modifier.fillMaxSize().testTag("player_portrait").padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BackButton(actions)
            Box(Modifier.weight(1f))
            OrientationControls(ui, actions, isLandscape = false)
        }
        when (ui.panel) {
            PlayerPanel.QUEUE -> QueuePanel(ui, actions, Modifier.weight(1f).fillMaxWidth().glass())
            PlayerPanel.LYRICS -> {
                LyricsPanel(ui, position, actions, Modifier.weight(1f).fillMaxWidth().glass())
                Column(Modifier.padding(top = 8.dp).fillMaxWidth().glass().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Timeline(ui, position, actions)
                    Transport(ui, actions, 56.dp)
                }
            }
            PlayerPanel.NONE -> BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val disc = min(maxWidth - 80.dp, maxHeight * 0.38f)
                val full = maxHeight
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = full),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (ui.visualizer.enabled) {
                        VisualizerStage(ui, actions, Modifier.fillMaxWidth().height(disc).glass().padding(12.dp))
                    } else {
                        Disc(ui, disc)
                    }
                    Column(
                        Modifier.fillMaxWidth().glass().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Titles(ui)
                        Timeline(ui, position, actions)
                        Transport(ui, actions, 72.dp)
                    }
                }
            }
        }
        // Secondary actions live in their own floating capsule.
        LeftToRight {
            Row(
                Modifier.padding(top = 8.dp, bottom = 8.dp).fillMaxWidth().glass(CircleShape),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val accent = MaterialTheme.colorScheme.primary
                ShuffleButton(ui, actions, accent)
                FavoriteButton(ui, actions, FavoriteTint)
                QueueButton(ui, actions, accent)
                LyricsButton(ui, actions, accent)
                VisualizerButton(ui, actions, accent, stageVisible = ui.panel == PlayerPanel.NONE)
                RepeatButton(ui, actions, accent)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SpeedMenu(ui, actions)
            SleepMenu(ui, actions)
        }
    }
}

/** Landscape: a stage pane and a side pane above a full-width control dock. */
@Composable
internal fun GlassLandscapePlayer(ui: PlayerUi, position: Position, actions: PlayerActions) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("player_landscape")) {
        val sidePane = maxWidth >= 640.dp
        val lyrics = ui.panel == PlayerPanel.LYRICS
        val sideWidth = if (lyrics) min(maxWidth * 0.4f, 420.dp) else min(maxWidth * 0.32f, 340.dp)
        // On narrow screens a panel takes the place of the disc.
        val stageVisible = sidePane || ui.panel == PlayerPanel.NONE
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight().glass()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BackButton(actions)
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                            val accent = MaterialTheme.colorScheme.primary
                            // With a side pane the queue is already on screen.
                            if (!sidePane) QueueButton(ui, actions, accent)
                            LyricsButton(ui, actions, accent)
                            VisualizerButton(ui, actions, accent, stageVisible)
                        }
                        OrientationControls(ui, actions, isLandscape = true)
                    }
                    val body = Modifier.weight(1f).fillMaxWidth()
                    when {
                        stageVisible -> LandscapeStage(ui, actions, body)
                        lyrics -> LyricsPanel(ui, position, actions, body)
                        else -> QueuePanel(ui, actions, body)
                    }
                }
                if (sidePane) {
                    val pane = Modifier.width(sideWidth).fillMaxHeight().glass()
                    if (lyrics) LyricsPanel(ui, position, actions, pane) else QueuePanel(ui, actions, pane)
                }
            }
            LeftToRight {
                Row(
                    Modifier.fillMaxWidth().glass().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShuffleButton(ui, actions, MaterialTheme.colorScheme.primary)
                    PreviousButton(actions, 28.dp)
                    Play(ui, actions, 52.dp)
                    NextButton(actions, 28.dp)
                    RepeatButton(ui, actions, MaterialTheme.colorScheme.primary)
                    Timeline(ui, position, actions, Modifier.weight(1f).padding(horizontal = 12.dp))
                    FavoriteButton(ui, actions, FavoriteTint)
                }
            }
        }
    }
}

@Composable
private fun LandscapeStage(ui: PlayerUi, actions: PlayerActions, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val disc = min(maxHeight - 12.dp, maxWidth * 0.4f).coerceAtLeast(48.dp)
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ui.visualizer.enabled) {
                VisualizerStage(ui, actions, Modifier.width(disc * 1.3f).height(disc))
            } else {
                Disc(ui, disc)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Titles(ui)
                Row {
                    SpeedMenu(ui, actions)
                    SleepMenu(ui, actions)
                }
            }
        }
    }
}

@Composable
private fun Transport(ui: PlayerUi, actions: PlayerActions, size: Dp) = LeftToRight {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviousButton(actions)
        Play(ui, actions, size)
        NextButton(actions)
    }
}

private val FavoriteTint = Color(0xFFFF5C9D)

/** Round artwork that turns while music plays and holds its angle when paused. */
@Composable
private fun Disc(ui: PlayerUi, size: Dp) {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(ui.isPlaying) {
        if (ui.isPlaying) {
            angle.snapTo(angle.value % 360f)
            angle.animateTo(
                angle.value + 360f,
                infiniteRepeatable(tween(24_000, easing = LinearEasing)),
            )
        }
    }
    Box(
        Modifier.size(size).glass(CircleShape).padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = angle.value }.clip(CircleShape)) {
            FadingArtwork(ui, Modifier.fillMaxSize().clip(CircleShape))
        }
        Box(
            Modifier.size(size * 0.12f).clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
        )
    }
}

@Composable
private fun Titles(ui: PlayerUi) {
    Text(
        ui.title, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
    Text(
        if (ui.album.isEmpty()) artistLabel(ui.artist) else "${artistLabel(ui.artist)} · ${ui.album}",
        Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
    if (ui.hasError) {
        Text(stringResource(ThemingR.string.player_error), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun Timeline(ui: PlayerUi, position: Position, actions: PlayerActions, modifier: Modifier = Modifier) =
    SeekBar(
        ui, position, actions,
        color = MaterialTheme.colorScheme.primary,
        track = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
        modifier = modifier,
    )

@Composable
private fun Play(ui: PlayerUi, actions: PlayerActions, size: Dp) = PlayButton(
    ui, actions, size, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary,
)
