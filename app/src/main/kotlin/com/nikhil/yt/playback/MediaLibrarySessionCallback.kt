/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.nikhil.yt.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.offline.Download
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.nikhil.yt.R
import com.nikhil.yt.extensions.toMediaItem
import com.nikhil.yt.innertube.YouTube
import com.nikhil.yt.innertube.models.AlbumItem
import com.nikhil.yt.innertube.models.ArtistItem
import com.nikhil.yt.innertube.models.PlaylistItem
import com.nikhil.yt.innertube.models.SongItem
import com.nikhil.yt.innertube.models.YTItem
import com.nikhil.yt.constants.MediaSessionConstants
import com.nikhil.yt.constants.SongSortType
import com.nikhil.yt.db.MusicDatabase
import com.nikhil.yt.db.entities.PlaylistEntity
import com.nikhil.yt.db.entities.Song
import com.nikhil.yt.extensions.toggleRepeatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.plus
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

class MediaLibrarySessionCallback
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) : MediaLibrarySession.Callback {
    private val scope = CoroutineScope(Dispatchers.Main) + Job()
    var toggleLike: () -> Unit = {}
    var toggleStartRadio: () -> Unit = {}
    var toggleLibrary: () -> Unit = {}

    private fun browsableExtras(
        browsableHint: Int = CONTENT_STYLE_LIST_ITEM,
        playableHint: Int = CONTENT_STYLE_LIST_ITEM,
    ) = Bundle().apply {
        putBoolean(EXTRA_CONTENT_STYLE_SUPPORTED, true)
        putInt(EXTRA_CONTENT_STYLE_BROWSABLE_HINT, browsableHint)
        putInt(EXTRA_CONTENT_STYLE_PLAYABLE_HINT, playableHint)
        putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
    }

    private fun playableExtras(
        playableHint: Int = CONTENT_STYLE_LIST_ITEM,
    ) = Bundle().apply {
        putBoolean(EXTRA_CONTENT_STYLE_SUPPORTED, true)
        putInt(EXTRA_CONTENT_STYLE_PLAYABLE_HINT, playableHint)
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_PREPARE)
            .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
            .add(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .add(Player.COMMAND_STOP)
            .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .add(Player.COMMAND_SET_SHUFFLE_MODE)
            .add(Player.COMMAND_SET_REPEAT_MODE)
            .build()

        val sessionCommands = connectionResult.availableSessionCommands
            .buildUpon()
            .add(MediaSessionConstants.CommandToggleLike)
            .add(MediaSessionConstants.CommandToggleStartRadio)
            .add(MediaSessionConstants.CommandToggleLibrary)
            .add(MediaSessionConstants.CommandToggleShuffle)
            .add(MediaSessionConstants.CommandToggleRepeatMode)
            .add(MediaSessionConstants.CommandTranslateLyrics)
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_UNSUBSCRIBE))
            .build()

        return MediaSession.ConnectionResult.accept(
            sessionCommands,
            playerCommands,
        )
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            MediaSessionConstants.ACTION_TOGGLE_LIKE -> toggleLike()
            MediaSessionConstants.ACTION_TOGGLE_START_RADIO -> toggleStartRadio()
            MediaSessionConstants.ACTION_TOGGLE_LIBRARY -> toggleLibrary()
            MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> session.player.shuffleModeEnabled =
                !session.player.shuffleModeEnabled

            MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> session.player.toggleRepeatMode()
            MediaSessionConstants.ACTION_TRANSLATE_LYRICS -> {
                val currentMediaItem = session.player.currentMediaItem
                if (currentMediaItem != null) {
                    val songId = currentMediaItem.mediaId.substringAfterLast("/")
                    scope.launch(Dispatchers.IO) {
                        try {
                            val lyricsEntity = database.lyrics(songId).first()
                            if (lyricsEntity != null && lyricsEntity.lyrics != com.nikhil.yt.db.entities.LyricsEntity.LYRICS_NOT_FOUND) {
                                val locale = java.util.Locale.getDefault()
                                val lang = when (locale.language.lowercase()) {
                                    "it" -> me.bush.translator.Language.ITALIAN
                                    "es" -> me.bush.translator.Language.SPANISH
                                    "fr" -> me.bush.translator.Language.FRENCH
                                    "de" -> me.bush.translator.Language.GERMAN
                                    "pt" -> me.bush.translator.Language.PORTUGUESE
                                    "ru" -> me.bush.translator.Language.RUSSIAN
                                    "ja" -> me.bush.translator.Language.JAPANESE
                                    "zh" -> me.bush.translator.Language.CHINESE_SIMPLIFIED
                                    else -> me.bush.translator.Language.ENGLISH
                                }
                                val translator = me.bush.translator.Translator()
                                val lines = lyricsEntity.lyrics.split("\n")
                                val tsRegex = Regex("^((?:\\[[0-9]{2}:[0-9]{2}(?:\\.[0-9]+)?\\])+)")
                                val contents = mutableListOf<String?>()
                                val stampsFor = mutableListOf<String?>()
                                for (line in lines) {
                                    val trimmed = line.trimEnd()
                                    val m = tsRegex.find(trimmed)
                                    if (m != null) {
                                        stampsFor.add(m.groupValues[1])
                                        val content = trimmed.substring(m.range.last + 1).trimStart()
                                        contents.add(if (content.isBlank()) null else content)
                                    } else {
                                        stampsFor.add(null)
                                        contents.add(if (trimmed.isBlank()) null else trimmed)
                                    }
                                }
                                val translatableIndices = contents.mapIndexedNotNull { idx, c -> if (c != null) idx else null }
                                val translatedMap = mutableMapOf<Int, String>()
                                if (translatableIndices.isNotEmpty()) {
                                    val sep = "<<<SEP-${java.util.UUID.randomUUID()}>>>"
                                    val maxCharsPerRequest = 4000
                                    val maxItemsPerBatch = 50
                                    var cursor = 0
                                    while (cursor < translatableIndices.size) {
                                        var currentChars = 0
                                        val batchIndices = mutableListOf<Int>()
                                        while (cursor < translatableIndices.size && batchIndices.size < maxItemsPerBatch) {
                                            val idx = translatableIndices[cursor]
                                            val pieceLen = contents[idx]!!.length
                                            if (batchIndices.isEmpty() || currentChars + pieceLen + sep.length <= maxCharsPerRequest) {
                                                batchIndices.add(idx)
                                                currentChars += pieceLen + sep.length
                                                cursor++
                                            } else break
                                        }
                                        val batchTexts = batchIndices.map { contents[it]!! }
                                        val joined = batchTexts.joinToString(separator = sep)
                                        val translatedJoined = translator.translateBlocking(joined, lang).translatedText
                                        val parts = translatedJoined.split(sep)
                                        if (parts.size == batchTexts.size) {
                                            for (i in batchIndices.indices) {
                                                translatedMap[batchIndices[i]] = parts[i]
                                            }
                                        } else {
                                            for (idx in batchIndices) {
                                                val original = contents[idx]!!
                                                val singleTranslated = runCatching {
                                                    translator.translateBlocking(original, lang).translatedText
                                                }.getOrNull() ?: original
                                                translatedMap[idx] = singleTranslated
                                            }
                                        }
                                    }
                                }
                                val out = mutableListOf<String>()
                                for (i in contents.indices) {
                                    val stamp = stampsFor[i]
                                    val c = contents[i]
                                    if (c == null) {
                                        if (stamp != null) out.add(stamp) else out.add("")
                                    } else {
                                        val translatedText = translatedMap[i] ?: c
                                        if (stamp != null) out.add("$stamp $translatedText") else out.add(translatedText)
                                    }
                                }
                                val translatedLyrics = out.joinToString("\n")
                                database.query {
                                    upsert(lyricsEntity.copy(lyrics = translatedLyrics))
                                }
                                (session as? MediaLibraryService.MediaLibrarySession)?.notifyChildrenChanged("lyrics", out.size, MediaLibraryService.LibraryParams.Builder().build())
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(
            LibraryResult.ofItem(
                MediaItem
                    .Builder()
                    .setMediaId(MusicService.ROOT)
                    .setMediaMetadata(
                        MediaMetadata
                            .Builder()
                            .setIsPlayable(false)
                            .setIsBrowsable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setExtras(browsableExtras())
                            .build(),
                    ).build(),
                params,
            ),
        )

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> =
        scope.future(Dispatchers.IO) {
            val q = query.trim()
            println("VeluneSearch: onSearch query='$q'")
            if (q.isNotBlank()) {
                session.notifySearchResultChanged(browser, query, 25, params)
            }
            LibraryResult.ofVoid(params)
        }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        scope.future(Dispatchers.IO) {
            val q = query.trim()
            println("VeluneSearch: onGetSearchResult query='$q' page=$page pageSize=$pageSize")
            if (q.isBlank() || pageSize <= 0 || page < 0) {
                return@future LibraryResult.ofItemList(emptyList(), params)
            }

            val cleanPageSize = if (pageSize > 100) 100 else pageSize
            val items = ArrayList<MediaItem>()

            try {
                val onlineResult = YouTube.searchSummary(q)
                if (onlineResult.isSuccess) {
                    val summaryPage = onlineResult.getOrNull()
                    if (summaryPage != null) {
                        val allItems = summaryPage.summaries.flatMap { it.items }
                        for (ytItem in allItems) {
                            when (ytItem) {
                                is SongItem -> {
                                    val original = ytItem.toMediaItem()
                                    items += MediaItem.Builder()
                                        .setMediaId(original.mediaId)
                                        .setUri(original.localConfiguration?.uri)
                                        .setCustomCacheKey(original.localConfiguration?.customCacheKey)
                                        .setTag(original.localConfiguration?.tag)
                                        .setMediaMetadata(
                                            original.mediaMetadata.buildUpon()
                                                .setIsBrowsable(false)
                                                .setIsPlayable(true)
                                                .build()
                                        ).build()
                                }
                                is AlbumItem -> {
                                    items += browsableMediaItem(
                                        "${MusicService.ALBUM}/${ytItem.id}",
                                        ytItem.title,
                                        ytItem.artists?.joinToString { it.name } ?: "",
                                        ytItem.thumbnail.toUri(),
                                        MediaMetadata.MEDIA_TYPE_ALBUM,
                                        browsableHint = CONTENT_STYLE_LIST_ITEM
                                    )
                                }
                                is ArtistItem -> {
                                    items += browsableMediaItem(
                                        "${MusicService.ARTIST}/${ytItem.id}",
                                        ytItem.title,
                                        "Artist",
                                        ytItem.thumbnail?.toUri(),
                                        MediaMetadata.MEDIA_TYPE_ARTIST,
                                        browsableHint = CONTENT_STYLE_LIST_ITEM
                                    )
                                }
                                is PlaylistItem -> {
                                    items += browsableMediaItem(
                                        "${MusicService.PLAYLIST}/${ytItem.id}",
                                        ytItem.title,
                                        ytItem.author?.name ?: "",
                                        ytItem.thumbnail?.toUri(),
                                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                        browsableHint = CONTENT_STYLE_LIST_ITEM
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("VeluneSearch: searchSummary failed: ${e.message}")
            }

            val from = page * cleanPageSize
            if (from >= items.size) return@future LibraryResult.ofItemList(emptyList(), params)
            val to = min(from + cleanPageSize, items.size)

            LibraryResult.ofItemList(items.subList(from, to), params)
        }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
        scope.future(Dispatchers.IO) {
            println("VeluneBrowse: onGetChildren start. parentId='$parentId' page=$page pageSize=$pageSize browser=${browser.packageName}")
            try {
                val list = when (parentId) {
                    MusicService.ROOT ->
                        listOf(
                            browsableMediaItem(
                                "${MusicService.PLAYLIST}/${PlaylistEntity.LIKED_PLAYLIST_ID}",
                                "Preferiti",
                                null,
                                drawableUri(R.drawable.favorite),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                "${MusicService.PLAYLIST}/${PlaylistEntity.DOWNLOADED_PLAYLIST_ID}",
                                "Scaricate",
                                null,
                                drawableUri(R.drawable.download),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.RECENT,
                                "Storia",
                                null,
                                drawableUri(R.drawable.history),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.QUEUE,
                                "Coda",
                                null,
                                drawableUri(R.drawable.queue_music),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.PLAYLIST,
                                "Playlist",
                                null,
                                drawableUri(R.drawable.queue_music),
                                MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.ARTIST,
                                "Artisti",
                                null,
                                drawableUri(R.drawable.artist),
                                MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.ALBUM,
                                "Album",
                                null,
                                drawableUri(R.drawable.album),
                                MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.SONG,
                                "Brani",
                                null,
                                drawableUri(R.drawable.music_note),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                "lyrics",
                                "Testo",
                                null,
                                drawableUri(R.drawable.translate),
                                MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                        )

                    "lyrics" -> {
                        val currentMediaItem = session.player.currentMediaItem
                        if (currentMediaItem == null) {
                            listOf(
                                MediaItem.Builder()
                                    .setMediaId("lyrics/no_track")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle("Nessun brano in riproduzione")
                                            .setIsBrowsable(false)
                                            .setIsPlayable(false)
                                            .build()
                                    ).build()
                            )
                        } else {
                            val songId = currentMediaItem.mediaId.substringAfterLast("/")
                            val lyricsEntity = database.lyrics(songId).first()
                            if (lyricsEntity == null || lyricsEntity.lyrics == com.nikhil.yt.db.entities.LyricsEntity.LYRICS_NOT_FOUND) {
                                listOf(
                                    MediaItem.Builder()
                                        .setMediaId("lyrics/not_found")
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle("Testo non trovato")
                                                .setIsBrowsable(false)
                                                .setIsPlayable(false)
                                                .build()
                                        ).build()
                                )
                            } else {
                                val lines = lyricsEntity.lyrics.split("\n")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                lines.mapIndexed { index, line ->
                                    MediaItem.Builder()
                                        .setMediaId("lyrics/line_$index")
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(line)
                                                .setIsBrowsable(false)
                                                .setIsPlayable(false)
                                                .build()
                                        ).build()
                                }
                            }
                        }
                    }

                    "library" ->
                        listOf(
                            browsableMediaItem(
                                MusicService.SONG,
                                context.getString(R.string.songs),
                                null,
                                drawableUri(R.drawable.music_note),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.ARTIST,
                                context.getString(R.string.artists),
                                null,
                                drawableUri(R.drawable.artist),
                                MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.ALBUM,
                                context.getString(R.string.albums),
                                null,
                                drawableUri(R.drawable.album),
                                MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.PLAYLIST,
                                context.getString(R.string.playlists),
                                null,
                                drawableUri(R.drawable.queue_music),
                                MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                MusicService.QUEUE,
                                context.getString(R.string.queue),
                                null,
                                drawableUri(R.drawable.queue_music),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                        )

                    MusicService.SONG -> database.songsByCreateDateAsc().first()
                        .map { it.toMediaItemWithPath(parentId) }

                    MusicService.ARTIST ->
                        database.artistsByCreateDateAsc().first().map { artist ->
                            browsableMediaItem(
                                "${MusicService.ARTIST}/${artist.id}",
                                artist.artist.name,
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    artist.songCount,
                                    artist.songCount
                                ),
                                artist.artist.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ARTIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            )
                        }

                    MusicService.ALBUM ->
                        database.albumsByCreateDateAsc().first().map { album ->
                            browsableMediaItem(
                                "${MusicService.ALBUM}/${album.id}",
                                album.album.title,
                                album.artists.joinToString {
                                    it.name
                                },
                                album.album.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ALBUM,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            )
                        }

                    MusicService.PLAYLIST -> {
                        val likedSongCount = database.likedSongsCount().first()
                        val downloadedSongCount = downloadUtil.downloads.value.size
                        listOf(
                            browsableMediaItem(
                                "${MusicService.PLAYLIST}/${PlaylistEntity.LIKED_PLAYLIST_ID}",
                                context.getString(R.string.liked_songs),
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    likedSongCount,
                                    likedSongCount
                                ),
                                drawableUri(R.drawable.favorite),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                            browsableMediaItem(
                                "${MusicService.PLAYLIST}/${PlaylistEntity.DOWNLOADED_PLAYLIST_ID}",
                                context.getString(R.string.downloaded_songs),
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    downloadedSongCount,
                                    downloadedSongCount
                                ),
                                drawableUri(R.drawable.download),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                browsableHint = CONTENT_STYLE_LIST_ITEM,
                            ),
                        ) +
                                database.playlistsByCreateDateAsc().first().map { playlist ->
                                    browsableMediaItem(
                                        "${MusicService.PLAYLIST}/${playlist.id}",
                                        playlist.playlist.name,
                                        context.resources.getQuantityString(
                                            R.plurals.n_song,
                                            playlist.songCount,
                                            playlist.songCount
                                        ),
                                        playlist.thumbnails.firstOrNull()?.toUri(),
                                        MediaMetadata.MEDIA_TYPE_PLAYLIST,
                                        browsableHint = CONTENT_STYLE_LIST_ITEM,
                                    )
                                }
                    }

                    MusicService.RECENT -> {
                        val events = database.events().first()
                        val uniqueSongs = events.distinctBy { it.event.songId }.map { it.song }
                        uniqueSongs.take(50).map { it.toMediaItemWithPath(parentId) }
                    }

                    MusicService.QUEUE -> {
                        val player = session.player
                        val list = mutableListOf<MediaItem>()
                        for (i in 0 until player.mediaItemCount) {
                            val originalItem = player.getMediaItemAt(i)
                            val item = MediaItem.Builder()
                                .setMediaId("${MusicService.QUEUE}/${originalItem.mediaId}")
                                .setMediaMetadata(
                                    originalItem.mediaMetadata.buildUpon()
                                        .setIsPlayable(true)
                                        .setIsBrowsable(false)
                                        .build()
                                ).build()
                            list.add(item)
                        }
                        list
                    }

                    else ->
                        when {
                            parentId.startsWith("${MusicService.ARTIST}/") -> {
                                val artistId = parentId.removePrefix("${MusicService.ARTIST}/")
                                val localSongs = database.artistSongsByCreateDateAsc(artistId).first()
                                if (localSongs.isNotEmpty()) {
                                    localSongs.map { it.toMediaItemWithPath(parentId) }
                                } else {
                                    val onlineResult = YouTube.artist(artistId)
                                    val onlineSongs = onlineResult.getOrNull()?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>() ?: emptyList()
                                    onlineSongs.map { song ->
                                        val original = song.toMediaItem()
                                        MediaItem.Builder()
                                            .setMediaId(original.mediaId)
                                            .setUri(original.localConfiguration?.uri)
                                            .setCustomCacheKey(original.localConfiguration?.customCacheKey)
                                            .setTag(original.localConfiguration?.tag)
                                            .setMediaMetadata(
                                                original.mediaMetadata.buildUpon()
                                                    .setIsBrowsable(false)
                                                    .setIsPlayable(true)
                                                    .build()
                                            ).build()
                                    }
                                }
                            }

                            parentId.startsWith("${MusicService.ALBUM}/") -> {
                                val albumId = parentId.removePrefix("${MusicService.ALBUM}/")
                                val localSongs = database.albumSongs(albumId).first()
                                if (localSongs.isNotEmpty()) {
                                    localSongs.map { it.toMediaItemWithPath(parentId) }
                                } else {
                                    val onlineResult = YouTube.album(albumId)
                                    val onlineSongs = onlineResult.getOrNull()?.songs ?: emptyList()
                                    onlineSongs.map { song ->
                                        val original = song.toMediaItem()
                                        MediaItem.Builder()
                                            .setMediaId(original.mediaId)
                                            .setUri(original.localConfiguration?.uri)
                                            .setCustomCacheKey(original.localConfiguration?.customCacheKey)
                                            .setTag(original.localConfiguration?.tag)
                                            .setMediaMetadata(
                                                original.mediaMetadata.buildUpon()
                                                    .setIsBrowsable(false)
                                                    .setIsPlayable(true)
                                                    .build()
                                            ).build()
                                    }
                                }
                            }

                            parentId.startsWith("${MusicService.PLAYLIST}/") -> {
                                val playlistId = parentId.removePrefix("${MusicService.PLAYLIST}/")
                                when (playlistId) {
                                    PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(
                                        SongSortType.CREATE_DATE,
                                        true
                                    ).first().map { it.toMediaItemWithPath(parentId) }

                                    PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                                        val downloads = downloadUtil.downloads.value
                                        database.allSongs().flowOn(Dispatchers.IO).map { songs ->
                                            songs.filter { downloads[it.id]?.state == Download.STATE_COMPLETED }
                                        }.map { songs ->
                                            songs.map { it to downloads[it.id] }.sortedBy { it.second?.updateTimeMs ?: 0L }.map { it.first }
                                        }.first().map { it.toMediaItemWithPath(parentId) }
                                    }

                                    else -> {
                                        val localSongs = database.playlistSongs(playlistId).first().map { it.song }
                                        if (localSongs.isNotEmpty()) {
                                            localSongs.map { it.toMediaItemWithPath(parentId) }
                                        } else {
                                            val onlineResult = YouTube.playlist(playlistId)
                                            val onlineSongs = onlineResult.getOrNull()?.songs ?: emptyList()
                                            onlineSongs.map { song ->
                                                val original = song.toMediaItem()
                                                MediaItem.Builder()
                                                    .setMediaId(original.mediaId)
                                                    .setUri(original.localConfiguration?.uri)
                                                    .setCustomCacheKey(original.localConfiguration?.customCacheKey)
                                                    .setTag(original.localConfiguration?.tag)
                                                    .setMediaMetadata(
                                                        original.mediaMetadata.buildUpon()
                                                            .setIsBrowsable(false)
                                                            .setIsPlayable(true)
                                                            .build()
                                                    ).build()
                                            }
                                        }
                                    }
                                }
                            }

                            else -> emptyList()
                        }
                }
                println("VeluneBrowse: returning ${list.size} children for parentId='$parentId'")
                LibraryResult.ofItemList(ImmutableList.copyOf(list), params)
            } catch (e: Exception) {
                println("VeluneBrowse: onGetChildren failed for parentId='$parentId': ${e.message}")
                e.printStackTrace()
                LibraryResult.ofItemList(ImmutableList.of(), params)
            }
        }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> =
        scope.future(Dispatchers.IO) {
            when {
                mediaId == MusicService.ROOT ->
                    LibraryResult.ofItem(
                        MediaItem
                            .Builder()
                            .setMediaId(MusicService.ROOT)
                            .setMediaMetadata(
                                MediaMetadata
                                    .Builder()
                                    .setIsPlayable(false)
                                    .setIsBrowsable(true)
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                    .setExtras(browsableExtras())
                                    .build(),
                            ).build(),
                        null,
                    )

                mediaId == "library" ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            "library",
                            "Libreria",
                            null,
                            drawableUri(R.drawable.library_music),
                            MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                        ),
                        null,
                    )

                mediaId == "lyrics" ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            "lyrics",
                            "Testo",
                            null,
                            drawableUri(R.drawable.translate),
                            MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                        ),
                        null,
                    )

                mediaId == MusicService.SONG ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.SONG,
                            context.getString(R.string.songs),
                            null,
                            drawableUri(R.drawable.music_note),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.ARTIST ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.ARTIST,
                            context.getString(R.string.artists),
                            null,
                            drawableUri(R.drawable.artist),
                            MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS,
                        ),
                        null,
                    )

                mediaId == MusicService.ALBUM ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.ALBUM,
                            context.getString(R.string.albums),
                            null,
                            drawableUri(R.drawable.album),
                            MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
                        ),
                        null,
                    )

                mediaId == MusicService.PLAYLIST ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.PLAYLIST,
                            context.getString(R.string.playlists),
                            null,
                            drawableUri(R.drawable.queue_music),
                            MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS,
                        ),
                        null,
                    )

                mediaId == MusicService.RECENT ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.RECENT,
                            context.getString(R.string.history),
                            null,
                            drawableUri(R.drawable.history),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId == MusicService.QUEUE ->
                    LibraryResult.ofItem(
                        browsableMediaItem(
                            MusicService.QUEUE,
                            context.getString(R.string.queue),
                            null,
                            drawableUri(R.drawable.queue_music),
                            MediaMetadata.MEDIA_TYPE_PLAYLIST,
                        ),
                        null,
                    )

                mediaId.startsWith("${MusicService.RECENT}/") ->
                    database.song(mediaId.removePrefix("${MusicService.RECENT}/")).first()?.let {
                        LibraryResult.ofItem(it.toMediaItemWithPath(MusicService.RECENT), null)
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.QUEUE}/") -> {
                    val songId = mediaId.removePrefix("${MusicService.QUEUE}/")
                    val player = session.player
                    var foundItem: MediaItem? = null
                    for (i in 0 until player.mediaItemCount) {
                        val item = player.getMediaItemAt(i)
                        if (item.mediaId == songId || item.mediaId.endsWith("/$songId")) {
                            foundItem = item
                            break
                        }
                    }
                    foundItem?.let {
                        LibraryResult.ofItem(
                            MediaItem.Builder()
                                .setMediaId(mediaId)
                                .setMediaMetadata(
                                    it.mediaMetadata.buildUpon()
                                        .setIsPlayable(true)
                                        .setIsBrowsable(false)
                                        .build()
                                ).build(),
                            null
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
                }

                mediaId.startsWith("${MusicService.SONG}/") ->
                    database.song(mediaId.removePrefix("${MusicService.SONG}/")).first()?.let {
                        LibraryResult.ofItem(it.toMediaItemWithPath(MusicService.SONG), null)
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.ARTIST}/") ->
                    database.artist(mediaId.removePrefix("${MusicService.ARTIST}/")).first()?.let { artist ->
                        LibraryResult.ofItem(
                            browsableMediaItem(
                                "${MusicService.ARTIST}/${artist.id}",
                                artist.title,
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    artist.songCount,
                                    artist.songCount,
                                ),
                                artist.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ARTIST,
                            ),
                            null,
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.ALBUM}/") ->
                    database.album(mediaId.removePrefix("${MusicService.ALBUM}/")).first()?.let { album ->
                        LibraryResult.ofItem(
                            browsableMediaItem(
                                "${MusicService.ALBUM}/${album.id}",
                                album.title,
                                album.artists.joinToString { it.name },
                                album.thumbnailUrl?.toUri(),
                                MediaMetadata.MEDIA_TYPE_ALBUM,
                            ),
                            null,
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                mediaId.startsWith("${MusicService.PLAYLIST}/") ->
                    database.playlist(mediaId.removePrefix("${MusicService.PLAYLIST}/")).first()?.let { playlist ->
                        LibraryResult.ofItem(
                            browsableMediaItem(
                                "${MusicService.PLAYLIST}/${playlist.id}",
                                playlist.title,
                                context.resources.getQuantityString(
                                    R.plurals.n_song,
                                    playlist.songCount,
                                    playlist.songCount,
                                ),
                                playlist.thumbnails.firstOrNull()?.toUri(),
                                MediaMetadata.MEDIA_TYPE_PLAYLIST,
                            ),
                            null,
                        )
                    } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)

                else -> {
                    val player = session.player
                    var foundItem: MediaItem? = null
                    for (i in 0 until player.mediaItemCount) {
                        val item = player.getMediaItemAt(i)
                        if (item.mediaId == mediaId || item.mediaId.substringAfterLast("/") == mediaId) {
                            foundItem = item
                            break
                        }
                    }
                    if (foundItem != null) {
                        LibraryResult.ofItem(
                            MediaItem.Builder()
                                .setMediaId(mediaId)
                                .setMediaMetadata(
                                    foundItem.mediaMetadata.buildUpon()
                                        .setIsPlayable(true)
                                        .setIsBrowsable(false)
                                        .build()
                                ).build(),
                            null
                        )
                    } else {
                        database.song(mediaId).first()?.toMediaItem()?.let {
                            LibraryResult.ofItem(it, null)
                        } ?: LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
                    }
                }
            }
        }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
        scope.future {
            // Play from Android Auto
            val defaultResult =
                MediaSession.MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
            val firstItem = mediaItems.firstOrNull() ?: return@future defaultResult
            val path = firstItem.mediaId.split("/").filter { it.isNotBlank() }
            when (path.firstOrNull()) {
                MusicService.SONG -> {
                    val songId = path.getOrNull(1)
                    val allSongs = database.songsByCreateDateAsc().first()
                    val index = if (songId != null) {
                        allSongs.indexOfFirst { it.id == songId }
                    } else -1
                    if (index != -1) {
                        MediaSession.MediaItemsWithStartPosition(
                            allSongs.map { it.toMediaItem() },
                            index,
                            startPositionMs,
                        )
                    } else {
                        val cleanItem = firstItem.buildUpon()
                            .setMediaId(songId ?: firstItem.mediaId)
                            .build()
                        MediaSession.MediaItemsWithStartPosition(
                            listOf(cleanItem),
                            0,
                            startPositionMs,
                        )
                    }
                }

                MusicService.ARTIST -> {
                    val artistId = path.getOrNull(1) ?: return@future defaultResult
                    val songId = path.getOrNull(2)
                    val songs = database.artistSongsByCreateDateAsc(artistId).first()
                    val index = if (songId != null) {
                        songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0
                    } else 0
                    MediaSession.MediaItemsWithStartPosition(
                        songs.map { it.toMediaItem() },
                        index,
                        startPositionMs,
                    )
                }

                MusicService.ALBUM -> {
                    val albumId = path.getOrNull(1) ?: return@future defaultResult
                    val songId = path.getOrNull(2)
                    val albumWithSongs =
                        database.albumWithSongs(albumId).first() ?: return@future defaultResult
                    val index = if (songId != null) {
                        albumWithSongs.songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0
                    } else 0
                    MediaSession.MediaItemsWithStartPosition(
                        albumWithSongs.songs.map { it.toMediaItem() },
                        index,
                        startPositionMs,
                    )
                }

                MusicService.PLAYLIST -> {
                    val playlistId = path.getOrNull(1) ?: return@future defaultResult
                    val songId = path.getOrNull(2)
                    val songs =
                        when (playlistId) {
                            PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(
                                SongSortType.CREATE_DATE,
                                descending = true
                            )

                            PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                                val downloads = downloadUtil.downloads.value
                                database
                                    .allSongs()
                                    .flowOn(Dispatchers.IO)
                                    .map { songs ->
                                        songs.filter {
                                            downloads[it.id]?.state == Download.STATE_COMPLETED
                                        }
                                    }.map { songs ->
                                        songs
                                            .map { it to downloads[it.id] }
                                            .sortedBy { it.second?.updateTimeMs ?: 0L }
                                            .map { it.first }
                                    }
                            }

                            else ->
                                database.playlistSongs(playlistId).map { list ->
                                    list.map { it.song }
                                }
                        }.first()
                    val index = if (songId != null) {
                        songs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0
                    } else 0
                    MediaSession.MediaItemsWithStartPosition(
                        songs.map { it.toMediaItem() },
                        index,
                        startPositionMs,
                    )
                }

                MusicService.RECENT -> {
                    val songId = path.getOrNull(1)
                    val events = database.events().first()
                    val uniqueSongs = events.distinctBy { it.event.songId }.map { it.song }
                    val index = if (songId != null) {
                        uniqueSongs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0
                    } else 0
                    MediaSession.MediaItemsWithStartPosition(
                        uniqueSongs.map { it.toMediaItem() },
                        index,
                        startPositionMs,
                    )
                }

                MusicService.QUEUE -> {
                    val songId = path.getOrNull(1)
                    val currentItems = mutableListOf<MediaItem>()
                    val player = mediaSession.player
                    for (i in 0 until player.mediaItemCount) {
                        currentItems.add(player.getMediaItemAt(i))
                    }
                    val index = if (songId != null) {
                        currentItems.indexOfFirst { it.mediaId == songId || it.mediaId.endsWith("/$songId") }.takeIf { it != -1 } ?: 0
                    } else player.currentMediaItemIndex
                    MediaSession.MediaItemsWithStartPosition(
                        currentItems,
                        index,
                        startPositionMs,
                    )
                }

                else -> {
                    if (firstItem.mediaMetadata.isPlayable == true) {
                        return@future MediaSession.MediaItemsWithStartPosition(
                            mediaItems,
                            0,
                            startPositionMs,
                        )
                    }

                    // Handle plain YouTube video IDs from AA search results
                    // (SongItem.toMediaItem() sets mediaId = videoId with no prefix)
                    val mediaId = firstItem.mediaId
                    if (mediaId.isNotBlank() && !mediaId.contains("/") && firstItem.localConfiguration?.uri != null) {
                        return@future MediaSession.MediaItemsWithStartPosition(
                            mediaItems.map { item ->
                                item.buildUpon()
                                    .setMediaMetadata(
                                        item.mediaMetadata.buildUpon()
                                            .setIsPlayable(true)
                                            .build()
                                    )
                                    .build()
                            },
                            0,
                            startPositionMs,
                        )
                    }

                    val query = firstItem.requestMetadata.searchQuery?.trim().orEmpty()
                    if (query.isBlank()) return@future defaultResult

                    val matchedSongs = database.searchSongs(query, previewSize = 50).first()
                    val songId = matchedSongs.firstOrNull()?.id ?: return@future defaultResult
                    val allSongs = database.songsByCreateDateAsc().first()
                    MediaSession.MediaItemsWithStartPosition(
                        allSongs.map { it.toMediaItem() },
                        allSongs.indexOfFirst { it.id == songId }.takeIf { it != -1 } ?: 0,
                        startPositionMs,
                    )
                }
            }
        }

    private fun drawableUri(
        @DrawableRes id: Int,
    ) = Uri
        .Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.resources.getResourcePackageName(id))
        .appendPath(context.resources.getResourceTypeName(id))
        .appendPath(context.resources.getResourceEntryName(id))
        .build()

    private fun browsableMediaItem(
        id: String,
        title: String,
        subtitle: String?,
        iconUri: Uri?,
        mediaType: Int = MediaMetadata.MEDIA_TYPE_MUSIC,
        browsableHint: Int = CONTENT_STYLE_LIST_ITEM,
        playableHint: Int = CONTENT_STYLE_LIST_ITEM,
    ) = MediaItem
        .Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtist(subtitle)
                .setArtworkUri(iconUri)
                .setIsPlayable(false)
                .setIsBrowsable(true)
                .setMediaType(mediaType)
                .setExtras(browsableExtras(browsableHint, playableHint))
                .build(),
        ).build()

    private fun Song.toMediaItemWithPath(path: String) =
        MediaItem
            .Builder()
            .setMediaId("$path/$id")
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(song.title)
                    .setSubtitle(artists.joinToString { it.name })
                    .setArtist(artists.joinToString { it.name })
                    .setArtworkUri(song.thumbnailUrl?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(playableExtras())
                    .build(),
            ).build()

    companion object {
        private const val EXTRA_CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val EXTRA_CONTENT_STYLE_BROWSABLE_HINT =
            "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val EXTRA_CONTENT_STYLE_PLAYABLE_HINT =
            "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"

        private const val CONTENT_STYLE_LIST_ITEM = 1
        private const val CONTENT_STYLE_GRID_ITEM = 2
    }
}
