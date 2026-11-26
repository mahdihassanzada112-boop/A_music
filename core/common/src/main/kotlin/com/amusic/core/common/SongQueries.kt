package com.amusic.core.common

import java.text.Collator
import java.util.Locale
import kotlin.math.abs

enum class SortOrder {
    TITLE, ARTIST, ALBUM, DATE_ADDED;

    companion object {
        fun fromName(name: String?): SortOrder = entries.firstOrNull { it.name == name } ?: TITLE
    }
}

enum class GroupKind { ALBUM, ARTIST, FOLDER, GENRE }

data class SongGroup(val key: String, val name: String, val songs: List<Song>)

fun List<Song>.sortedBy(order: SortOrder, locale: Locale = Locale.getDefault()): List<Song> {
    val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
    val text = { selector: (Song) -> String ->
        Comparator<Song> { a, b -> collator.compare(selector(a), selector(b)) }
    }
    val byTitle = text { it.title }
    return sortedWith(
        when (order) {
            SortOrder.TITLE -> byTitle
            SortOrder.ARTIST -> text { it.artist }.then(byTitle)
            SortOrder.ALBUM -> text { it.album }.thenBy { it.track }.then(byTitle)
            SortOrder.DATE_ADDED -> compareByDescending<Song> { it.dateAddedEpochSeconds }.then(byTitle)
        },
    )
}

/** Every word of the query must appear in the title, artist or album. */
fun List<Song>.search(query: String): List<Song> {
    val words = normalizeForSearch(query).split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return this
    return filter { song ->
        val haystack = normalizeForSearch("${song.title} ${song.artist} ${song.album}")
        words.all { it in haystack }
    }
}

fun List<Song>.grouped(kind: GroupKind): List<SongGroup> =
    groupBy {
        when (kind) {
            GroupKind.ALBUM -> it.albumId.toString()
            GroupKind.ARTIST -> it.artist
            GroupKind.FOLDER -> it.folder
            GroupKind.GENRE -> it.genre
        }
    }.map { (key, songs) ->
        val first = songs.first()
        val name = when (kind) {
            GroupKind.ALBUM -> first.album
            GroupKind.ARTIST -> first.artist
            GroupKind.FOLDER -> first.folder.substringAfterLast('/')
            GroupKind.GENRE -> first.genre
        }
        val ordered =
            if (kind == GroupKind.ALBUM) songs.sortedWith(compareBy({ it.track }, { it.title }))
            else songs.sortedBy(SortOrder.TITLE)
        SongGroup(key, name, ordered)
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

fun Song.isInDownloads(): Boolean =
    folder.split('/').any { it.equals("Download", true) || it.equals("Downloads", true) }

/** Songs with the same normalized title and artist whose lengths differ by at most 2 s. */
fun List<Song>.duplicates(): List<List<Song>> =
    groupBy { normalizeForSearch(it.title) to normalizeForSearch(it.artist) }
        .values
        .flatMap { candidates ->
            val clusters = mutableListOf<MutableList<Song>>()
            for (song in candidates.sortedBy { it.durationMs }) {
                val last = clusters.lastOrNull()
                if (last != null && abs(song.durationMs - last.last().durationMs) <= 2_000) {
                    last.add(song)
                } else {
                    clusters.add(mutableListOf(song))
                }
            }
            clusters.filter { it.size > 1 }
        }
