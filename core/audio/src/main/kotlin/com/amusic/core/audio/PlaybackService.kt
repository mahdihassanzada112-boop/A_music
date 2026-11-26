package com.amusic.core.audio

import android.app.PendingIntent
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.amusic.core.storage.PlaybackStateStore
import com.amusic.core.storage.SavedPlayback
import com.amusic.data.LibraryRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Owns the only player in the app. Every screen controls it through [PlayerController]. */
@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    @Inject lateinit var stateStore: PlaybackStateStore
    @Inject lateinit var library: LibraryRepository
    @Inject lateinit var sleepTimer: SleepTimer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var session: MediaSession? = null

    /** Saving is blocked until the old state is read, so it is never overwritten with nothing. */
    private var restored = false

    /** The song already counted in history, so pause/resume does not count it twice. */
    private var countedItem: MediaItem? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true, // handle audio focus and interruptions
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val builder = MediaSession.Builder(this, player).setCallback(SessionCallback())
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            builder.setSessionActivity(
                PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE),
            )
        }
        session = builder.build()

        player.addListener(listener)
        scope.launch { restore(player) }
        scope.launch {
            while (isActive) {
                delay(5_000)
                if (player.isPlaying) save(player)
            }
        }
        scope.launch {
            sleepTimer.endsAt.collectLatest { end ->
                if (end != null) {
                    delay(end - SystemClock.elapsedRealtime())
                    player.pause()
                    sleepTimer.cancel()
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.run {
            save(player)
            player.release()
            release()
        }
        session = null
        scope.cancel()
        super.onDestroy()
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                )
            ) {
                save(player)
            }
            // Repeating one song counts each pass.
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) countedItem = null
            val item = player.currentMediaItem
            if (player.isPlaying && item != null && item !== countedItem) {
                countedItem = item
                item.mediaId.toLongOrNull()?.let { id -> scope.launch { library.recordPlay(id) } }
            }
        }
    }

    private suspend fun restore(player: Player) {
        val saved = stateStore.load()
        val songs = library.songsByIds(saved.songIds)
        // Something may have been queued while we were loading; that wins.
        if (player.mediaItemCount == 0) {
            player.repeatMode = saved.repeatMode
            player.shuffleModeEnabled = saved.shuffle
            player.playbackParameters = PlaybackParameters(saved.speed)
            if (songs.isNotEmpty()) {
                val wanted = saved.songIds.getOrNull(saved.index)
                val sameSong = songs.indexOfFirst { it.id == wanted }
                val index = when {
                    songs.size == saved.songIds.size -> saved.index.coerceIn(songs.indices)
                    else -> sameSong.coerceAtLeast(0)
                }
                val position = if (songs[index].id == wanted) saved.positionMs else 0
                player.setMediaItems(songs.map { it.toMediaItem() }, index, position)
                player.prepare()
            }
        }
        restored = true
        save(player)
    }

    private fun save(player: Player) {
        if (!restored) return
        val state = SavedPlayback(
            songIds = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId }
                .mapNotNull { it.toLongOrNull() },
            index = player.currentMediaItemIndex,
            positionMs = player.currentPosition.coerceAtLeast(0),
            repeatMode = player.repeatMode,
            shuffle = player.shuffleModeEnabled,
            speed = player.playbackParameters.speed,
        )
        // NonCancellable: the final save in onDestroy must outlive the scope.
        scope.launch(NonCancellable) { stateStore.save(state) }
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            // Only device media may be queued; other apps cannot make us open arbitrary URIs.
            val resolved = mediaItems.mapNotNull { item ->
                val uri = item.requestMetadata.mediaUri
                if (uri?.scheme == "content" && uri.authority == "media") {
                    item.buildUpon().setUri(uri).build()
                } else {
                    null
                }
            }
            return Futures.immediateFuture(resolved)
        }
    }
}
