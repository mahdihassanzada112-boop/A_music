package com.amusic.core.common

import java.util.Locale

/** Formats milliseconds as m:ss or h:mm:ss. Negative values are treated as zero. */
fun formatDuration(millis: Long, locale: Locale = Locale.US): String {
    val total = millis.coerceAtLeast(0) / 1000
    val hours = total / 3600
    val minutes = total % 3600 / 60
    val seconds = total % 60
    return if (hours > 0) {
        String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(locale, "%d:%02d", minutes, seconds)
    }
}
