package com.amusic.features.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amusic.core.common.GroupKind
import com.amusic.core.common.Song
import com.amusic.core.common.SortOrder
import com.amusic.core.design.SongRow
import com.amusic.core.design.artistLabel
import com.amusic.data.PlaylistItem

private data class MenuAction(val label: Int, val run: () -> Unit)

@Composable
fun LibraryScreen(viewModel: LibraryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val browse = state.browse
    val content = state.content

    var adding by remember { mutableStateOf<List<Song>?>(null) }
    var creating by remember { mutableStateOf<List<Song>?>(null) }
    var info by remember { mutableStateOf<Song?>(null) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val inDetail = browse.groupKey != null || browse.playlistId != null
    BackHandler(enabled = inDetail) { viewModel.back() }
    BackHandler(enabled = selection.isNotEmpty()) { viewModel.clearSelection() }

    val visible = when (content) {
        is Content.Songs -> content.songs
        is Content.PlaylistSongs -> content.items.map { it.song }
        else -> emptyList()
    }
    val selected = visible.filter { it.id in selection }.distinctBy { it.id }

    fun songActions(song: Song) = listOf(
        MenuAction(R.string.action_play_next) { viewModel.playNext(listOf(song)) },
        MenuAction(R.string.action_add_to_queue) { viewModel.addToQueue(listOf(song)) },
        if (song.id in favorites) {
            MenuAction(R.string.action_unfavorite) { viewModel.setFavorite(listOf(song), false) }
        } else {
            MenuAction(R.string.action_favorite) { viewModel.setFavorite(listOf(song), true) }
        },
        MenuAction(R.string.action_add_to_playlist) { adding = listOf(song) },
        MenuAction(R.string.action_info) { info = song },
    )

    Column(Modifier.fillMaxSize()) {
        if (selection.isNotEmpty()) {
            SelectionBar(
                count = selected.size,
                onClose = viewModel::clearSelection,
                actions = listOf(
                    MenuAction(R.string.action_select_all) { viewModel.select(visible.map { it.id }) },
                    MenuAction(R.string.action_play_next) {
                        viewModel.playNext(selected); viewModel.clearSelection()
                    },
                    MenuAction(R.string.action_add_to_queue) {
                        viewModel.addToQueue(selected); viewModel.clearSelection()
                    },
                    MenuAction(R.string.action_favorite) {
                        viewModel.setFavorite(selected, true); viewModel.clearSelection()
                    },
                    MenuAction(R.string.action_add_to_playlist) { adding = selected },
                ),
            )
        } else {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (inDetail) {
                    IconButton(onClick = viewModel::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
                Text(
                    state.detailTitle?.ifEmpty { null }
                        ?: if (browse.groupKey != null) stringResource(R.string.unknown)
                        else stringResource(R.string.library_title),
                    Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (browse.playlistId != null) {
                    OverflowMenu(
                        listOf(
                            MenuAction(R.string.playlist_rename) { renaming = true },
                            MenuAction(R.string.playlist_delete) { deleting = true },
                        ),
                    )
                }
            }
        }
        if (!inDetail) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Section.entries) { section ->
                    FilterChip(
                        selected = section == browse.section,
                        onClick = { viewModel.show(section) },
                        label = { Text(stringResource(section.label)) },
                    )
                }
            }
        }
        OutlinedTextField(
            browse.query, viewModel::search,
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            label = { Text(stringResource(R.string.library_search)) },
            singleLine = true,
            trailingIcon = {
                if (browse.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.search("") }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.action_clear_search))
                    }
                }
            },
        )
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (visible.isNotEmpty()) {
                TextButton(onClick = { viewModel.play(visible, 0) }) {
                    Text(stringResource(R.string.action_play_all))
                }
                TextButton(onClick = { viewModel.shuffle(visible) }) {
                    Text(stringResource(R.string.action_shuffle))
                }
            }
            if (browse.section == Section.PLAYLISTS && browse.playlistId == null) {
                TextButton(onClick = { creating = emptyList() }) {
                    Text(stringResource(R.string.playlist_new))
                }
            }
            Box(Modifier.weight(1f))
            if (browse.section.sortable) SortMenu(sortOrder, viewModel::setSortOrder)
        }

        LazyColumn(Modifier.fillMaxSize()) {
            when (content) {
                Content.Loading -> Unit
                is Content.Songs -> {
                    if (content.songs.isEmpty()) emptyMessage()
                    itemsIndexed(content.songs, key = { _, s -> s.id }) { index, song ->
                        SongRow(
                            song,
                            onClick = {
                                if (selection.isEmpty()) viewModel.play(content.songs, index)
                                else viewModel.toggleSelected(song.id)
                            },
                            onLongClick = { viewModel.toggleSelected(song.id) },
                            selected = song.id in selection,
                            trailing = { OverflowMenu(songActions(song)) },
                        )
                    }
                }
                is Content.Groups -> {
                    if (content.groups.isEmpty()) emptyMessage()
                    items(content.groups, key = { it.key }) { group ->
                        val name = group.name.ifEmpty {
                            if (browse.section.group == GroupKind.ARTIST) artistLabel("")
                            else stringResource(R.string.unknown)
                        }
                        val count = group.songs.size
                        ListRow(name, pluralStringResource(R.plurals.song_count, count, count)) {
                            viewModel.openGroup(group.key)
                        }
                    }
                }
                is Content.Playlists -> {
                    if (content.playlists.isEmpty()) emptyMessage()
                    items(content.playlists, key = { it.id }) { playlist ->
                        val count = playlist.songCount
                        ListRow(playlist.name, pluralStringResource(R.plurals.song_count, count, count)) {
                            viewModel.openPlaylist(playlist.id)
                        }
                    }
                }
                is Content.PlaylistSongs -> {
                    if (content.items.isEmpty()) emptyMessage()
                    itemsIndexed(content.items, key = { _, i -> i.entry.entryId }) { index, item ->
                        SongRow(
                            item.song,
                            onClick = { viewModel.play(visible, index) },
                            trailing = {
                                OverflowMenu(
                                    playlistActions(
                                        viewModel, content.items, index, browse.query.isEmpty(),
                                    ) + songActions(item.song),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    adding?.let { songs ->
        PickPlaylistDialog(
            playlists,
            onDismiss = { adding = null },
            onPick = { viewModel.addToPlaylist(it, songs); viewModel.clearSelection() },
            onNew = { creating = songs; adding = null },
        )
    }
    creating?.let { songs ->
        NameDialog(stringResource(R.string.playlist_new), "", onDismiss = { creating = null }) {
            viewModel.createPlaylist(it, songs)
            viewModel.clearSelection()
        }
    }
    info?.let { InfoDialog(it) { info = null } }
    browse.playlistId?.let { id ->
        val name = state.detailTitle.orEmpty()
        if (renaming) {
            NameDialog(stringResource(R.string.playlist_rename), name, { renaming = false }) {
                viewModel.renamePlaylist(id, it)
            }
        }
        if (deleting) {
            ConfirmDeleteDialog(name, { deleting = false }) { viewModel.deletePlaylist(id) }
        }
    }
}

/** Moving is offered only on the unfiltered list, where neighbours are real neighbours. */
private fun playlistActions(
    viewModel: LibraryViewModel,
    items: List<PlaylistItem>,
    index: Int,
    canMove: Boolean,
): List<MenuAction> = buildList {
    val item = items[index]
    if (canMove && index > 0) {
        add(MenuAction(R.string.action_move_up) { viewModel.swap(item, items[index - 1]) })
    }
    if (canMove && index < items.lastIndex) {
        add(MenuAction(R.string.action_move_down) { viewModel.swap(item, items[index + 1]) })
    }
    add(MenuAction(R.string.action_remove_from_playlist) { viewModel.removeFromPlaylist(item) })
}

private fun androidx.compose.foundation.lazy.LazyListScope.emptyMessage() = item {
    Text(stringResource(R.string.library_empty), Modifier.padding(24.dp))
}

@Composable
private fun ListRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OverflowMenu(actions: List<MenuAction>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(stringResource(action.label)) },
                    onClick = { open = false; action.run() },
                )
            }
        }
    }
}

@Composable
private fun SelectionBar(count: Int, onClose: () -> Unit, actions: List<MenuAction>) {
    Row(
        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, stringResource(R.string.action_cancel))
        }
        Text(
            pluralStringResource(R.plurals.selected_count, count, count),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
        )
        OverflowMenu(actions)
    }
}

@Composable
private fun SortMenu(current: SortOrder, onSelect: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val labels = mapOf(
        SortOrder.TITLE to R.string.sort_title,
        SortOrder.ARTIST to R.string.sort_artist,
        SortOrder.ALBUM to R.string.sort_album,
        SortOrder.DATE_ADDED to R.string.sort_date,
    )
    Box {
        TextButton(onClick = { open = true }) {
            Text(stringResource(R.string.sort_by, stringResource(labels.getValue(current))))
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            labels.forEach { (order, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = { open = false; onSelect(order) },
                )
            }
        }
    }
}
