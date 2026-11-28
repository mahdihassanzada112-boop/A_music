package com.amusic.features.studio

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.amusic.core.common.BackgroundLayer
import com.amusic.core.common.BackgroundPalette
import com.amusic.core.common.BackgroundScene
import com.amusic.core.common.BackgroundStyle
import kotlin.math.roundToInt

/** One layer: always its name and switch; opened, all its own settings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LayerCard(
    layer: BackgroundLayer,
    open: Boolean,
    isTop: Boolean,
    isBottom: Boolean,
    onToggleOpen: () -> Unit,
    viewModel: StudioViewModel,
) {
    val name = stringResource(styleName(layer.style))
    fun edit(new: BackgroundLayer) = viewModel.editScene { it.update(new) }
    fun tryOut(new: BackgroundLayer) = viewModel.previewScene { it.update(new) }
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            val openState = stringResource(if (open) R.string.studio_expanded else R.string.studio_collapsed)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = 52.dp)
                        .clickable(role = Role.Button, onClick = onToggleOpen)
                        .semantics { stateDescription = openState },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                    )
                    Text(name, Modifier.padding(start = 8.dp))
                }
                val switchLabel = stringResource(R.string.studio_layer_on, name)
                Switch(
                    layer.enabled,
                    onCheckedChange = { edit(layer.copy(enabled = it)) },
                    Modifier.semantics { contentDescription = switchLabel },
                )
            }
            if (!open) return@Column
            LevelSlider(
                stringResource(R.string.layer_opacity), layer.opacity, BackgroundLayer.OPACITY,
                onDrag = { tryOut(layer.copy(opacity = it)) }, onDone = { edit(layer.copy(opacity = it)) },
            )
            LevelSlider(
                stringResource(
                    if (layer.style == BackgroundStyle.BLUR) R.string.layer_depth else R.string.layer_intensity,
                ),
                layer.intensity, BackgroundLayer.INTENSITY,
                onDrag = { tryOut(layer.copy(intensity = it)) }, onDone = { edit(layer.copy(intensity = it)) },
            )
            if (layer.style.animated) {
                LevelSlider(
                    stringResource(R.string.layer_speed), layer.speed, BackgroundLayer.SPEED,
                    onDrag = { tryOut(layer.copy(speed = it)) }, onDone = { edit(layer.copy(speed = it)) },
                )
            }
            Text(stringResource(R.string.studio_colors))
            PaletteChips(BackgroundPalette.entries, layer.palette) { edit(layer.copy(palette = it)) }
            if (layer.palette == BackgroundPalette.CUSTOM) {
                ColorPicker(
                    layer.color,
                    // A very bright solid color would hide the player's white text.
                    maxValue = if (layer.style == BackgroundStyle.SOLID) 0.6f else 1f,
                    onDrag = { tryOut(layer.copy(color = it)) },
                    onDone = { edit(layer.copy(color = it)) },
                )
            }
            FlowRow {
                TextButton(
                    onClick = { viewModel.editScene { it.move(layer.style, up = true) } },
                    enabled = !isTop,
                ) { Text(stringResource(R.string.layer_up)) }
                TextButton(
                    onClick = { viewModel.editScene { it.move(layer.style, up = false) } },
                    enabled = !isBottom,
                ) { Text(stringResource(R.string.layer_down)) }
                TextButton(onClick = { edit(BackgroundScene.defaultLayer(layer.style)) }) {
                    Text(stringResource(R.string.layer_reset))
                }
                TextButton(onClick = { viewModel.editScene { it.remove(layer.style) } }) {
                    Text(stringResource(R.string.layer_remove))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaletteChips(
    choices: List<BackgroundPalette>,
    selected: BackgroundPalette,
    onSelect: (BackgroundPalette) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { palette ->
            FilterChip(
                selected = palette == selected,
                onClick = { onSelect(palette) },
                label = { Text(stringResource(paletteName(palette))) },
            )
        }
    }
}

/** Follows the finger in the preview and is saved when it lifts. */
@Composable
internal fun LevelSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onDrag: (Float) -> Unit,
    onDone: (Float) -> Unit,
) {
    var shown by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, Modifier.weight(1f))
            Text("%d%%".format((shown * 100).roundToInt()), style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            shown,
            onValueChange = { shown = it; onDrag(it) },
            Modifier.semantics { contentDescription = label },
            valueRange = range,
            onValueChangeFinished = { onDone(shown) },
        )
    }
}

/** Hue, richness and lightness, with the result shown above. */
@Composable
private fun ColorPicker(color: Int, maxValue: Float, onDrag: (Int) -> Unit, onDone: (Int) -> Unit) {
    val start = remember { hsvOf(color) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2].coerceAtMost(maxValue)) }
    fun mixed() = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    // Follows a color changed elsewhere (a reset or a preset). The sliders' own colors are
    // left alone, so the hue is not lost at black or white.
    LaunchedEffect(color) {
        if (color != mixed()) {
            val outside = hsvOf(color)
            hue = outside[0]
            saturation = outside[1]
            value = outside[2].coerceAtMost(maxValue)
        }
    }
    Column {
        Box(
            Modifier.fillMaxWidth().height(32.dp).clip(MaterialTheme.shapes.small)
                .background(Color(mixed())),
        )
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0f, 35f, 140f, 190f, 230f, 280f, 330f).forEachIndexed { index, preset ->
                val label = stringResource(R.string.studio_color_swatch, index + 1)
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(Color.hsv(preset, 0.75f, maxValue.coerceAtMost(0.8f)))
                        .clickable(role = Role.Button) {
                            hue = preset
                            saturation = 0.75f
                            value = maxValue.coerceAtMost(0.5f)
                            onDone(mixed())
                        }
                        .semantics { contentDescription = label },
                )
            }
        }
        val drag = { onDrag(mixed()) }
        val done = { onDone(mixed()) }
        Channel(R.string.color_hue, hue, 0f..360f, { hue = it; drag() }, done)
        Channel(R.string.color_saturation, saturation, 0f..1f, { saturation = it; drag() }, done)
        Channel(R.string.color_lightness, value, 0f..maxValue, { value = it; drag() }, done)
    }
}

private fun hsvOf(color: Int) = FloatArray(3).also { AndroidColor.colorToHSV(color, it) }

@Composable
private fun Channel(
    label: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
) {
    val name = stringResource(label)
    Text(name)
    Slider(
        value, onChange,
        Modifier.semantics { contentDescription = name },
        valueRange = range,
        onValueChangeFinished = onDone,
    )
}
