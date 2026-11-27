package com.amusic.core.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import com.amusic.core.common.AudioFeed
import com.amusic.core.common.AudioFrame
import com.amusic.core.common.PlayoutQueue
import com.amusic.core.common.SpectrumAnalyzer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to a copy of the decoded audio on its way to the speakers. It never touches the
 * microphone, leaves the audio unchanged and does no work while nothing is watching.
 */
@Singleton
@OptIn(UnstableApi::class)
class AudioAnalyzer @Inject constructor() : AudioFeed {
    private val watchers = AtomicInteger()
    private val queue = PlayoutQueue<AudioFrame>()

    @Volatile private var speed = 1f
    @Volatile private var playing = false

    // The fields below belong to the playback thread.
    private var sampleRate = 0
    private var channels = 0
    private var encoding = C.ENCODING_INVALID
    private var analyzer: SpectrumAnalyzer? = null
    private val block = FloatArray(SpectrumAnalyzer.SIZE)
    private var filled = 0

    override fun watch(): AutoCloseable {
        if (watchers.getAndIncrement() == 0) queue.clear()
        var open = true
        return AutoCloseable {
            if (open) {
                open = false
                watchers.decrementAndGet()
            }
        }
    }

    override fun frame(): AudioFrame =
        if (!playing) AudioFrame.SILENCE else queue.poll(now()) ?: AudioFrame.SILENCE

    fun setPlaying(value: Boolean) {
        playing = value
        queue.setPlaying(value, now())
    }

    fun setSpeed(value: Float) {
        speed = value.coerceAtLeast(0.1f)
    }

    /** What the player's audio tap writes to. A member, so Media3 types stay inside this module. */
    internal val sink = object : TeeAudioProcessor.AudioBufferSink {
        override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) =
            configure(sampleRateHz, channelCount, encoding)

        override fun handleBuffer(buffer: ByteBuffer) = consume(buffer)
    }

    private fun configure(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        sampleRate = sampleRateHz
        channels = channelCount
        this.encoding = encoding
        analyzer = if (sampleRateHz > 0) SpectrumAnalyzer(sampleRateHz) else null
        filled = 0
        queue.clear()
    }

    private fun consume(buffer: ByteBuffer) {
        val analyzer = analyzer ?: return
        if (watchers.get() == 0 || channels <= 0) {
            filled = 0
            return
        }
        val bytes = when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_FLOAT -> 4
            else -> return
        }
        val data = buffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder())
        while (data.remaining() >= bytes * channels) {
            var sum = 0f
            repeat(channels) {
                sum += if (bytes == 2) data.short / 32_768f else data.float
            }
            block[filled++] = sum / channels
            if (filled == block.size) {
                filled = 0
                // Faster playback means each block is heard for a shorter time.
                val duration = (block.size * 1_000_000L / sampleRate / speed).toLong()
                queue.add(analyzer.analyze(block), duration, now())
            }
        }
    }

    private fun now() = System.nanoTime() / 1_000
}
