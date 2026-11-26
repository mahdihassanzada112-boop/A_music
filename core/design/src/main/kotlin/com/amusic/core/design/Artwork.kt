package com.amusic.core.design

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 16).toInt()) {
    override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
}

/** Album art for a MediaStore album-art URI, with a music-note placeholder. */
@Composable
fun Artwork(uri: Uri?, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier.clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val context = LocalContext.current
        val limit = with(LocalDensity.current) { 640.dp.roundToPx() }
        val pixels = constraints.maxWidth.coerceIn(1, limit)
        val key = "$uri@$pixels"
        val bitmap by produceState(cache.get(key), key) {
            value = cache.get(key)
            if (value == null && uri != null) {
                value = withContext(Dispatchers.IO) { load(context, uri, pixels) }
                    ?.also { cache.put(key, it) }
            }
        }
        val image = bitmap
        if (image != null) {
            Image(
                image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box { Text("♪", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

private fun load(context: Context, uri: Uri, pixels: Int): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= 29) {
        val albums = android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val album = ContentUris.withAppendedId(albums, ContentUris.parseId(uri))
        context.contentResolver.loadThumbnail(album, Size(pixels, pixels), null)
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= pixels) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}.getOrNull() // a missing cover is normal
