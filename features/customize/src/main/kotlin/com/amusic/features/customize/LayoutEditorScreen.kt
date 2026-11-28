package com.amusic.features.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.common.LandscapeLayout
import com.amusic.core.common.LandscapePanel
import com.amusic.core.theming.LocalPlayerTheme
import com.amusic.core.theming.R as ThemingR
import kotlin.math.roundToInt

/** Arranges the landscape player of the theme in use. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LayoutEditorScreen(onBack: () -> Unit, viewModel: LayoutEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var naming by rememberSaveable { mutableStateOf(false) }
    val current = state ?: return
    val layout = current.layout
    val theme = LocalPlayerTheme.current

    Screen(stringResource(ThemingR.string.layout_customize), "layout_editor", onBack) {
        Text(stringResource(R.string.layout_for_theme, stringResource(theme.name)))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = layout == null,
                onClick = viewModel::restoreThemeDesign,
                label = { Text(stringResource(R.string.layout_theme_design)) },
            )
            FilterChip(
                selected = layout?.columns?.size == 2,
                onClick = { viewModel.setColumns(2) },
                label = { Text(stringResource(R.string.layout_two_columns)) },
            )
            FilterChip(
                selected = layout?.columns?.size == 3,
                onClick = { viewModel.setColumns(3) },
                label = { Text(stringResource(R.string.layout_three_columns)) },
            )
        }

        if (layout == null) {
            Hint(stringResource(R.string.layout_theme_design_hint))
        } else {
            Hint(stringResource(R.string.layout_drag_hint))
            Board(layout, viewModel::change)

            Heading(R.string.layout_widths)
            layout.columns.forEachIndexed { index, column ->
                Amount(
                    stringResource(R.string.layout_column, index + 1),
                    column.width,
                    LandscapeLayout.WIDTH,
                ) { width -> viewModel.change { it.setWidth(index, width) } }
            }
            val tall = layout.columns.flatMap { it.panels }.filter { it.panel.flexible }
            if (tall.isNotEmpty()) {
                Heading(R.string.layout_heights)
                tall.forEach { slot ->
                    Amount(stringResource(panelName(slot.panel)), slot.size, LandscapeLayout.SIZE) { size ->
                        viewModel.change { it.setSize(slot.panel, size) }
                    }
                }
                Hint(stringResource(R.string.layout_heights_hint))
            }

            Heading(R.string.layout_buttons)
            SwitchRow(stringResource(R.string.layout_favorite), layout.favorite) { on ->
                viewModel.change { it.copy(favorite = on) }
            }
            SwitchRow(stringResource(ThemingR.string.player_shuffle), layout.shuffle) { on ->
                viewModel.change { it.copy(shuffle = on) }
            }
            SwitchRow(stringResource(ThemingR.string.player_repeat), layout.repeat) { on ->
                viewModel.change { it.copy(repeat = on) }
            }
            SwitchRow(stringResource(R.string.layout_animated_background), layout.animatedBackground) { on ->
                viewModel.change { it.copy(animatedBackground = on) }
            }
            Hint(stringResource(R.string.layout_animated_hint))
        }

        Heading(R.string.layout_profiles)
        SavedItems(
            current.profiles.map { Saved(it.id, it.name, it.layout == layout) },
            onUse = { id -> current.profiles.firstOrNull { it.id == id }?.let { viewModel.apply(it.layout) } },
            onDelete = viewModel::deleteProfile,
        )
        Button(onClick = { naming = true }, enabled = layout != null) {
            Text(stringResource(R.string.layout_save_profile))
        }
        Button(onClick = viewModel::restoreThemeDesign, enabled = layout != null) {
            Text(stringResource(R.string.layout_restore))
        }
    }
    if (naming) {
        NameDialog(
            stringResource(R.string.layout_save_profile),
            onSave = { viewModel.saveProfile(it); naming = false },
            onDismiss = { naming = false },
        )
    }
}

/** What is being dragged and where the finger is, in the coordinates of the whole window. */
private data class Drag(val panel: LandscapePanel, val at: Offset)

