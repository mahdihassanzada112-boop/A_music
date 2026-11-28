package com.amusic.features.customize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amusic.core.theming.R as ThemingR
import com.amusic.data.PresetRepository

/** A titled screen with a way back, scrolling as a whole. */
@Composable
internal fun Screen(
    title: String,
    tag: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(ThemingR.string.player_back))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().testTag(tag)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
internal fun Heading(text: Int) =
    Text(stringResource(text), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)

@Composable
internal fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodySmall)

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onCheckedChange = null)
    }
}

internal data class Saved(val id: Long, val name: String, val selected: Boolean)

/** The listener's own named items: tap to use one, or delete it after confirming. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SavedItems(items: List<Saved>, onUse: (Long) -> Unit, onDelete: (Long) -> Unit) {
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = item.selected,
                    onClick = { onUse(item.id) },
                    label = { Text(item.name) },
                )
                IconButton({ deleting = item.id }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.customize_delete_named, item.name))
                }
            }
        }
    }
    items.firstOrNull { it.id == deleting }?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text(stringResource(R.string.customize_delete_confirm, item.name)) },
            confirmButton = {
                TextButton(onClick = { onDelete(item.id); deleting = null }) {
                    Text(stringResource(R.string.customize_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(ThemingR.string.action_cancel)) }
            },
        )
    }
}

@Composable
internal fun NameDialog(title: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                name,
                { name = it.take(PresetRepository.MAX_NAME) },
                Modifier.testTag("profile_name"),
                label = { Text(stringResource(R.string.customize_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(ThemingR.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(ThemingR.string.action_cancel)) }
        },
    )
}
