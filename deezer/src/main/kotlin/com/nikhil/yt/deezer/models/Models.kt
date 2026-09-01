package com.nikhil.yt.deezer.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SearchResponse(
    val data: List<DeezerTrackRs> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class DeezerTrackRs(
    val id: Long = 0,
    val title: String = "",
    val duration: Int = 0,
    val artist: DeezerArtistRs? = null,
    val album: DeezerAlbumRs? = null,
    val explicit_lyrics: Boolean = false,
    val preview: String? = null,
)

@Serializable
data class DeezerArtistRs(
    val id: Long = 0,
    val name: String = "",
)

@Serializable
data class DeezerAlbumRs(
    val id: Long = 0,
    val title: String = "",
    val cover_medium: String? = null,
)

data class DeezerTrack(
    val id: String,
    val title: String,
    val durationSeconds: Int,
    val artist: String,
    val artistId: String,
    val album: String,
    val artworkUrl: String?,
    val explicit: Boolean,
)

data class StreamPlan(
    val cdnUrl: String,
    val trackId: String,
    val format: String,
    val encrypted: Boolean = true,
    val preview: Boolean = false,
)

object DeezerJson {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
}
