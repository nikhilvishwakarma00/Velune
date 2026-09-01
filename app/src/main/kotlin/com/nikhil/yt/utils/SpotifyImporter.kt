package com.nikhil.yt.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.nikhil.yt.constants.SavedSpotifyPlaylistsKey
import com.nikhil.yt.db.MusicDatabase
import com.nikhil.yt.db.entities.PlaylistEntity
import com.nikhil.yt.models.toMediaMetadata
import com.nikhil.yt.innertube.YouTube
import com.nikhil.yt.innertube.models.SongItem

object SpotifyImporter {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    data class SpotifyTrack(val title: String, val artist: String, val album: String?)
    data class SpotifyPlaylist(val name: String, val tracks: List<SpotifyTrack>)

    fun extractPlaylistId(url: String): String? {
        val pattern = Pattern.compile("playlist/([a-zA-Z0-9]{22})")
        val matcher = pattern.matcher(url)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }

    suspend fun fetchPlaylist(url: String): Result<SpotifyPlaylist> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val playlistId = extractPlaylistId(url)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Spotify playlist URL"))

        val fetchUrl = "https://open.spotify.com/embed/playlist/$playlistId"
        val request = Request.Builder()
            .url(fetchUrl)
            .header("User-Agent", userAgent)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to load playlist page: ${response.code}"))
                }
                val html = response.body?.string() ?: ""
                if (html.isBlank()) {
                    return@withContext Result.failure(Exception("Empty response from Spotify"))
                }

                // 1. Parse __NEXT_DATA__ or initial-state JSON
                val regexNextData = Regex("""<script\s+id="__NEXT_DATA__"\s+type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                val regexInitialState = Regex("""<script\s+id="initial-state"\s+type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)

                val jsonString = regexNextData.find(html)?.groupValues?.get(1)
                    ?: regexInitialState.find(html)?.groupValues?.get(1)
                    ?: return@withContext Result.failure(Exception("Could not find Spotify playlist data in HTML. Make sure the playlist is public."))

                val jsonElement = Json.parseToJsonElement(jsonString)
                
                // 2. Parse playlist name
                val nameFromJson = findPlaylistName(jsonElement)
                val ogTitleRegex = Regex("""<meta\s+property="og:title"\s+content="([^"]+)"""", RegexOption.IGNORE_CASE)
                val titleRegex = Regex("""<title>([^<]+)</title>""", RegexOption.IGNORE_CASE)
                val rawName = nameFromJson
                    ?: ogTitleRegex.find(html)?.groupValues?.get(1)
                    ?: titleRegex.find(html)?.groupValues?.get(1)?.substringBefore(" | Spotify")?.substringBefore(" - playlist")
                    ?: "Imported Spotify Playlist"
                val playlistName = rawName.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")

                val tracksArray = findTracksArray(jsonElement)
                    ?: return@withContext Result.failure(Exception("Could not parse tracks array from page data."))

                val tracksList = mutableListOf<SpotifyTrack>()
                for (item in tracksArray) {
                    if (item is JsonObject) {
                        val parsed = parseTrackItem(item)
                        if (parsed != null) {
                            tracksList.add(parsed)
                        }
                    }
                }

                Result.success(SpotifyPlaylist(playlistName, tracksList))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findPlaylistName(jsonElement: JsonElement): String? {
        if (jsonElement is JsonObject) {
            val type = jsonElement["type"]?.jsonPrimitive?.content
            if (type == "playlist") {
                val name = jsonElement["name"]?.jsonPrimitive?.content
                    ?: jsonElement["title"]?.jsonPrimitive?.content
                if (name != null) return name
            }
            for (value in jsonElement.values) {
                val found = findPlaylistName(value)
                if (found != null) return found
            }
        } else if (jsonElement is JsonArray) {
            for (value in jsonElement) {
                val found = findPlaylistName(value)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findTracksArray(jsonElement: JsonElement): JsonArray? {
        if (jsonElement is JsonObject) {
            if (jsonElement.containsKey("trackList") && jsonElement["trackList"] is JsonArray) {
                return jsonElement["trackList"] as JsonArray
            }
            if (jsonElement.containsKey("tracks") && jsonElement["tracks"] is JsonArray) {
                val arr = jsonElement["tracks"] as JsonArray
                val first = arr.firstOrNull() as? JsonObject
                if (first != null && (first.containsKey("title") || first.containsKey("name") || first.containsKey("uri"))) {
                    return arr
                }
            }
            if (jsonElement.containsKey("tracks") && jsonElement["tracks"] is JsonObject) {
                val tracksObj = jsonElement["tracks"] as JsonObject
                if (tracksObj.containsKey("items") && tracksObj["items"] is JsonArray) {
                    return tracksObj["items"] as JsonArray
                }
            }
            if (jsonElement.containsKey("items") && jsonElement["items"] is JsonArray) {
                val itemsArr = jsonElement["items"] as JsonArray
                val first = itemsArr.firstOrNull() as? JsonObject
                if (first != null && (first.containsKey("track") || first.containsKey("name") || first.containsKey("title"))) {
                    return itemsArr
                }
            }
            for (value in jsonElement.values) {
                val found = findTracksArray(value)
                if (found != null) return found
            }
        } else if (jsonElement is JsonArray) {
            for (value in jsonElement) {
                val found = findTracksArray(value)
                if (found != null) return found
            }
        }
        return null
    }

    private fun parseTrackItem(item: JsonObject): SpotifyTrack? {
        val track = item["track"] as? JsonObject ?: item
        val name = track["name"]?.jsonPrimitive?.content
            ?: track["title"]?.jsonPrimitive?.content
            ?: return null

        val artistsArr = track["artists"] as? JsonArray
        val artistName = if (artistsArr != null && artistsArr.isNotEmpty()) {
            val firstArtist = artistsArr[0] as? JsonObject
            firstArtist?.get("name")?.jsonPrimitive?.content ?: ""
        } else {
            track["artistName"]?.jsonPrimitive?.content
                ?: track["artist"]?.jsonPrimitive?.content
                ?: track["subtitle"]?.jsonPrimitive?.content
                ?: ""
        }

        val album = (track["album"] as? JsonObject)?.get("name")?.jsonPrimitive?.content
            ?: track["albumName"]?.jsonPrimitive?.content

        return SpotifyTrack(name, artistName, album)
    }

    suspend fun savePlaylistForSync(context: Context, playlistId: String, spotifyUrl: String, playlistName: String) {
        val prefs = context.dataStore.data.first()
        val savedStr = prefs[SavedSpotifyPlaylistsKey] ?: ""
        
        val jsonArray = try {
            JSONArray(savedStr)
        } catch (_: Exception) {
            JSONArray()
        }

        // Check if already registered
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            if (obj.optString("playlistId") == playlistId) {
                // Update Spotify URL and return
                obj.put("spotifyUrl", spotifyUrl)
                obj.put("playlistName", playlistName)
                context.dataStore.edit {
                    it[SavedSpotifyPlaylistsKey] = jsonArray.toString()
                }
                return
            }
        }

        // If not found, add it
        val newObj = JSONObject().apply {
            put("playlistId", playlistId)
            put("spotifyUrl", spotifyUrl)
            put("playlistName", playlistName)
        }
        jsonArray.put(newObj)
        context.dataStore.edit {
            it[SavedSpotifyPlaylistsKey] = jsonArray.toString()
        }
    }

    suspend fun syncSavedPlaylists(context: Context, database: MusicDatabase) {
        val prefs = context.dataStore.data.first()
        val savedStr = prefs[SavedSpotifyPlaylistsKey] ?: ""
        if (savedStr.isBlank()) return

        val jsonArray = try {
            JSONArray(savedStr)
        } catch (_: Exception) {
            return
        }

        if (jsonArray.length() == 0) return

        val newSavedArray = JSONArray()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val playlistId = obj.optString("playlistId") ?: continue
            val spotifyUrl = obj.optString("spotifyUrl") ?: continue

            // 1. Check if local playlist still exists in DB
            val playlist = database.getPlaylistById(playlistId)
            if (playlist == null) {
                // If it doesn't exist, skip it so it is deleted from sync list
                continue
            }

            // Keep it in the new sync list
            newSavedArray.put(obj)

            // 2. Fetch updated tracks from Spotify
            fetchPlaylist(spotifyUrl).onSuccess { spotifyPlaylist ->
                val spotifyTracks = spotifyPlaylist.tracks
                if (spotifyTracks.isEmpty()) return@onSuccess

                // 3. Get songs currently in the local playlist
                val localSongs = database.playlistSongs(playlistId).firstOrNull() ?: emptyList()
                val localSongTitlesAndArtists = localSongs.map { 
                    val artistsStr = it.song.artists.joinToString { artist -> artist.name }
                    "${it.song.title.lowercase().trim()} ${artistsStr.lowercase().trim()}"
                }.toSet()

                val newTrackIds = mutableListOf<String>()

                // 4. Find and match new songs
                spotifyTracks.forEach { track ->
                    val cleanTitle = track.title.lowercase().trim()
                    val cleanArtist = track.artist.lowercase().trim()
                    val searchKey = "$cleanTitle $cleanArtist"

                    // If not already in local playlist, search and add it!
                    if (!localSongTitlesAndArtists.contains(searchKey)) {
                        val queryText = "${track.title} ${track.artist}"
                        val searchResult = YouTube.search(queryText, YouTube.SearchFilter.FILTER_SONG)
                        searchResult.onSuccess { search ->
                            val bestMatch = search.items.distinctBy { it.id }.firstOrNull() as? SongItem
                            if (bestMatch != null) {
                                val media = bestMatch.toMediaMetadata()
                                try {
                                    database.insert(media)
                                    newTrackIds.add(bestMatch.id)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }

                // 5. Add matching new tracks to the playlist
                if (newTrackIds.isNotEmpty()) {
                    database.addSongToPlaylist(playlist, newTrackIds)
                }
            }
        }

        // Save updated sync list (filtering out deleted playlists)
        context.dataStore.edit {
            it[SavedSpotifyPlaylistsKey] = newSavedArray.toString()
        }
    }
}
