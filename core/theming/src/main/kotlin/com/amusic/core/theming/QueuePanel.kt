package com.amusic.core.theming

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amusic.core.design.artistLabel

/** The queue in list order. With shuffle on, playback follows the player's shuffled order. */
@Composable
fun QueuePanel(ui: PlayerUi, actions: PlayerActions, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = ui.queueIndex)
    LazyColumn(modifier, state = listState) {
        itemsIndexed(ui.queue) { index, item ->
            val color =
                if (index == ui.queueIndex) MaterialTheme.colorScheme.primary else LocalContentColor.current
            Row(
                Modifier.fillMaxWidth().clickable { actions.jumpTo(index) }.padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(item.title, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        artistLabel(item.artist),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                var open by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { open = true }) {
                        Icon(Icons.Filled.MoreVert, stringResource(R.string.player_more))
                    }
                    DropdownMenu(open, onDismissRequest = { open = false }) {
                        if (index > 0) {
                            QueueAction(R.string.queue_up) { open = false; actions.move(index, index - 1) }
                        }
                        if (index < ui.queue.lastIndex) {
                            QueueAction(R.string.queue_down) { open = false; actions.move(index, index + 1) }
                        }
                        QueueAction(R.string.queue_remove) { open = false; actions.removeAt(index) }
                    }
                }
            }
        }
        item {
            TextButton(onClick = actions::clearQueue, Modifier.padding(8.dp)) {
                Text(stringResource(R.string.queue_clear))
            }
        }
    }
}

@Composable
private fun QueueAction(label: Int, onClick: () -> Unit) =
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick)
