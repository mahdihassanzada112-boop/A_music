package com.amusic.app

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amusic.data.AudioPermission
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Launch, scan, play, skip and persist on a real Android system. */
@RunWith(AndroidJUnit4::class)
class PlaybackSmokeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext

    @Test
    fun scansAndPlaysLocalFiles() = runBlocking {
        grant(AudioPermission.name)
        if (Build.VERSION.SDK_INT < 29) grant("android.permission.WRITE_EXTERNAL_STORAGE")
        addTone("amusic_test_one")
        addTone("amusic_test_two")

        val access = EntryPointAccessors.fromApplication(context, TestAccess::class.java)
        val library = access.library()
        val player = access.player()

        ActivityScenario.launch(MainActivity::class.java).use {
            library.start()
            val songs = withTimeout(60_000) {
                library.songs.first { all -> all.count { it.title.startsWith("amusic_test") } == 2 }
            }.filter { it.title.startsWith("amusic_test") }.sortedBy { it.title }

            withContext(Dispatchers.Main) { player.play(songs, 0) }
            withTimeout(30_000) { player.state.first { it.isPlaying } }
            waitUntil { withContext(Dispatchers.Main) { player.positionMs() } > 1_000 }
            assertEquals(2, player.state.value.queue.size)

            withContext(Dispatchers.Main) { player.next() }
            withTimeout(10_000) { player.state.first { it.current?.songId == songs[1].id } }

            withContext(Dispatchers.Main) { player.togglePlay() }
            withTimeout(10_000) { player.state.first { !it.isPlaying } }

            waitUntil { access.playbackStore().load().index == 1 }
            assertEquals(songs.map { it.id }, access.playbackStore().load().songIds)
            val recent = withTimeout(10_000) { library.recentlyPlayed.first { it.size >= 2 } }
            assertTrue(recent.map { it.id }.containsAll(songs.map { it.id }))
        }
    }

    private suspend fun waitUntil(condition: suspend () -> Boolean) = withTimeout(30_000) {
        while (!condition()) delay(200)
    }

    private fun grant(permission: String) {
        val output = instrumentation.uiAutomation
            .executeShellCommand("pm grant ${context.packageName} $permission")
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }

    private fun addTone(name: String) {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$name.wav")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-wav")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)!!
            resolver.openOutputStream(uri)!!.use(::writeWav)
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            folder.mkdirs()
            val file = File(folder, "$name.wav")
            file.outputStream().use(::writeWav)
            MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
        }
    }

    /** Twenty seconds of silence: mono, 16-bit, 8 kHz. */
    private fun writeWav(out: OutputStream) {
        val rate = 8_000
        val dataSize = rate * 2 * 20
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVEfmt ".toByteArray())
        header.putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2)
        header.putShort(2).putShort(16).put("data".toByteArray()).putInt(dataSize)
        out.write(header.array())
        out.write(ByteArray(dataSize))
    }
}
