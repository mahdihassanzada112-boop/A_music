package com.amusic.features.customize

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.common.EQ_FREQUENCIES
import com.amusic.core.common.EQ_LIMIT
import com.amusic.core.common.EqPreset
import com.amusic.core.theming.EqualizerPresets
import com.amusic.core.theming.EqualizerSlider
import com.amusic.core.theming.EqualizerSwitch
import com.amusic.core.theming.R as ThemingR

@Composable
fun EqualizerScreen(onBack: () -> Unit, viewModel: EqualizerViewModel = hiltViewModel()) {
    val equalizer by viewModel.equalizer.collectAsStateWithLifecycle()
    val curves by viewModel.curves.collectAsStateWithLifecycle()
    val headphones by viewModel.headphones.collectAsStateWithLifecycle()
    var naming by rememberSaveable { mutableStateOf(false) }
    val settings = equalizer ?: return

    Screen(stringResource(ThemingR.string.player_equalizer), "equalizer_screen", onBack) {
        EqualizerSwitch(settings.enabled) { on -> viewModel.update { it.copy(enabled = on) } }
        SwitchRow(stringResource(R.string.equalizer_headphones_only), settings.headphonesOnly) { on ->
            viewModel.update { it.copy(headphonesOnly = on) }
        }
        val status = when {
            !settings.enabled -> R.string.equalizer_status_off
            settings.headphonesOnly && !headphones -> R.string.equalizer_status_waiting
            else -> R.string.equalizer_status_on
        }
        Text(stringResource(status), Modifier.testTag("eq_status"))
        Hint(stringResource(R.string.equalizer_headphones_hint))

        Heading(R.string.equalizer_presets)
        EqualizerPresets(settings.preset) { preset ->
            viewModel.update { it.choose(preset).copy(enabled = true) }
        }

        Heading(R.string.equalizer_bands)
        EQ_FREQUENCIES.forEachIndexed { index, frequency ->
            EqualizerSlider(
                frequencyLabel(frequency),
                settings.bands[index],
                -EQ_LIMIT..EQ_LIMIT,
                shownValue = ::gainLabel,
            ) { gain -> viewModel.update { it.withBand(index, gain).copy(enabled = true) } }
        }

        EqualizerSlider(stringResource(ThemingR.string.eq_bass), settings.bass, 0f..1f) { value ->
            viewModel.update { it.copy(bass = value, enabled = true) }
        }
        EqualizerSlider(stringResource(ThemingR.string.eq_width), settings.width, 0f..1f) { value ->
            viewModel.update { it.copy(width = value, enabled = true) }
        }
        Hint(stringResource(R.string.equalizer_width_hint))

        Heading(R.string.equalizer_own)
        SavedItems(
            curves.map {
                Saved(it.id, it.name, settings.preset == EqPreset.CUSTOM && settings.bands == it.bands)
            },
            onUse = { id -> curves.firstOrNull { it.id == id }?.let(viewModel::use) },
            onDelete = viewModel::deleteCurve,
        )
        Button(onClick = { naming = true }) { Text(stringResource(R.string.equalizer_save)) }
        OutlinedButton(onClick = { viewModel.reset() }) { Text(stringResource(R.string.equalizer_reset)) }
        Hint(stringResource(R.string.equalizer_delay_hint))
    }
    if (naming) {
        NameDialog(
            stringResource(R.string.equalizer_save),
            onSave = { viewModel.saveCurve(it); naming = false },
            onDismiss = { naming = false },
        )
    }
}

private fun frequencyLabel(hertz: Int) =
    if (hertz < 1_000) "$hertz Hz" else "${hertz / 1_000} kHz"

private fun gainLabel(gain: Float) = "%+.1f dB".format(gain)
