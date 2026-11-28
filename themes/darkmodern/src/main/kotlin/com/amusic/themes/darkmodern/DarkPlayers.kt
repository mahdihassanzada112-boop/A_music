package com.amusic.themes.darkmodern

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.amusic.core.common.LandscapeLayout
import com.amusic.core.common.LandscapePanel
import com.amusic.core.design.artistLabel
import com.amusic.core.theming.EqualizerButton
import com.amusic.core.theming.LandscapeControls
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
import com.amusic.core.theming.StudioButton
import com.amusic.core.theming.SleepMenu
import com.amusic.core.theming.SpeedMenu
import com.amusic.core.theming.R as ThemingR

/** Portrait: large square art, start-aligned titles, controls within thumb reach. */
@Composable
internal fun DarkPortraitPlayer(ui: PlayerUi, position: Position, actions: PlayerActions) {
    Column(Modifier.fillMaxSize().testTag("player_portrait")) {
        TopBar(ui, actions, isLandscape = false)
        when (ui.panel) {
            PlayerPanel.QUEUE -> QueuePanel(ui, actions, Modifier.weight(1f).fillMaxWidth())
            PlayerPanel.LYRICS -> {
                LyricsPanel(ui, position, actions, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp))
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Timeline(ui, position, actions)
                    Transport(ui, actions, 60.dp)
                }
            }
            PlayerPanel.NONE -> BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val art = min(maxWidth - 48.dp, maxHeight * 0.44f)
                val full = maxHeight
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .heightIn(min = full).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Stage(ui, actions, art)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Titles(ui, TextAlign.Start, Modifier.weight(1f))
                        FavoriteButton(ui, actions, MaterialTheme.colorScheme.primary)
                    }
                    Timeline(ui, position, actions)
                    Transport(ui, actions, 76.dp)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpeedMenu(ui, actions)
                        SleepMenu(ui, actions)
                        EqualizerButton(ui, actions, MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            val accent = MaterialTheme.colorScheme.primary
            QueueButton(ui, actions, accent)
            LyricsButton(ui, actions, accent)
            VisualizerButton(ui, actions, accent, stageVisible = ui.panel == PlayerPanel.NONE)
            StudioButton(actions)
        }
    }
}

/** Landscape: artwork, then information and controls, then a queue or lyrics column. */
@Composable
internal fun DarkLandscapePlayer(ui: PlayerUi, position: Position, actions: PlayerActions) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("player_landscape")) {
        // A third column only fits on wider screens; otherwise panels replace the artwork.
        val threeColumns = maxWidth >= 640.dp
        val lyrics = ui.panel == PlayerPanel.LYRICS
        val art = min(maxHeight - 32.dp, maxWidth * 0.3f)
        val panelWidth = maxWidth * 0.44f
        val sideWidth = if (lyrics) min(maxWidth * 0.34f, 380.dp) else min(maxWidth * 0.28f, 320.dp)
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                threeColumns || ui.panel == PlayerPanel.NONE -> Stage(ui, actions, art)
                lyrics -> LyricsPanel(ui, position, actions, Modifier.width(panelWidth).fillMaxHeight())
                else -> QueuePanel(ui, actions, Modifier.width(panelWidth).fillMaxHeight())
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TopBar(ui, actions, isLandscape = true)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Titles(ui, TextAlign.Start, Modifier.weight(1f))
                    FavoriteButton(ui, actions, MaterialTheme.colorScheme.primary)
                }
                Timeline(ui, position, actions)
                Transport(ui, actions, 60.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val accent = MaterialTheme.colorScheme.primary
                    // With a third column the queue is already on screen.
                    if (!threeColumns) QueueButton(ui, actions, accent)
                    LyricsButton(ui, actions, accent)
                    VisualizerButton(
                        ui, actions, accent,
                        stageVisible = threeColumns || ui.panel == PlayerPanel.NONE,
                    )
                    StudioButton(actions)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpeedMenu(ui, actions)
                    SleepMenu(ui, actions)
                    EqualizerButton(ui, actions, MaterialTheme.colorScheme.primary)
                }
            }
            if (threeColumns) {
                Column(
                    Modifier.width(sideWidth).fillMaxHeight()
                        .padding(vertical = 8.dp).clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Text(
                        stringResource(if (lyrics) ThemingR.string.player_lyrics else ThemingR.string.player_queue),
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val rest = Modifier.weight(1f).fillMaxWidth()
                    if (lyrics) LyricsPanel(ui, position, actions, rest) else QueuePanel(ui, actions, rest)
                }
            }
        }
    }
}

@Composable
internal fun DarkLandscapeBlock(
    panel: LandscapePanel,
    ui: PlayerUi,
    position: Position,
    actions: PlayerActions,
    layout: LandscapeLayout,
    modifier: Modifier,
) {
    when (panel) {
        LandscapePanel.ARTWORK -> BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            Art(ui, min(maxWidth, maxHeight))
        }
        LandscapePanel.INFO -> Titles(ui, TextAlign.Center, modifier)
        LandscapePanel.PROGRESS -> Box(modifier) { Timeline(ui, position, actions) }
        LandscapePanel.CONTROLS -> LandscapeControls(
            ui, actions, layout, MaterialTheme.colorScheme.primary, modifier,
        ) {
            PlayButton(
                ui, actions, 56.dp, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary,
            )
        }
        else -> Unit
    }
}

@Composable
internal fun DarkLandscapePane(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer)) {
        content()
    }
}

/** Artwork or, when chosen, the visualizer in the same square. */
@Composable
private fun Stage(ui: PlayerUi, actions: PlayerActions, size: Dp) {
    if (ui.visualizer.enabled) {
        VisualizerStage(
            ui, actions,
            Modifier.size(size).clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer).padding(8.dp),
        )
    } else {
        Art(ui, size)
    }
}

@Composable
private fun TopBar(ui: PlayerUi, actions: PlayerActions, isLandscape: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BackButton(actions)
        Text(
            stringResource(ThemingR.string.player_title),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        OrientationControls(ui, actions, isLandscape)
    }
}

/** The artwork shrinks slightly while paused. */
@Composable
private fun Art(ui: PlayerUi, size: Dp) {
    val scale by animateFloatAsState(if (ui.isPlaying) 1f else 0.9f, label = "art")
    Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        FadingArtwork(ui, Modifier.size(size))
    }
}

@Composable
private fun Titles(ui: PlayerUi, align: TextAlign, modifier: Modifier) {
    Column(modifier) {
        Text(
            ui.title, Modifier.fillMaxWidth(), textAlign = align,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            artistLabel(ui.artist), Modifier.fillMaxWidth(), textAlign = align,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (ui.album.isNotEmpty()) {
            Text(
                ui.album, Modifier.fillMaxWidth(), textAlign = align,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (ui.hasError) {
            Text(stringResource(ThemingR.string.player_error), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Timeline(ui: PlayerUi, position: Position, actions: PlayerActions) = SeekBar(
    ui, position, actions,
    color = MaterialTheme.colorScheme.primary,
    track = MaterialTheme.colorScheme.surfaceVariant,
)

@Composable
private fun Transport(ui: PlayerUi, actions: PlayerActions, playSize: Dp) = LeftToRight {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShuffleButton(ui, actions, accent)
        PreviousButton(actions)
        PlayButton(ui, actions, playSize, accent, MaterialTheme.colorScheme.onPrimary)
        NextButton(actions)
        RepeatButton(ui, actions, accent)
    }
}

