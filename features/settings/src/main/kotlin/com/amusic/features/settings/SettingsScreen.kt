package com.amusic.features.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.common.ThemeMode
import com.amusic.core.design.ScreenScaffold

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val mode by viewModel.themeMode.collectAsStateWithLifecycle()
    // An empty tag means "follow the system language".
    val language = AppCompatDelegate.getApplicationLocales()[0]?.language.orEmpty()

    ScreenScaffold(stringResource(R.string.settings_title)) {
        ChoiceGroup(
            title = stringResource(R.string.settings_appearance),
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.settings_follow_system),
                ThemeMode.LIGHT to stringResource(R.string.settings_light),
                ThemeMode.DARK to stringResource(R.string.settings_dark),
            ),
            selected = mode,
            onSelect = viewModel::setThemeMode,
        )
        ChoiceGroup(
            title = stringResource(R.string.settings_language),
            options = listOf(
                "" to stringResource(R.string.settings_follow_system),
                "fa" to "فارسی",
                "en" to "English",
            ),
            selected = language,
            onSelect = {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it))
            },
        )
    }
}

@Composable
private fun <T> ChoiceGroup(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        options.forEach { (value, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(value == selected, role = Role.RadioButton) { onSelect(value) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = value == selected, onClick = null)
                Text(label, Modifier.padding(start = 12.dp))
            }
        }
    }
}
