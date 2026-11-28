package com.amusic.features.studio

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.common.AutoBackground
import com.amusic.core.common.BackgroundPalette
import com.amusic.core.common.BackgroundSettings
import com.amusic.core.common.BackgroundStyle
import com.amusic.core.common.BuiltInPreset
import com.amusic.core.common.LookSetting
import com.amusic.core.theming.ApplyTheme
import com.amusic.core.theming.BackgroundStage
import com.amusic.core.theming.LocalAudioFeed
import com.amusic.core.theming.LocalPlayerTheme
import com.amusic.core.theming.R as ThemingR
import com.amusic.data.BackgroundPreset
import com.amusic.data.PresetRepository

/** Builds the player's background: a live preview beside every control. */
@Composable
fun StudioScreen(onBack: () -> Unit, viewModel: StudioViewModel = hiltViewModel()) {
    val background by viewModel.background.collectAsStateWithLifecycle()
    val settings = background ?: return
    Column(Modifier.fillMaxSize().testTag("studio")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(ThemingR.string.player_back))
            }
            Text(
                stringResource(ThemingR.string.player_studio),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize()) {
                    Preview(settings, viewModel, Modifier.weight(0.45f).fillMaxHeight().padding(12.dp))
                    Controls(settings, viewModel, Modifier.weight(0.55f).fillMaxHeight())
                }
            } else {
                val previewHeight = maxHeight * 0.36f
                Column(Modifier.fillMaxSize()) {
                    Preview(settings, viewModel, Modifier.fillMaxWidth().height(previewHeight).padding(horizontal = 16.dp))
                    Controls(settings, viewModel, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

/** What the player's background looks like now. It moves here even while music is paused. */
@Composable
private fun Preview(settings: BackgroundSettings, viewModel: StudioViewModel, modifier: Modifier) {
    val song by viewModel.song.collectAsStateWithLifecycle()
    val (title, artwork) = song.first to song.second
    val theme = LocalPlayerTheme.current
    ApplyTheme(theme, dark = true) {
        CompositionLocalProvider(
            LocalAudioFeed provides viewModel.audioFeed,
            LocalContentColor provides Color.White,
        ) {
            Box(modifier.clip(MaterialTheme.shapes.large).clipToBounds().testTag("studio_preview")) {
                theme.Background(Modifier.matchParentSize())
                if (settings.enabled) {
                    BackgroundStage(settings, artwork, running = true, Modifier.matchParentSize())
                }
                Text(
                    if (settings.enabled) title ?: stringResource(R.string.studio_preview)
                    else stringResource(R.string.studio_off_hint),
                    Modifier.align(Alignment.BottomStart).padding(12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Controls(settings: BackgroundSettings, viewModel: StudioViewModel, modifier: Modifier) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var naming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    val scene = settings.scene

    Column(
        modifier.testTag("studio_controls").verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SwitchRow(stringResource(R.string.studio_enable), settings.enabled) { on ->
            viewModel.update { it.copy(enabled = on) }
        }

        Heading(R.string.studio_presets)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BuiltInPreset.entries.forEach { preset ->
                FilterChip(
                    selected = settings.enabled && scene == preset.scene,
                    onClick = { viewModel.editScene { preset.scene } },
                    label = { Text(stringResource(presetName(preset))) },
                )
            }
            presets.forEach { preset ->
                SavedChip(
                    preset,
                    selected = settings.enabled && scene == preset.scene,
                    onClick = { viewModel.editScene { preset.scene } },
                    onDelete = { deleting = preset.id },
                )
            }
        }
        OutlinedButton(onClick = { naming = "" }) { Text(stringResource(R.string.studio_save_preset)) }

        Heading(R.string.studio_layers)
        Text(stringResource(R.string.studio_layers_hint), style = MaterialTheme.typography.bodySmall)
        // The top layer of the picture is listed first.
        scene.layers.asReversed().forEach { layer ->
            LayerCard(
                layer,
                open = expanded == layer.style.name,
                isTop = layer.style == scene.layers.last().style,
                isBottom = layer.style == scene.layers.first().style,
                onToggleOpen = {
                    expanded = if (expanded == layer.style.name) null else layer.style.name
                },
                viewModel = viewModel,
            )
        }
        if (scene.layers.isEmpty()) Text(stringResource(R.string.studio_no_layers))
        Button(onClick = { adding = true }, enabled = scene.canAdd) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.studio_add_layer), Modifier.padding(start = 8.dp))
        }
        if (!scene.canAdd) {
            Text(stringResource(R.string.studio_layer_limit), style = MaterialTheme.typography.bodySmall)
        }

        Heading(R.string.studio_colors)
        PaletteChips(
            BackgroundPalette.entries - listOf(BackgroundPalette.AUTO, BackgroundPalette.CUSTOM).toSet(),
            scene.palette,
        ) { palette -> viewModel.editScene { it.copy(palette = palette) } }

        Heading(R.string.studio_settings)
        LookSetting.entries.forEach { setting ->
            LevelSlider(
                stringResource(settingName(setting)),
                scene.look[setting],
                setting.range,
                onDrag = { value -> viewModel.previewScene { it.copy(look = it.look.with(setting, value)) } },
                onDone = { value -> viewModel.editScene { it.copy(look = it.look.with(setting, value)) } },
            )
        }

        Heading(R.string.studio_auto)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutoBackground.entries.forEach { auto ->
                FilterChip(
                    selected = settings.auto == auto,
                    onClick = {
                        viewModel.update {
                            it.copy(auto = auto, enabled = it.enabled || auto != AutoBackground.OFF)
                        }
                    },
                    label = { Text(stringResource(autoName(auto))) },
                )
            }
        }
        Text(stringResource(R.string.studio_auto_hint), style = MaterialTheme.typography.bodySmall)

        Heading(R.string.studio_performance)
        SwitchRow(stringResource(R.string.studio_battery_saver), settings.batterySaver) { on ->
            viewModel.update { it.copy(batterySaver = on) }
        }
        Text(stringResource(R.string.studio_battery_hint), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.studio_fps))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackgroundSettings.FPS_CHOICES.forEach { fps ->
                FilterChip(
                    selected = settings.fpsLimit == fps,
                    onClick = { viewModel.update { it.copy(fpsLimit = fps) } },
                    label = { Text("%d".format(fps)) },
                )
            }
        }

        TextButton(onClick = viewModel::restoreDefaults) { Text(stringResource(R.string.studio_restore)) }
    }

    if (adding) {
        AddLayerDialog(
            scene.unused,
            onAdd = { style ->
                adding = false
                expanded = style.name
                viewModel.editScene { it.add(style) }
            },
            onDismiss = { adding = false },
        )
    }
    naming?.let { name ->
        AlertDialog(
            onDismissRequest = { naming = null },
            title = { Text(stringResource(R.string.studio_save_preset)) },
            text = {
                OutlinedTextField(
                    name,
                    { naming = it.take(PresetRepository.MAX_NAME) },
                    Modifier.testTag("preset_name"),
                    label = { Text(stringResource(R.string.studio_preset_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.savePreset(name); naming = null },
                    enabled = name.isNotBlank(),
                ) { Text(stringResource(ThemingR.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { naming = null }) { Text(stringResource(ThemingR.string.action_cancel)) }
            },
        )
    }
    presets.firstOrNull { it.id == deleting }?.let { preset ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text(stringResource(R.string.studio_delete_confirm, preset.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePreset(preset.id); deleting = null }) {
                    Text(stringResource(R.string.studio_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(ThemingR.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SavedChip(preset: BackgroundPreset, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(selected = selected, onClick = onClick, label = { Text(preset.name) })
        IconButton(onDelete) {
            Icon(Icons.Filled.Delete, stringResource(R.string.studio_delete_named, preset.name))
        }
    }
}

@Composable
private fun AddLayerDialog(styles: List<BackgroundStyle>, onAdd: (BackgroundStyle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.studio_add_layer)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                styles.forEach { style ->
                    TextButton(
                        onClick = { onAdd(style) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(styleName(style))) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(ThemingR.string.action_cancel)) }
        },
    )
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f).padding(end = 12.dp))
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
private fun Heading(text: Int) = Text(
    stringResource(text), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium,
)
