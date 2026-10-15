/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.nikhil.yt.extensions

import com.nikhil.yt.db.entities.Song
import com.nikhil.yt.db.entities.Album
import kotlin.random.Random

fun <T> List<T>.reversed(reversed: Boolean) = if (reversed) asReversed() else this

fun <T> MutableList<T>.move(
    fromIndex: Int,
    toIndex: Int,
): MutableList<T> {
    add(toIndex, removeAt(fromIndex))
    return this
}

// Items missing from [order] (e.g. new downloads) come first, in their current order.
fun <T> List<T>.inCustomOrder(order: List<String>, id: (T) -> String): List<T> {
    val position = order.withIndex().associate { it.value to it.index }
    val (known, new) = partition { id(it) in position }
    return new + known.sortedBy { position.getValue(id(it)) }
}

// Shuffle that favours most-played items ~60/40 so least-played ones still surface.
fun <T> List<T>.playTimeMix(
    playTime: (T) -> Long,
    first: T? = null,
    random: Random = Random.Default,
): List<T> {
    val rest = filter { it != first }.sortedByDescending(playTime)
    val top = ArrayDeque(rest.take((rest.size + 1) / 2).shuffled(random))
    val bottom = ArrayDeque(rest.drop(top.size).shuffled(random))
    return buildList {
        first?.let { add(it) }
        while (top.isNotEmpty() || bottom.isNotEmpty()) {
            val fromTop = bottom.isEmpty() || (top.isNotEmpty() && random.nextFloat() < 0.6f)
            add(if (fromTop) top.removeFirst() else bottom.removeFirst())
        }
    }
}

fun <T : Any> List<T>.mergeNearbyElements(
    key: (T) -> Any = { it },
    merge: (first: T, second: T) -> T = { first, _ -> first },
): List<T> {
    if (isEmpty()) return emptyList()

    val mergedList = mutableListOf<T>()
    var currentItem = this[0]

    for (i in 1 until size) {
        val nextItem = this[i]
        if (key(currentItem) == key(nextItem)) {
            currentItem = merge(currentItem, nextItem)
        } else {
            mergedList.add(currentItem)
            currentItem = nextItem
        }
    }
    mergedList.add(currentItem)

    return mergedList
}

// Extension function to filter explicit content for local Song entities
fun List<Song>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filter { !it.song.explicit }
    } else {
        this
    }

// Extension function to filter explicit content for local Album entities
fun List<Album>.filterExplicitAlbums(enabled: Boolean = true) =
    if (enabled) {
        filter { !it.album.explicit }
    } else {
        this
    }

// No-op for local songs: local Song entities do not contain video metadata to filter reliably
fun List<Song>.filterVideo(enabled: Boolean = true) = this
