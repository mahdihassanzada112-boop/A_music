package com.amusic.core.theming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amusic.core.common.LandscapeLayout
import com.amusic.core.common.LandscapePanel
import com.amusic.core.common.PanelSlot

/**
 * The listener's own landscape player. The bar on top cannot be removed, so there is always
 * a way back, to the settings and to the editor.
 */
@Composable
fun CustomLandscape(layout: LandscapeLayout, ui: PlayerUi, position: Position, actions: PlayerActions) {
    Column(
        Modifier.fillMaxSize().testTag("player_landscape").padding(horizontal = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth().testTag("custom_landscape"), verticalAlignment = Alignment.CenterVertically) {
            BackButton(actions)
            Text(
                stringResource(R.string.player_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            SpeedMenu(ui, actions)
            SleepMenu(ui, actions)
            EqualizerButton(ui, actions, MaterialTheme.colorScheme.primary)
            StudioButton(actions)
            OrientationControls(ui, actions, isLandscape = true)
        }
        // Columns without panels give their room to the others.
        val columns = layout.columns.filter { it.panels.isNotEmpty() }
        if (columns.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.layout_empty))
                TextButton(onClick = actions::openLayoutEditor) {
                    Text(stringResource(R.string.layout_customize), color = LocalContentColor.current)
                }
            }
        } else {
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                columns.forEach { column ->
                    val place = Modifier.weight(column.width).fillMaxHeight().clipToBounds()
                    if (column.panels.any { it.panel.flexible }) {
                        Column(place, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            column.panels.forEach { slot ->
                                key(slot.panel) {
                                    val height =
                                        if (slot.panel.flexible) Modifier.weight(slot.size) else Modifier
                                    Panel(slot, layout, ui, position, actions, height.fillMaxWidth())
                                }
                            }
                        }
                    } else {
                        // Fixed panels sit in the middle and scroll when they do not fit.
                        Box(place, contentAlignment = Alignment.Center) {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                column.panels.forEach { slot ->
                                    key(slot.panel) {
                                        Panel(slot, layout, ui, position, actions, Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(
    slot: PanelSlot,
    layout: LandscapeLayout,
    ui: PlayerUi,
    position: Position,
    actions: PlayerActions,
    modifier: Modifier,
) {
    val theme = LocalPlayerTheme.current
    val tagged = modifier.testTag("panel_${slot.panel.name}")
    val fill = Modifier.fillMaxSize()
    when (slot.panel) {
        LandscapePanel.ARTWORK, LandscapePanel.INFO, LandscapePanel.PROGRESS, LandscapePanel.CONTROLS ->
            theme.LandscapeBlock(slot.panel, ui, position, actions, layout, tagged)
        LandscapePanel.LYRICS -> theme.LandscapePane(tagged) { LyricsPanel(ui, position, actions, fill) }
        LandscapePanel.QUEUE -> theme.LandscapePane(tagged) { QueuePanel(ui, actions, fill) }
        LandscapePanel.VISUALIZER -> theme.LandscapePane(tagged) { VisualizerStage(ui, actions, fill) }
        LandscapePanel.EQUALIZER -> theme.LandscapePane(tagged) { EqualizerPanel(ui, actions, fill) }
    }
}

/**
 * Shuffle, previous, play, next, repeat and favorite, in one line where it fits and in two
 * where the column is narrow. [play] draws the theme's own play button.
 */
@Composable
fun LandscapeControls(
    ui: PlayerUi,
    actions: PlayerActions,
    layout: LandscapeLayout,
    favoriteTint: Color,
    modifier: Modifier,
    play: @Composable () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val transport: @Composable () -> Unit = {
        PreviousButton(actions, 28.dp)
        play()
        NextButton(actions, 28.dp)
    }
    LeftToRight {
        BoxWithConstraints(modifier) {
            if (maxWidth >= 300.dp) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (layout.shuffle) ShuffleButton(ui, actions, accent)
                    transport()
                    if (layout.repeat) RepeatButton(ui, actions, accent)
                    if (layout.favorite) FavoriteButton(ui, actions, favoriteTint)
                }
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    WrappingRow { transport() }
                    WrappingRow {
                        if (layout.shuffle) ShuffleButton(ui, actions, accent)
                        if (layout.favorite) FavoriteButton(ui, actions, favoriteTint)
                        if (layout.repeat) RepeatButton(ui, actions, accent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrappingRow(content: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
