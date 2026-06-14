/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.services

import android.util.Base64
import com.nikhil.yt.db.entities.Playlist
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles encoding and decoding of local playlists for export/import.
 *
 * Export format (base64url of compact JSON):
 * {
 *   "title": "My Playlist",
 *   "songs": ["videoId1", "videoId2", ...]
 * }
 *
 * Song IDs are YouTube video IDs (same as SongEntity.id).
 */
object ExportPlaylistService {

    /**
     * Encodes a [Playlist] into a compact base64url string suitable for
     * sharing via clipboard or QR code.
     *
     * @param playlist The Room [Playlist] object (with its song list populated).
     * @param songs    List of YouTube video IDs belonging to the playlist, in order.
     * @return A base64url-encoded string, or null if encoding fails.
     */
    fun encode(playlist: Playlist, songs: List<String>): String? {
        return try {
            val json = JSONObject().apply {
                put("title", playlist.playlist.name)
                put("songs", JSONArray(songs))
            }
            Base64.encodeToString(
                json.toString().toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a base64url string (produced by [encode]) back into its
     * constituent parts.
     *
     * @param encoded The base64url string to decode.
     * @return A [DecodedPlaylist] with title and song IDs, or null on failure.
     */
    fun decode(encoded: String): DecodedPlaylist? {
        return try {
            val json = JSONObject(
                String(
                    Base64.decode(encoded, Base64.URL_SAFE),
                    Charsets.UTF_8
                )
            )
            val title = json.getString("title")
            val songsArray = json.getJSONArray("songs")
            val ids = (0 until songsArray.length()).map { songsArray.getString(it) }
            DecodedPlaylist(title = title, songIds = ids)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class DecodedPlaylist(
        val title: String,
        val songIds: List<String>,
    )
}
