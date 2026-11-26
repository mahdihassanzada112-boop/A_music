package com.amusic.core.audio

import android.content.ContentUris
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.amusic.core.common.Song

private val ALBUM_ART: Uri = Uri.parse("content://media/external/audio/albumart")

@OptIn(UnstableApi::class)
fun Song.toMediaItem(): MediaItem {
    val uri = Uri.parse(contentUri)
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        // The URI is stripped when items cross the session boundary; this copy survives.
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist.ifEmpty { null })
                .setAlbumTitle(album.ifEmpty { null })
                .setArtworkUri(ContentUris.withAppendedId(ALBUM_ART, albumId))
                .setDurationMs(durationMs)
                .build(),
        )
        .build()
}