/** Positions of the board's parts, read when a panel is dropped. */
private class Places {
    var board: LayoutCoordinates? = null
    var tray: LayoutCoordinates? = null
    val columns = mutableMapOf<Int, LayoutCoordinates>()
    val cards = mutableMapOf<LandscapePanel, LayoutCoordinates>()

    /** The column under the finger, going by its horizontal position. */
    fun columnAt(at: Offset, count: Int): Int? {
        val area = board.bounds() ?: return null
        if (at.y < area.top || at.y > area.bottom) return null
        return (0 until count).firstOrNull { index ->
            columns[index].bounds()?.let { at.x >= it.left && at.x <= it.right } == true
        }
    }

    fun overTray(at: Offset) = tray.bounds()?.contains(at) == true

    private fun LayoutCoordinates?.bounds() = this?.takeIf { it.isAttached }?.boundsInRoot()

    /**
     * What dropping here does, or null for nothing. The screen is read now; the returned
     * change may run later and elsewhere.
     */
    fun drop(layout: LandscapeLayout, drag: Drag): ((LandscapeLayout) -> LandscapeLayout)? {
        val panel = drag.panel
        val column = columnAt(drag.at, layout.columns.size)
        if (column == null) {
            if (!overTray(drag.at)) return null
            return { it.hide(panel) }
        }
        // It lands below every card whose middle is above the finger.
        val index = layout.columns[column].panels.count { slot ->
            slot.panel != drag.panel &&
                cards[slot.panel].bounds()?.let { it.center.y < drag.at.y } == true
        }
        return { it.place(panel, column, index) }
    }
}

