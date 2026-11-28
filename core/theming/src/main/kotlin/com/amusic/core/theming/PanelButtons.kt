package com.amusic.core.theming

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

@Composable
private fun ToggleIcon(icon: Int, label: Int, on: Boolean, active: Color, onClick: () -> Unit) {
    val state = stringResource(if (on) R.string.state_on else R.string.state_off)
    IconButton(onClick, Modifier.semantics { stateDescription = state }) {
        Icon(
            painterResource(icon),
            stringResource(label),
            tint = if (on) active else LocalContentColor.current.copy(alpha = 0.7f),
        )
    }
}

@Composable
fun QueueButton(ui: PlayerUi, actions: PlayerActions, active: Color) =
    PanelToggle(ui, actions, PlayerPanel.QUEUE, R.drawable.ic_queue, R.string.player_queue, active)

@Composable
fun LyricsButton(ui: PlayerUi, actions: PlayerActions, active: Color) =
    PanelToggle(ui, actions, PlayerPanel.LYRICS, R.drawable.ic_lyrics, R.string.player_lyrics, active)

@Composable
private fun PanelToggle(
    ui: PlayerUi,
    actions: PlayerActions,
    panel: PlayerPanel,
    icon: Int,
    label: Int,
    active: Color,
) {
    val on = ui.panel == panel
    ToggleIcon(icon, label, on, active) { actions.showPanel(if (on) PlayerPanel.NONE else panel) }
}

/** Opens the Background Studio. */
@Composable
fun StudioButton(actions: PlayerActions) = IconButton(actions::openBackgroundStudio) {
    Icon(painterResource(R.drawable.ic_studio), stringResource(R.string.player_studio))
}

/**
 * Swaps the artwork for the visualizer. When a panel covers that area ([stageVisible] is
 * false), the button closes the panel and shows the visualizer.
 */
@Composable
fun VisualizerButton(ui: PlayerUi, actions: PlayerActions, active: Color, stageVisible: Boolean) {
    val on = ui.visualizer.enabled && stageVisible
    ToggleIcon(R.drawable.ic_visualizer, R.string.player_visualizer, on, active) {
        if (!stageVisible) actions.showPanel(PlayerPanel.NONE)
        actions.setVisualizer(ui.visualizer.copy(enabled = !on))
    }
}
