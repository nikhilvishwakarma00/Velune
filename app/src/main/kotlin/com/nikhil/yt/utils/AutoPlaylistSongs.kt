/*
 * Velune - by Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.media3.exoplayer.offline.Download
import com.nikhil.yt.R
import com.nikhil.yt.constants.AutoPlaylistSongSortDescendingKey
import com.nikhil.yt.constants.AutoPlaylistSongSortType
import com.nikhil.yt.constants.AutoPlaylistSongSortTypeKey
import com.nikhil.yt.constants.DownloadedSongOrderKey
import com.nikhil.yt.constants.DownloadedSongSortDescendingKey
import com.nikhil.yt.constants.DownloadedSongSortTypeKey
import com.nikhil.yt.constants.HideExplicitKey
import com.nikhil.yt.constants.HideVideoKey
import com.nikhil.yt.constants.LikedSongOrderKey
import com.nikhil.yt.constants.SongSortType
import com.nikhil.yt.db.MusicDatabase
import com.nikhil.yt.db.entities.Song
import com.nikhil.yt.extensions.filterExplicit
import com.nikhil.yt.extensions.inCustomOrder
import com.nikhil.yt.extensions.reversed
import com.nikhil.yt.extensions.toEnum
import com.nikhil.yt.playback.DownloadUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.Collator
import java.util.Locale

/** Sort settings of the "liked" and "downloaded" auto playlists, shared with Library > Songs. */
class AutoPlaylistKeys(
    val sortType: Preferences.Key<String>,
    val descending: Preferences.Key<Boolean>,
    val order: Preferences.Key<String>,
)

fun autoPlaylistKeys(playlist: String) =
    if (playlist == "downloaded") {
        AutoPlaylistKeys(DownloadedSongSortTypeKey, DownloadedSongSortDescendingKey, DownloadedSongOrderKey)
    } else {
        AutoPlaylistKeys(AutoPlaylistSongSortTypeKey, AutoPlaylistSongSortDescendingKey, LikedSongOrderKey)
    }

fun AutoPlaylistSongSortType.labelRes() =
    when (this) {
        AutoPlaylistSongSortType.CUSTOM -> R.string.sort_by_custom
        AutoPlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
        AutoPlaylistSongSortType.NAME -> R.string.sort_by_name
        AutoPlaylistSongSortType.ARTIST -> R.string.sort_by_artist
        AutoPlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
    }

private data class AutoPlaylistSettings(
    val sortType: AutoPlaylistSongSortType,
    val descending: Boolean,
    val order: List<String>,
    val hideExplicit: Boolean,
    val hideVideo: Boolean,
)

/**
 * Songs of the "liked" or "downloaded" playlist, sorted by its saved settings.
 * Custom order puts songs missing from the saved order (new likes/downloads) on top, newest first.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun autoPlaylistSongs(
    context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    playlist: String,
): Flow<List<Song>> {
    val keys = autoPlaylistKeys(playlist)
    return context.dataStore.data
        .map {
            AutoPlaylistSettings(
                sortType = it[keys.sortType].toEnum(AutoPlaylistSongSortType.CREATE_DATE),
                descending = it[keys.descending] ?: true,
                order = it[keys.order].orEmpty().split(','),
                hideExplicit = it[HideExplicitKey] ?: false,
                hideVideo = it[HideVideoKey] ?: false,
            )
        }.distinctUntilChanged()
        .flatMapLatest { settings ->
            val custom = settings.sortType == AutoPlaylistSongSortType.CUSTOM
            val songs = when (playlist) {
                "liked" -> database.likedSongs(
                    settings.sortType.toSongSortType(),
                    descending = custom || settings.descending,
                    settings.hideVideo,
                )
                "downloaded" -> downloadedSongs(database, downloadUtil, settings)
                else -> flowOf(emptyList())
            }
            songs.map { list ->
                (if (custom) list.inCustomOrder(settings.order) { it.id } else list)
                    .filterExplicit(settings.hideExplicit)
            }
        }
}

private fun downloadedSongs(
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    settings: AutoPlaylistSettings,
): Flow<List<Song>> =
    combine(
        downloadUtil.downloads,
        database.allSongs().flowOn(Dispatchers.IO),
        if (settings.hideVideo) database.videoSongIds() else flowOf(emptyList()),
    ) { downloads, songs, videoIds ->
        val hidden = videoIds.toSet()
        val downloadTime = { song: Song -> downloads[song.id]?.updateTimeMs ?: 0L }
        val downloaded = songs.filter {
            downloads[it.id]?.state == Download.STATE_COMPLETED && it.id !in hidden
        }
        val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
        when (settings.sortType) {
            AutoPlaylistSongSortType.CUSTOM -> downloaded.sortedByDescending(downloadTime)
            AutoPlaylistSongSortType.CREATE_DATE -> downloaded.sortedBy(downloadTime).reversed(settings.descending)
            AutoPlaylistSongSortType.NAME ->
                downloaded.sortedWith(compareBy(collator) { it.song.title }).reversed(settings.descending)
            AutoPlaylistSongSortType.ARTIST ->
                downloaded.sortedWith(compareBy(collator) { song -> song.artists.joinToString("") { it.name } })
                    .reversed(settings.descending)
            AutoPlaylistSongSortType.PLAY_TIME ->
                downloaded.sortedBy { it.song.totalPlayTime }.reversed(settings.descending)
        }
    }.flowOn(Dispatchers.Default)

private fun AutoPlaylistSongSortType.toSongSortType() =
    when (this) {
        AutoPlaylistSongSortType.CUSTOM, AutoPlaylistSongSortType.CREATE_DATE -> SongSortType.CREATE_DATE
        AutoPlaylistSongSortType.NAME -> SongSortType.NAME
        AutoPlaylistSongSortType.ARTIST -> SongSortType.ARTIST
        AutoPlaylistSongSortType.PLAY_TIME -> SongSortType.PLAY_TIME
    }
