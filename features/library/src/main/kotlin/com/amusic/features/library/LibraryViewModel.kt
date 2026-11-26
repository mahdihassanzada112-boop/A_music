package com.amusic.features.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amusic.core.audio.PlayerController
import com.amusic.core.common.GroupKind
import com.amusic.core.common.Song
import com.amusic.core.common.SongGroup
import com.amusic.core.common.SortOrder
import com.amusic.core.common.duplicates
import com.amusic.core.common.grouped
import com.amusic.core.common.isInDownloads
import com.amusic.core.common.normalizeForSearch
import com.amusic.core.common.search
import com.amusic.core.common.sortedBy
import com.amusic.core.database.PlaylistSummary
import com.amusic.core.storage.SettingsRepository
import com.amusic.data.LibraryRepository
import com.amusic.data.PlaylistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Section(val label: Int, val group: GroupKind? = null) {
    SONGS(R.string.section_songs),
    ALBUMS(R.string.section_albums, GroupKind.ALBUM),
    ARTISTS(R.string.section_artists, GroupKind.ARTIST),
    FOLDERS(R.string.section_folders, GroupKind.FOLDER),
    GENRES(R.string.section_genres, GroupKind.GENRE),
    FAVORITES(R.string.section_favorites),
    RECENTLY_PLAYED(R.string.section_recently_played),
    RECENTLY_ADDED(R.string.section_recently_added),
    PLAYLISTS(R.string.section_playlists),
    DOWNLOADS(R.string.section_downloads),
    DUPLICATES(R.string.section_duplicates);

    val sortable get() = this == SONGS || this == FAVORITES || this == DOWNLOADS
}

/** What the user is looking at. [groupKey] and [playlistId] open a detail list. */
data class Browse(
    val section: Section = Section.SONGS,
    val query: String = "",
    val groupKey: String? = null,
    val playlistId: Long? = null,
)

sealed interface Content {
    data object Loading : Content
    data class Songs(val songs: List<Song>) : Content
    data class Groups(val groups: List<SongGroup>) : Content
    data class Playlists(val playlists: List<PlaylistSummary>) : Content
    data class PlaylistSongs(val items: List<PlaylistItem>) : Content
}

data class LibraryState(
    val browse: Browse = Browse(),
    /** Name of the open group or playlist. */
    val detailTitle: String? = null,
    val content: Content = Content.Loading,
)

private data class Source(
    val all: List<Song>,
    val favorites: List<Song>,
    val recent: List<Song>,
    val playlists: List<PlaylistSummary>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
    private val player: PlayerController,
) : ViewModel() {
    private val browse = MutableStateFlow(Browse())

    private val _selection = MutableStateFlow(emptySet<Long>())
    val selection = _selection.asStateFlow()

    val sortOrder = settings.sortOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SortOrder.TITLE)

    val favoriteIds = library.favoriteIds.map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists = library.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val source = combine(
        library.songs, library.favorites, library.recentlyPlayed, library.playlists,
    ) { all, favorites, recent, lists -> Source(all, favorites, recent, lists) }

    private val openPlaylist = browse.map { it.playlistId }.flatMapLatest { id ->
        if (id == null) flowOf(null) else library.playlistItems(id).map { id to it }
    }

    val state = combine(browse, source, settings.sortOrder, openPlaylist) { b, s, order, open ->
            build(b, s, order, open)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    private fun build(
        b: Browse,
        s: Source,
        order: SortOrder,
        open: Pair<Long, List<PlaylistItem>>?,
    ): LibraryState {
        if (b.section == Section.PLAYLISTS) {
            if (b.playlistId == null) {
                val q = normalizeForSearch(b.query)
                return LibraryState(
                    b, null,
                    Content.Playlists(s.playlists.filter { q in normalizeForSearch(it.name) }),
                )
            }
            val name = s.playlists.firstOrNull { it.id == b.playlistId }?.name
            val items = open?.takeIf { it.first == b.playlistId }?.second
                ?: return LibraryState(b, name, Content.Loading)
            val matching = items.map { it.song }.search(b.query).mapTo(HashSet()) { it.id }
            return LibraryState(b, name, Content.PlaylistSongs(items.filter { it.song.id in matching }))
        }
        val kind = b.section.group
        if (kind != null) {
            if (b.groupKey == null) {
                return LibraryState(b, null, Content.Groups(s.all.search(b.query).grouped(kind)))
            }
            val group = s.all.grouped(kind).firstOrNull { it.key == b.groupKey }
            return LibraryState(b, group?.name, Content.Songs(group?.songs.orEmpty().search(b.query)))
        }
        val songs = when (b.section) {
            Section.FAVORITES -> s.favorites.sortedBy(order)
            Section.RECENTLY_PLAYED -> s.recent
            Section.RECENTLY_ADDED -> s.all.sortedBy(SortOrder.DATE_ADDED).take(100)
            Section.DOWNLOADS -> s.all.filter { it.isInDownloads() }.sortedBy(order)
            Section.DUPLICATES -> s.all.duplicates().flatten()
            else -> s.all.sortedBy(order)
        }
        return LibraryState(b, null, Content.Songs(songs.search(b.query)))
    }

    fun show(section: Section) {
        browse.value = Browse(section)
        clearSelection()
    }

    fun search(query: String) = browse.update { it.copy(query = query) }

    fun openGroup(key: String) {
        browse.update { it.copy(groupKey = key, query = "") }
        clearSelection()
    }

    fun openPlaylist(id: Long) = browse.update { it.copy(playlistId = id, query = "") }

    fun back() {
        browse.update { it.copy(groupKey = null, playlistId = null, query = "") }
        clearSelection()
    }

    fun toggleSelected(id: Long) = _selection.update { if (id in it) it - id else it + id }

    fun select(ids: List<Long>) {
        _selection.value = ids.toSet()
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settings.setSortOrder(order) }
    }

    fun play(songs: List<Song>, index: Int) = player.play(songs, index)
    fun shuffle(songs: List<Song>) = player.shufflePlay(songs)
    fun playNext(songs: List<Song>) = player.playNext(songs)
    fun addToQueue(songs: List<Song>) = player.addToQueue(songs)

    fun setFavorite(songs: List<Song>, favorite: Boolean) {
        viewModelScope.launch { library.setFavorite(songs.map { it.id }, favorite) }
    }

    fun createPlaylist(name: String, songs: List<Song> = emptyList()) {
        viewModelScope.launch { library.createPlaylist(name, songs.map { it.id }) }
    }

    fun addToPlaylist(id: Long, songs: List<Song>) {
        viewModelScope.launch { library.addToPlaylist(id, songs.map { it.id }) }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { library.renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: Long) {
        back()
        viewModelScope.launch { library.deletePlaylist(id) }
    }

    fun removeFromPlaylist(item: PlaylistItem) {
        viewModelScope.launch { library.removeFromPlaylist(item.entry.entryId) }
    }

    fun swap(a: PlaylistItem, b: PlaylistItem) {
        viewModelScope.launch { library.swapInPlaylist(a, b) }
    }
}
