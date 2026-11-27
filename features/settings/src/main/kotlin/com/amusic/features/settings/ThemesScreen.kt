package com.amusic.features.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.common.OrientationMode
import com.amusic.core.design.ScreenScaffold
import com.amusic.core.theming.PlayerTheme
import com.amusic.core.theming.R as ThemingR

/** Preview, choose and reset themes. Changing a theme never touches playback. */
@Composable
fun ThemesScreen(
    themes: List<PlayerTheme>,
    onBack: () -> Unit,
    viewModel: ThemesViewModel = hiltViewModel(),
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val orientation by viewModel.orientation.collectAsStateWithLifecycle()
    ScreenScaffold(stringResource(R.string.themes_title)) {
        TextButton(onClick = onBack) { Text(stringResource(ThemingR.string.player_back)) }
        themes.forEach { theme ->
            Text(stringResource(theme.name), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(theme.description))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThemePreview(theme, landscape = false)
                ThemePreview(theme, landscape = true)
            }
            if (theme.id == selected) {
                OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(R.string.theme_in_use)) }
            } else {
                Button(
                    onClick = { viewModel.select(theme.id) },
                    modifier = Modifier.testTag("use_${theme.id.name}"),
                ) { Text(stringResource(R.string.theme_use)) }
            }
        }
        ChoiceGroup(
            title = stringResource(R.string.settings_orientation),
            options = listOf(
                OrientationMode.AUTO to stringResource(ThemingR.string.orientation_auto),
                OrientationMode.PORTRAIT to stringResource(ThemingR.string.orientation_portrait),
                OrientationMode.LANDSCAPE to stringResource(ThemingR.string.orientation_landscape),
            ),
            selected = orientation,
            onSelect = viewModel::setOrientation,
        )
        OutlinedButton(onClick = viewModel::reset) { Text(stringResource(R.string.themes_reset)) }
    }
}