/** A small picture of the screen: its columns, their panels and the hidden ones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Board(layout: LandscapeLayout, onChange: ((LandscapeLayout) -> LandscapeLayout) -> Unit) {
    var drag by remember { mutableStateOf<Drag?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val places = remember { Places() }
    val change by rememberUpdatedState(onChange)
    val shown by rememberUpdatedState(layout)

    @Composable
    fun card(panel: LandscapePanel, modifier: Modifier) {
        var own by remember { mutableStateOf<LayoutCoordinates?>(null) }
        PanelCard(
            panel,
            layout,
            onChange,
            modifier
                .alpha(if (drag?.panel == panel) 0.35f else 1f)
                .onGloballyPositioned {
                    own = it
                    places.cards[panel] = it
                }
                .pointerInput(panel) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { start ->
                            own?.takeIf { it.isAttached }?.let { drag = Drag(panel, it.localToRoot(start)) }
                        },
                        onDrag = { event, amount ->
                            event.consume()
                            drag = drag?.let { it.copy(at = it.at + amount) }
                        },
                        onDragEnd = {
                            drag?.let { dropped -> places.drop(shown, dropped)?.let { change(it) } }
                            drag = null
                        },
                        onDragCancel = { drag = null },
                    )
                },
        )
    }

    Box(Modifier.fillMaxWidth().onGloballyPositioned { origin = it.positionInRoot() }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val target = drag?.let { places.columnAt(it.at, layout.columns.size) }
            Row(
                Modifier.fillMaxWidth().onGloballyPositioned { places.board = it },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                layout.columns.forEachIndexed { index, column ->
                    val edge =
                        if (index == target) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    Column(
                        // Widths are hinted at only, so that every card stays readable.
                        Modifier.weight(1f + (column.width - 1f) * 0.3f).heightIn(min = 220.dp)
                            .onGloballyPositioned { places.columns[index] = it }
                            .border(if (index == target) 2.dp else 1.dp, edge, MaterialTheme.shapes.small)
                            .testTag("editor_column_$index").padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.layout_column, index + 1),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        column.panels.forEach { slot ->
                            androidx.compose.runtime.key(slot.panel) { card(slot.panel, Modifier.fillMaxWidth()) }
                        }
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().onGloballyPositioned { places.tray = it }.testTag("editor_hidden"),
            ) {
                Text(stringResource(R.string.layout_hidden), style = MaterialTheme.typography.titleSmall)
                if (layout.hidden.isEmpty()) Hint(stringResource(R.string.layout_hidden_none))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    layout.hidden.forEach { panel ->
                        androidx.compose.runtime.key(panel) { card(panel, Modifier.size(150.dp, 56.dp)) }
                    }
                }
            }
        }
        // The dragged panel follows the finger.
        drag?.let { moving ->
            Surface(
                Modifier.align(AbsoluteAlignment.TopLeft).offset {
                    val at = moving.at - origin
                    IntOffset((at.x - 60.dp.toPx()).roundToInt(), (at.y - 24.dp.toPx()).roundToInt())
                }.size(120.dp, 48.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                shadowElevation = 8.dp,
            ) {
                Box(contentAlignment = Alignment.Center) { Text(stringResource(panelName(moving.panel))) }
            }
        }
    }
}

/** One panel on the board; its menu does everything dragging does. */
@Composable
private fun PanelCard(
    panel: LandscapePanel,
    layout: LandscapeLayout,
    onChange: ((LandscapeLayout) -> LandscapeLayout) -> Unit,
    modifier: Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val name = stringResource(panelName(panel))
    val column = layout.columnOf(panel)
    Surface(
        modifier.testTag("editor_card_${panel.name}"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
            Box {
                val label = stringResource(R.string.layout_move_named, name)
                IconButton({ open = true }, Modifier.size(40.dp).testTag("editor_menu_${panel.name}")) {
                    Icon(Icons.Filled.MoreVert, label)
                }
                DropdownMenu(open, onDismissRequest = { open = false }) {
                    fun act(change: (LandscapeLayout) -> LandscapeLayout) {
                        open = false
                        onChange(change)
                    }
                    if (column >= 0) {
                        val panels = layout.columns[column].panels
                        val index = panels.indexOfFirst { it.panel == panel }
                        if (index > 0) {
                            Choice(stringResource(ThemingR.string.queue_up)) {
                                act { it.place(panel, column, index - 1) }
                            }
                        }
                        if (index < panels.lastIndex) {
                            Choice(stringResource(ThemingR.string.queue_down)) {
                                act { it.place(panel, column, index + 1) }
                            }
                        }
                    }
                    layout.columns.indices.filter { it != column }.forEach { other ->
                        Choice(stringResource(R.string.layout_move_to, other + 1)) {
                            act { it.place(panel, other, Int.MAX_VALUE) }
                        }
                    }
                    if (column >= 0) {
                        Choice(stringResource(R.string.layout_hide)) { act { it.hide(panel) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun Choice(label: String, onClick: () -> Unit) =
    DropdownMenuItem(text = { Text(label) }, onClick = onClick)

/** A share of width or height, stored when the finger lifts. */
@Composable
private fun Amount(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onDone: (Float) -> Unit,
) {
    var shown by remember(value) { mutableFloatStateOf(value) }
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            shown,
            { shown = it },
            Modifier.semantics { contentDescription = label },
            valueRange = range,
            onValueChangeFinished = { onDone(shown) },
        )
    }
}

private fun panelName(panel: LandscapePanel) = when (panel) {
    LandscapePanel.ARTWORK -> R.string.panel_artwork
    LandscapePanel.INFO -> R.string.panel_info
    LandscapePanel.PROGRESS -> R.string.panel_progress
    LandscapePanel.CONTROLS -> R.string.panel_controls
    LandscapePanel.LYRICS -> ThemingR.string.player_lyrics
    LandscapePanel.VISUALIZER -> ThemingR.string.player_visualizer
    LandscapePanel.QUEUE -> ThemingR.string.player_queue
    LandscapePanel.EQUALIZER -> ThemingR.string.player_equalizer
}
