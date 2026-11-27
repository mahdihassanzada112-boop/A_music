package com.amusic.core.common

/**
 * Audio is processed a fraction of a second before it is heard. This queue models the output
 * buffer as playing in real time, so each item is handed out when its sound is audible.
 * Times are any monotonic clock in microseconds. Thread safe.
 */
class PlayoutQueue<T : Any>(private val capacity: Int = 512) {
    private class Entry<T>(var dueUs: Long, val item: T)

    private val entries = ArrayDeque<Entry<T>>()
    private var endUs = 0L
    private var pausedAtUs: Long? = null
    private var current: T? = null

    @Synchronized
    fun add(item: T, durationUs: Long, nowUs: Long) {
        val clock = pausedAtUs ?: nowUs
        val due = maxOf(endUs, clock)
        endUs = due + durationUs
        entries.addLast(Entry(due, item))
        if (entries.size > capacity) entries.removeFirst()
    }

    /** The newest item that has become audible. */
    @Synchronized
    fun poll(nowUs: Long): T? {
        val clock = pausedAtUs ?: nowUs
        while (entries.isNotEmpty() && entries.first().dueUs <= clock) current = entries.removeFirst().item
        return current
    }

    @Synchronized
    fun setPlaying(playing: Boolean, nowUs: Long) {
        val pausedAt = pausedAtUs
        if (!playing && pausedAt == null) {
            pausedAtUs = nowUs
        } else if (playing && pausedAt != null) {
            // Buffered audio was held back for as long as the pause lasted.
            val shift = nowUs - pausedAt
            entries.forEach { it.dueUs += shift }
            endUs += shift
            pausedAtUs = null
        }
    }

    /** Drops everything buffered, as a seek does. */
    @Synchronized
    fun clear() {
        entries.clear()
        endUs = 0
        current = null
    }
}
