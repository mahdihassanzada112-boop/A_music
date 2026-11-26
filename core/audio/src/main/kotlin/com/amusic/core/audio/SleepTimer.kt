package com.amusic.core.audio

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared by the UI and the playback service, which pauses when the deadline passes. */
@Singleton
class SleepTimer @Inject constructor() {
    private val _endsAt = MutableStateFlow<Long?>(null)

    /** Deadline on the [SystemClock.elapsedRealtime] clock, or null when off. */
    val endsAt: StateFlow<Long?> = _endsAt.asStateFlow()

    fun start(minutes: Int) {
        _endsAt.value = SystemClock.elapsedRealtime() + minutes * 60_000L
    }

    fun cancel() {
        _endsAt.value = null
    }
}
