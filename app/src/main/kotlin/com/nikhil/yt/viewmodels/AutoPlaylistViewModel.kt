/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.nikhil.yt.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.nikhil.yt.constants.AutoPlaylistSongSortType
import com.nikhil.yt.constants.HideExplicitKey
import com.nikhil.yt.constants.HideVideoKey
import com.nikhil.yt.db.MusicDatabase
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.playback.DownloadUtil
import com.nikhil.yt.utils.ForYouSuggestionEngine
import com.nikhil.yt.utils.SyncUtils
import com.nikhil.yt.utils.autoPlaylistKeys
import com.nikhil.yt.utils.autoPlaylistSongs
import com.nikhil.yt.utils.dataStore
import com.nikhil.yt.utils.get
import com.nikhil.yt.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoPlaylistViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
    savedStateHandle: SavedStateHandle,
    private val syncUtils: SyncUtils,
    private val forYouEngine: ForYouSuggestionEngine,
) : ViewModel() {
    val playlist = savedStateHandle.get<String>("playlist")!!

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val likedSongs =
        autoPlaylistSongs(context, database, downloadUtil, playlist)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // "Suggested for you" under the Downloaded playlist: For You picks not downloaded yet.
    private val _suggestions = MutableStateFlow<List<SongItem>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    init {
        if (playlist == "downloaded") {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val suggested = forYouEngine.getSuggestions(
                        hideExplicit = context.dataStore.get(HideExplicitKey, false),
                        hideVideo = context.dataStore.get(HideVideoKey, false),
                        maxAgeMs = SUGGESTIONS_MAX_AGE_MS,
                    )
                    val downloads = downloadUtil.downloads.value
                    _suggestions.value = suggested
                        .filter { downloads[it.id]?.state != Download.STATE_COMPLETED }
                        .take(MAX_SUGGESTIONS)
                } catch (e: Exception) {
                    reportException(e)
                }
            }
        }
    }

    /** Freezes the playlist in [songIds] order and switches its sort to Custom. */
    fun saveCustomOrder(songIds: List<String>) {
        val keys = autoPlaylistKeys(playlist)
        viewModelScope.launch {
            context.dataStore.edit {
                it[keys.order] = songIds.joinToString(",")
                it[keys.sortType] = AutoPlaylistSongSortType.CUSTOM.name
            }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                when (playlist) {
                    "liked" -> syncUtils.syncLikedSongs()
                    else -> Unit
                }
            } catch (e: Exception) {
                reportException(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun syncLikedSongs() {
        refresh()
    }

    private companion object {
        const val MAX_SUGGESTIONS = 20
        const val SUGGESTIONS_MAX_AGE_MS = 10 * 60 * 1000L
    }
}
