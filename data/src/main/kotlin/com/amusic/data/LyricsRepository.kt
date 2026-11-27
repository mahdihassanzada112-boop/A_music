package com.amusic.data

import android.content.Context
import android.net.Uri
import com.amusic.core.common.decodeLyricsFile
import com.amusic.core.database.LyricsDao
import com.amusic.core.database.LyricsEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

enum class ImportResult { IMPORTED, EMPTY, TOO_LARGE, UNREADABLE }

/** Lyrics the user typed or imported, stored on this device. */
@Singleton
class LyricsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: LyricsDao,
) {
    fun observe(songId: Long): Flow<LyricsEntity?> = dao.observe(songId)

    /** Replaces the text and resets the timing adjustment. Blank text removes the lyrics. */
    suspend fun save(songId: Long, content: String) {
        if (content.isBlank()) dao.delete(songId) else dao.upsert(LyricsEntity(songId, content, 0))
    }

    suspend fun setOffset(songId: Long, offsetMs: Long) =
        dao.setOffset(songId, offsetMs.coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS))

    suspend fun delete(songId: Long) = dao.delete(songId)

    /** Reads a file the user picked. Existing lyrics are replaced only on success. */
    suspend fun import(songId: Long, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                // One byte past the limit is enough to know the file is too big.
                val buffer = ByteArray(MAX_BYTES + 1)
                var size = 0
                while (size < buffer.size) {
                    val read = input.read(buffer, size, buffer.size - size)
                    if (read < 0) break
                    size += read
                }
                buffer.copyOf(size)
            }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } ?: return@withContext ImportResult.UNREADABLE
        if (bytes.size > MAX_BYTES) return@withContext ImportResult.TOO_LARGE
        val text = decodeLyricsFile(bytes)
        // Binary files contain NUL characters; lyrics never do.
        if ('\u0000' in text) return@withContext ImportResult.UNREADABLE
        if (text.isBlank()) return@withContext ImportResult.EMPTY
        save(songId, text)
        ImportResult.IMPORTED
    }

    private companion object {
        const val MAX_BYTES = 256 * 1024
        const val MAX_OFFSET_MS = 60_000L
    }
}
