package com.amusic.app

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import com.amusic.core.common.Song
import com.amusic.data.AudioPermission
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/** Permission, two sample files and access to the app's singletons. */
object DeviceFixture {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context: Context = instrumentation.targetContext
    val access: TestAccess by lazy {
        EntryPointAccessors.fromApplication(context, TestAccess::class.java)
    }
    private val names = listOf("amusic_test_one", "amusic_test_two")

    suspend fun prepareSongs(): List<Song> {
        grant(AudioPermission.name)
        if (Build.VERSION.SDK_INT < 29) grant("android.permission.WRITE_EXTERNAL_STORAGE")
        val library = access.library()
        library.start()
        val present = library.songs.first().map { it.title }.toSet()
        // Another test in this run may already have created them.
        names.filterNot { it in present }.forEach(::addTone)
        library.requestScan()
        return withTimeout(60_000) {
            library.songs.first { all -> names.all { name -> all.any { it.title == name } } }
        }.filter { it.title in names }.distinctBy { it.title }.sortedBy { it.title }
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

    /** Sixty seconds of a quiet 440 Hz tone: mono, 16-bit, 8 kHz. */
    private fun writeWav(out: OutputStream) {
        val rate = 8_000
        val dataSize = rate * 2 * 60
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVEfmt ".toByteArray())
        header.putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2)
        header.putShort(2).putShort(16).put("data".toByteArray()).putInt(dataSize)
        out.write(header.array())
        val samples = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        repeat(dataSize / 2) {
            samples.putShort((sin(2 * PI * 440 * it / rate) * 6_000).toInt().toShort())
        }
        out.write(samples.array())
    }
}
