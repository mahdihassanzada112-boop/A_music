package com.amusic.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore.Audio
import com.amusic.core.common.repairLegacyArabic
import com.amusic.core.database.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MediaStoreScanner @Inject constructor(@param:ApplicationContext private val context: Context) {

    /** Reads every music file indexed by Android. Throws if MediaStore cannot be queried. */
    fun scan(): List<SongEntity> {
        val modern = Build.VERSION.SDK_INT >= 30
        @Suppress("DEPRECATION")
        val pathColumn =
            if (Build.VERSION.SDK_INT >= 29) Audio.Media.RELATIVE_PATH else Audio.Media.DATA
        val columns = mutableListOf(
            Audio.Media._ID, Audio.Media.TITLE, Audio.Media.ARTIST, Audio.Media.ALBUM,
            Audio.Media.ALBUM_ID, Audio.Media.DURATION, Audio.Media.SIZE,
            Audio.Media.DATE_ADDED, Audio.Media.TRACK, Audio.Media.MIME_TYPE, pathColumn,
        )
        if (modern) columns.add(Audio.Media.GENRE)
        val legacyGenres = if (modern) emptyMap() else legacyGenres()

        val cursor = context.contentResolver.query(
            Audio.Media.EXTERNAL_CONTENT_URI,
            columns.toTypedArray(),
            "${Audio.Media.IS_MUSIC} != 0",
            null,
            null,
        ) ?: error("MediaStore returned no cursor")

        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val id = c.long(Audio.Media._ID)
                    val path = c.text(pathColumn)
                    add(
                        SongEntity(
                            id = id,
                            contentUri = ContentUris
                                .withAppendedId(Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
                            title = tag(c.text(Audio.Media.TITLE)),
                            artist = tag(c.text(Audio.Media.ARTIST)),
                            album = tag(c.text(Audio.Media.ALBUM)),
                            albumId = c.long(Audio.Media.ALBUM_ID),
                            genre = tag(
                                if (modern) c.text(Audio.Media.GENRE) else legacyGenres[id].orEmpty(),
                            ),
                            // RELATIVE_PATH is a folder; DATA is a full file path.
                            folder = if (Build.VERSION.SDK_INT >= 29) path.trimEnd('/')
                            else path.substringBeforeLast('/', ""),
                            durationMs = c.long(Audio.Media.DURATION),
                            sizeBytes = c.long(Audio.Media.SIZE),
                            dateAddedEpochSeconds = c.long(Audio.Media.DATE_ADDED),
                            track = c.long(Audio.Media.TRACK).toInt(),
                            mimeType = c.text(Audio.Media.MIME_TYPE),
                        ),
                    )
                }
            }
        }
    }

    private fun legacyGenres(): Map<Long, String> {
        val result = HashMap<Long, String>()
        context.contentResolver.query(
            Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(Audio.Genres._ID, Audio.Genres.NAME), null, null, null,
        )?.use { genres ->
            while (genres.moveToNext()) {
                val name = genres.getString(1) ?: continue
                context.contentResolver.query(
                    Audio.Genres.Members.getContentUri("external", genres.getLong(0)),
                    arrayOf(Audio.Genres.Members.AUDIO_ID), null, null, null,
                )?.use { members ->
                    while (members.moveToNext()) result[members.getLong(0)] = name
                }
            }
        }
        return result
    }

    private fun tag(raw: String): String =
        if (raw == "<unknown>") "" else repairLegacyArabic(raw).trim()

    private fun Cursor.text(column: String): String = getString(getColumnIndexOrThrow(column)).orEmpty()

    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
}
