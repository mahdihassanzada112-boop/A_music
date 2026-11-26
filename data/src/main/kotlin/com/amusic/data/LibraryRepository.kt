package com.amusic.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.amusic.core.common.Song
import com.amusic.core.database.FavoriteEntity
import com.amusic.core.database.LibraryDao
import com.amusic.core.database.PlaylistEntity
import com.amusic.core.database.PlaylistEntry
import com.amusic.core.database.PlaylistSummary
import com.amusic.core.database.SongDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ScanState { IDLE, SCANNING, FAILED }

data class PlaylistItem(val entry: PlaylistEntry, val song: Song)

@Singleton
class LibraryRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val scanner: MediaStoreScanner,
    private val songDao: SongDao,
    private val libraryDao: LibraryDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scanLock = Mutex()
    private var observing = false
    private var pendingScan: Job? = null

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val songs: Flow<List<Song>> = songDao.observeAll()
        .map { list -> list.map { it.toSong() } }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val favoriteIds: Flow<List<Long>> = libraryDao.observeFavoriteIds()
    val favorites: Flow<List<Song>> = combine(songs, favoriteIds) { all, ids -> inIdOrder(all, ids) }
    val recentlyPlayed: Flow<List<Song>> =
        combine(songs, libraryDao.observeRecentIds()) { all, ids -> inIdOrder(all, ids) }
    val playlists: Flow<List<PlaylistSummary>> = libraryDao.observePlaylists()

    fun playlistItems(id: Long): Flow<List<PlaylistItem>> =
        libraryDao.observePlaylistEntries(id)
            .map { entries -> entries.map { PlaylistItem(it, it.song.toSong()) } }

    /** Returns the songs that still exist, in the order of [ids] (repeats included). */
    suspend fun songsByIds(ids: List<Long>): List<Song> {
        val found = songDao.byIds(ids).associate { it.id to it.toSong() }
        return ids.mapNotNull { found[it] }
    }

    /** Scans now and again whenever MediaStore changes. Call once permission is granted. */
    @Synchronized
    fun start() {
        if (!AudioPermission.isGranted(context)) return
        if (!observing) {
            observing = true
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = requestScan(delayMs = 1_500)
            }
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer,
            )
        }
        requestScan()
    }

    /** Bursts of requests collapse into one scan. */
    @Synchronized
    fun requestScan(delayMs: Long = 0) {
        pendingScan?.cancel()
        pendingScan = scope.launch {
            delay(delayMs)
            // A separate job, so a newer request never cancels a scan half way.
            scope.launch { scanNow() }
        }
    }

    private suspend fun scanNow() = scanLock.withLock {
        // Without permission MediaStore looks empty, which would wipe the library.
        if (!AudioPermission.isGranted(context)) return@withLock
        _scanState.value = ScanState.SCANNING
        _scanState.value = try {
            songDao.replaceAll(scanner.scan())
            ScanState.IDLE
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("LibraryRepository", "Library scan failed", e)
            ScanState.FAILED
        }
    }

    suspend fun setFavorite(songIds: List<Long>, favorite: Boolean) {
        if (favorite) {
            val now = System.currentTimeMillis()
            libraryDao.addFavorites(songIds.map { FavoriteEntity(it, now) })
        } else {
            songIds.forEach { libraryDao.removeFavorite(it) }
        }
    }

    suspend fun recordPlay(songId: Long) = libraryDao.recordPlay(songId, System.currentTimeMillis())

    suspend fun createPlaylist(name: String, songIds: List<Long> = emptyList()) {
        val id = libraryDao.insertPlaylist(
            PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()),
        )
        if (songIds.isNotEmpty()) libraryDao.appendToPlaylist(id, songIds)
    }

    suspend fun addToPlaylist(playlistId: Long, songIds: List<Long>) =
        libraryDao.appendToPlaylist(playlistId, songIds)

    suspend fun renamePlaylist(id: Long, name: String) = libraryDao.renamePlaylist(id, name.trim())

    suspend fun deletePlaylist(id: Long) = libraryDao.deletePlaylist(id)

    suspend fun removeFromPlaylist(entryId: Long) = libraryDao.removeEntry(entryId)

    suspend fun swapInPlaylist(a: PlaylistItem, b: PlaylistItem) =
        libraryDao.swapEntries(a.entry, b.entry)

    private fun inIdOrder(songs: List<Song>, ids: List<Long>): List<Song> {
        val byId = songs.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
}
