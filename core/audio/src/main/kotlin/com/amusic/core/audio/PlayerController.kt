package com.amusic.core.audio

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.amusic.core.common.Song
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The UI's single handle on playback. [state] mirrors the service's player, which stays the
 * only authority. All calls must come from the main thread.
 */
@Singleton
class PlayerController @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var future: ListenableFuture<MediaController>? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Connects early so a restored queue appears without the user doing anything. */
    fun connect() = command { }

    fun positionMs(): Long = connected()?.currentPosition?.coerceAtLeast(0) ?: 0

    fun play(songs: List<Song>, startIndex: Int = 0) = command {
        if (songs.isEmpty()) return@command
        it.setMediaItems(songs.map { song -> song.toMediaItem() }, startIndex, 0)
        it.prepare()
        it.play()
    }

    fun shufflePlay(songs: List<Song>) = command {
        if (songs.isEmpty()) return@command
        it.shuffleModeEnabled = true
        it.setMediaItems(songs.map { song -> song.toMediaItem() }, songs.indices.random(), 0)
        it.prepare()
        it.play()
    }

    /** With shuffle on, the player chooses where inserted items fall in the play order. */
    fun playNext(songs: List<Song>) = command {
        val at = if (it.mediaItemCount == 0) 0 else it.currentMediaItemIndex + 1
        it.addMediaItems(at, songs.map { song -> song.toMediaItem() })
        it.prepare()
    }

    fun addToQueue(songs: List<Song>) = command {
        it.addMediaItems(songs.map { song -> song.toMediaItem() })
        it.prepare()
    }

    @OptIn(UnstableApi::class)
    fun togglePlay() = command { Util.handlePlayPauseButtonAction(it) }

    fun next() = command { it.seekToNext() }

    fun previous() = command { it.seekToPrevious() }

    fun seekTo(positionMs: Long) = command { it.seekTo(positionMs) }

    fun jumpTo(index: Int) = command {
        if (index in 0 until it.mediaItemCount) {
            it.seekTo(index, 0)
            it.prepare()
            it.play()
        }
    }

    fun removeAt(index: Int) = command {
        if (index in 0 until it.mediaItemCount) it.removeMediaItem(index)
    }

    fun move(from: Int, to: Int) = command {
        if (from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) {
            it.moveMediaItem(from, to)
        }
    }

    fun clearQueue() = command { it.clearMediaItems() }

    fun toggleShuffle() = command { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = command {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) = command { it.playbackParameters = PlaybackParameters(speed) }

    private fun connected(): MediaController? =
        future?.takeIf { it.isDone && !it.isCancelled }
            ?.let { runCatching { it.get() }.getOrNull() }
            ?.takeIf { it.isConnected }

    private fun command(action: (MediaController) -> Unit) {
        scope.launch {
            try {
                action(awaitController())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PlayerController", "Playback command failed", e)
                future = null
                _state.value = _state.value.copy(hasError = true)
            }
        }
    }

    private suspend fun awaitController(): MediaController {
        connected()?.let { return it }
        val pending = future?.takeIf { !it.isDone } ?: build().also { future = it }
        return suspendCancellableCoroutine { continuation ->
            pending.addListener(
                {
                    try {
                        continuation.resume(pending.get())
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    private fun build(): ListenableFuture<MediaController> {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val result = MediaController.Builder(context, token)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    future = null
                    _state.value = PlayerState()
                }
            })
            .buildAsync()
        result.addListener(
            {
                val controller = runCatching { result.get() }.getOrNull() ?: return@addListener
                controller.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) = publish(
                        player,
                        queueChanged = events.contains(Player.EVENT_TIMELINE_CHANGED),
                    )
                })
                publish(controller, queueChanged = true)
            },
            ContextCompat.getMainExecutor(context),
        )
        return result
    }

    @OptIn(UnstableApi::class)
    private fun publish(player: Player, queueChanged: Boolean) {
        val metadata = player.currentMediaItem?.mediaMetadata
        val duration = player.duration.takeIf { it > 0 } ?: metadata?.durationMs ?: 0
        _state.value = PlayerState(
            // Rebuilding thousands of entries on every event would be wasteful.
            queue = if (queueChanged) queueOf(player) else _state.value.queue,
            index = player.currentMediaItemIndex,
            album = metadata?.albumTitle?.toString().orEmpty(),
            artworkUri = metadata?.artworkUri,
            durationMs = duration,
            isPlaying = player.isPlaying,
            showPause = !Util.shouldShowPlayButton(player),
            shuffle = player.shuffleModeEnabled,
            repeat = repeatSettingOf(player.repeatMode),
            speed = player.playbackParameters.speed,
            hasError = player.playerError != null,
        )
    }

    private fun queueOf(player: Player) = List(player.mediaItemCount) {
        val item = player.getMediaItemAt(it)
        QueueItem(
            item.mediaId.toLongOrNull() ?: -1,
            item.mediaMetadata.title?.toString().orEmpty(),
            item.mediaMetadata.artist?.toString().orEmpty(),
        )
    }
}
