package com.amusic.core.common

/** Center frequencies of the equalizer's bands, in Hz. */
val EQ_FREQUENCIES = listOf(31, 62, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000)

/** Largest boost or cut of one band, in dB. */
const val EQ_LIMIT = 12f

/** Ready-made band settings in dB. CUSTOM stands for whatever the listener has set. */
enum class EqPreset(val bands: List<Float>) {
    NORMAL(listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    ROCK(listOf(5f, 4f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 4f)),
    POP(listOf(-1f, 0f, 2f, 4f, 5f, 4f, 2f, 0f, -1f, -1f)),
    CLASSICAL(listOf(4f, 3f, 2f, 1f, 0f, 0f, 0f, 1f, 3f, 4f)),
    JAZZ(listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f)),
    HIP_HOP(listOf(6f, 5f, 3f, 1f, -1f, -1f, 1f, 0f, 2f, 3f)),
    ELECTRONIC(listOf(5f, 4f, 1f, 0f, -2f, 1f, 0f, 1f, 4f, 5f)),
    ACOUSTIC(listOf(4f, 4f, 3f, 1f, 2f, 1f, 3f, 3f, 3f, 2f)),
    CUSTOM(List(10) { 0f });

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

data class EqualizerSettings(
    val enabled: Boolean = false,
    val preset: EqPreset = EqPreset.NORMAL,
    /** One gain per band in dB; follows [preset] unless that is CUSTOM. */
    val bands: List<Float> = EqPreset.NORMAL.bands,
    /** 0 to 1: extra weight below about 120 Hz. */
    val bass: Float = 0f,
    /** 0 to 1: how much wider stereo sound is spread. */
    val width: Float = 0f,
    /** Apply only while headphones or a Bluetooth audio device is connected. */
    val headphonesOnly: Boolean = false,
) {
    fun choose(preset: EqPreset) =
        if (preset == EqPreset.CUSTOM) copy(preset = preset) else copy(preset = preset, bands = preset.bands)

    /** Changing one band turns the current curve into a custom one. */
    fun withBand(index: Int, gain: Float) = copy(
        preset = EqPreset.CUSTOM,
        bands = bands.mapIndexed { i, old -> if (i == index) gain else old },
    ).clamped()

    fun clamped() = copy(
        bands = List(EQ_FREQUENCIES.size) { (bands.getOrNull(it) ?: 0f).safe(-EQ_LIMIT, EQ_LIMIT) },
        bass = bass.safe(0f, 1f),
        width = width.safe(0f, 1f),
    )
}

private fun Float.safe(low: Float, high: Float) = if (isNaN()) 0f else coerceIn(low, high)

/** Band gains as text, for storage. */
fun encodeBands(bands: List<Float>): String = bands.joinToString(",")

/** null unless [text] holds exactly one number per band. */
fun decodeBands(text: String?): List<Float>? {
    val values = text?.split(',')?.map { it.toFloatOrNull() ?: return null } ?: return null
    if (values.size != EQ_FREQUENCIES.size) return null
    return EqualizerSettings(bands = values).clamped().bands
}
