package com.amusic.core.common

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset

/** [timeMs] is null in plain-text lyrics. */
data class LyricLine(val timeMs: Long?, val text: String)

data class ParsedLyrics(val lines: List<LyricLine>, val synced: Boolean) {
    /**
     * The line being sung at [positionMs], or -1 before the first one. A positive [offsetMs]
     * makes every line appear later.
     */
    fun activeIndex(positionMs: Long, offsetMs: Long = 0): Int {
        if (!synced) return -1
        val wanted = positionMs - offsetMs
        var low = 0
        var high = lines.lastIndex
        var found = -1
        while (low <= high) {
            val middle = (low + high) / 2
            if ((lines[middle].timeMs ?: 0) <= wanted) {
                found = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return found
    }
}

// [0-9] rather than \d: on Android \d would also accept digits that toInt() handles differently.
private val timeTag = Regex("""\[([0-9]{1,3}):([0-5]?[0-9])(?:[.:]([0-9]{1,3}))?]""")
private val wordTag = Regex("""<[0-9]{1,3}:[0-5]?[0-9](?:[.:][0-9]{1,3})?>""")
private val infoTag = Regex("""^\[(ar|ti|al|au|by|re|ve|length|offset|la|tool)\s*:(.*)]$""", RegexOption.IGNORE_CASE)

/** Reads LRC; text without any time tag is returned as plain lyrics. */
fun parseLyrics(source: String): ParsedLyrics {
    val timed = ArrayList<LyricLine>()
    val plain = ArrayList<LyricLine>()
    var fileOffset = 0L
    for (raw in source.removePrefix("﻿").lineSequence()) {
        val line = raw.trim()
        val info = infoTag.matchEntire(line)
        if (info != null) {
            if (info.groupValues[1].equals("offset", ignoreCase = true)) {
                fileOffset = info.groupValues[2].trim().removePrefix("+").toLongOrNull() ?: 0
            }
            continue
        }
        val times = ArrayList<Long>()
        var rest = line
        while (true) {
            val match = timeTag.find(rest)?.takeIf { it.range.first == 0 } ?: break
            val (minutes, seconds, fraction) = match.destructured
            times += minutes.toLong() * 60_000 + seconds.toLong() * 1_000 +
                if (fraction.isEmpty()) 0 else fraction.padEnd(3, '0').toLong()
            rest = rest.substring(match.range.last + 1).trimStart()
        }
        val text = rest.replace(wordTag, "").trim()
        if (times.isEmpty()) plain += LyricLine(null, text) else times.forEach { timed += LyricLine(it, text) }
    }
    if (timed.isEmpty()) {
        return ParsedLyrics(plain.dropWhile { it.text.isEmpty() }.dropLastWhile { it.text.isEmpty() }, false)
    }
    // In LRC a positive offset means the lyrics should show sooner.
    val lines = timed.map { it.copy(timeMs = (it.timeMs!! - fileOffset).coerceAtLeast(0)) }
        .sortedBy { it.timeMs }
    return ParsedLyrics(lines, true)
}

/** Decodes an imported text file: Unicode first, then the legacy Persian/Arabic code page. */
fun decodeLyricsFile(bytes: ByteArray): String {
    fun startsWith(vararg mark: Int) =
        bytes.size >= mark.size && mark.indices.all { bytes[it] == mark[it].toByte() }
    return when {
        startsWith(0xEF, 0xBB, 0xBF) -> String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        startsWith(0xFF, 0xFE) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        startsWith(0xFE, 0xFF) -> String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        else -> try {
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            String(bytes, Charset.forName("windows-1256"))
        }
    }
}
