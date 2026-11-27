package com.amusic.core.theming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.amusic.core.common.VisualizerColor
import com.amusic.core.common.VisualizerSettings
import com.amusic.core.common.VisualizerStyle

@Composable
fun LyricsEditorDialog(text: String, onChange: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lyrics_editor_title)) },
        text = {
            OutlinedTextField(
                text,
                onChange,
                Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 320.dp).testTag("lyrics_editor"),
                textStyle = TextStyle(textDirection = TextDirection.Content),
                supportingText = { Text(stringResource(R.string.lyrics_editor_hint)) },
            )
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun RemoveLyricsDialog(onRemove: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.lyrics_remove_confirm)) },
        confirmButton = { TextButton(onClick = onRemove) { Text(stringResource(R.string.lyrics_remove)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualizerDialog(settings: VisualizerSettings, onChange: (VisualizerSettings) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.visualizer_options)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.visualizer_style), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisualizerStyle.entries.forEach {
                        FilterChip(
                            selected = it == settings.style,
                            onClick = { onChange(settings.copy(style = it)) },
                            label = { Text(stringResource(styleName(it))) },
                        )
                    }
                }
                Text(stringResource(R.string.visualizer_color), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisualizerColor.entries.forEach {
                        FilterChip(
                            selected = it == settings.color,
                            onClick = { onChange(settings.copy(color = it)) },
                            label = { Text(stringResource(colorName(it))) },
                        )
                    }
                }
                Level(R.string.visualizer_intensity, settings.intensity, VisualizerSettings.INTENSITY) {
                    onChange(settings.copy(intensity = it))
                }
                Level(R.string.visualizer_speed, settings.speed, VisualizerSettings.SPEED) {
                    onChange(settings.copy(speed = it))
                }
                Level(R.string.visualizer_sensitivity, settings.sensitivity, VisualizerSettings.SENSITIVITY) {
                    onChange(settings.copy(sensitivity = it))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

/** Saved once the finger lifts, not on every movement. */
@Composable
private fun Level(label: Int, value: Float, range: ClosedFloatingPointRange<Float>, onDone: (Float) -> Unit) {
    var shown by remember(value) { mutableFloatStateOf(value) }
    Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
    Slider(shown, { shown = it }, valueRange = range, onValueChangeFinished = { onDone(shown) })
}

private fun styleName(style: VisualizerStyle) = when (style) {
    VisualizerStyle.BARS -> R.string.style_bars
    VisualizerStyle.CIRCULAR -> R.string.style_circular
    VisualizerStyle.WAVEFORM -> R.string.style_waveform
    VisualizerStyle.NEON -> R.string.style_neon
    VisualizerStyle.PARTICLES -> R.string.style_particles
    VisualizerStyle.FLUID -> R.string.style_fluid
    VisualizerStyle.DEPTH -> R.string.style_depth
    VisualizerStyle.RETRO -> R.string.style_retro
    VisualizerStyle.MINIMAL -> R.string.style_minimal
}

private fun colorName(color: VisualizerColor) = when (color) {
    VisualizerColor.THEME -> R.string.color_theme
    VisualizerColor.AMBER -> R.string.color_amber
    VisualizerColor.CYAN -> R.string.color_cyan
    VisualizerColor.MAGENTA -> R.string.color_magenta
    VisualizerColor.GREEN -> R.string.color_green
    VisualizerColor.RAINBOW -> R.string.color_rainbow
}
