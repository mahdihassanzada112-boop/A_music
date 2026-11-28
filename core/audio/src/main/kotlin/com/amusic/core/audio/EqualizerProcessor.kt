package com.amusic.core.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.amusic.core.common.EqualizerDsp
import com.amusic.core.common.EqualizerSettings
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The equalizer's switchboard: what is wanted and whether headphones are connected. */
@Singleton
class EqualizerControl @Inject constructor() {
    private val _headphones = MutableStateFlow(false)
    val headphones: StateFlow<Boolean> = _headphones.asStateFlow()

    @OptIn(UnstableApi::class)
    internal val processor = EqualizerProcessor()

    @OptIn(UnstableApi::class)
    internal fun apply(settings: EqualizerSettings, headphones: Boolean) {
        _headphones.value = headphones
        processor.set(settings, settings.enabled && (!settings.headphonesOnly || headphones))
    }
}

/** Runs [EqualizerDsp] on the decoded audio on its way to the speakers. */
@OptIn(UnstableApi::class)
internal class EqualizerProcessor : BaseAudioProcessor() {
    private class Wanted(val settings: EqualizerSettings, val active: Boolean)

    @Volatile private var wanted = Wanted(EqualizerSettings(), false)
    @Volatile private var dsp: EqualizerDsp? = null

    // Used by the playback thread only.
    private var format: AudioProcessor.AudioFormat? = null
    private var samples = FloatArray(0)

    fun set(settings: EqualizerSettings, active: Boolean) {
        wanted = Wanted(settings, active)
        dsp?.target(settings, active)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) inputAudioFormat
        else AudioProcessor.AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytes = inputBuffer.remaining()
        if (bytes == 0) return
        val dsp = dspFor(inputAudioFormat)
        val output = replaceOutputBuffer(bytes)
        if (dsp.bypassed) {
            output.put(inputBuffer)
        } else {
            val count = bytes / 2
            if (samples.size < count) samples = FloatArray(count)
            for (i in 0 until count) samples[i] = inputBuffer.short / 32_768f
            dsp.process(samples, count - count % inputAudioFormat.channelCount)
            for (i in 0 until count) {
                output.putShort((samples[i] * 32_768f).toInt().coerceIn(-32_768, 32_767).toShort())
            }
        }
        output.flip()
    }

    private fun dspFor(current: AudioProcessor.AudioFormat): EqualizerDsp {
        dsp?.let { if (current == format) return it }
        val created = EqualizerDsp(current.sampleRate, current.channelCount)
        val now = wanted
        created.target(now.settings, now.active)
        created.reset()
        format = current
        dsp = created
        // A change that arrived while building must not be lost.
        val latest = wanted
        if (latest !== now) created.target(latest.settings, latest.active)
        return created
    }
}
