package com.amusic.core.theming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amusic.core.common.EqPreset
import com.amusic.core.common.EqualizerSettings

/** The equalizer's essentials, small enough to sit beside the player. */
@Composable
fun EqualizerPanel(ui: PlayerUi, actions: PlayerActions, modifier: Modifier = Modifier) {
    val settings = ui.equalizer
    Column(
        modifier.testTag("equalizer_panel").verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        EqualizerSwitch(settings.enabled) { actions.setEqualizer(settings.copy(enabled = it)) }
        EqualizerPresets(settings.preset) { actions.setEqualizer(settings.choose(it).copy(enabled = true)) }
        EqualizerSlider(stringResource(R.string.eq_bass), settings.bass, 0f..1f) {
            actions.setEqualizer(settings.copy(bass = it, enabled = true))
        }
        EqualizerSlider(stringResource(R.string.eq_width), settings.width, 0f..1f) {
            actions.setEqualizer(settings.copy(width = it, enabled = true))
        }
        TextButton(onClick = actions::openEqualizer) {
            Text(stringResource(R.string.eq_all_bands), color = LocalContentColor.current)
        }
    }
}

@Composable
fun EqualizerSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.eq_enable), Modifier.weight(1f))
        Switch(checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerPresets(selected: EqPreset, onSelect: (EqPreset) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EqPreset.entries.forEach { preset ->
            FilterChip(
                selected = preset == selected,
                onClick = { onSelect(preset) },
                label = { Text(stringResource(equalizerPresetName(preset)), color = LocalContentColor.current) },
            )
        }
    }
}

/** Reports the value once the finger lifts, so storage is not written on every movement. */
@Composable
fun EqualizerSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    shownValue: (Float) -> String = { "${(it * 100).toInt()}%" },
    onDone: (Float) -> Unit,
) {
    var shown by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(shownValue(shown), style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            shown,
            { shown = it },
            Modifier.semantics { contentDescription = label },
            valueRange = range,
            onValueChangeFinished = { onDone(shown) },
        )
    }
}

private fun equalizerPresetName(preset: EqPreset) = when (preset) {
    EqPreset.NORMAL -> R.string.eq_normal
    EqPreset.ROCK -> R.string.eq_rock
    EqPreset.POP -> R.string.eq_pop
    EqPreset.CLASSICAL -> R.string.eq_classical
    EqPreset.JAZZ -> R.string.eq_jazz
    EqPreset.HIP_HOP -> R.string.eq_hip_hop
    EqPreset.ELECTRONIC -> R.string.eq_electronic
    EqPreset.ACOUSTIC -> R.string.eq_acoustic
    EqPreset.CUSTOM -> R.string.eq_custom
}
