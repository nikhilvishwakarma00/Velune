package com.nikhil.yt.deezer

import com.nikhil.yt.deezer.crypto.StripeDecryptor
import com.nikhil.yt.deezer.models.DeezerJson
import com.nikhil.yt.deezer.models.DeezerTrack
import com.nikhil.yt.deezer.models.DeezerTrackRs
import com.nikhil.yt.deezer.models.LoginResponse
import com.nikhil.yt.deezer.models.MediaResponse
import com.nikhil.yt.deezer.models.SearchResponse
import com.nikhil.yt.deezer.models.SongDataResponse
import com.nikhil.yt.deezer.models.StreamPlan
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Deezer {
    private const val GW_URL = "https://www.deezer.com/ajax/gw-light.php"
    private const val MEDIA_URL = "https://media.deezer.com/v1/get_url"
    private const val REST_URL = "https://api.deezer.com"
    private const val USER_AGENT = "Mozilla/5.0 DeezerKotlin/1.0"

    private var arl: String = ""
    private var apiToken: String = ""
    private var licenseToken: String = ""
    private var sid: String = ""
    private var loggedIn: Boolean = false
    private var logFile: File? = null

    fun setLogDir(dir: File) {
        dir.mkdirs()
        logFile = File(dir, "deezer_log.txt")
        logFile?.appendText("--- Deezer log started ---\n")
    }

    private fun log(msg: String) {
        val f = logFile
        if (f != null) {
            try { f.appendText("$msg\n") } catch (_: Exception) {}
        }
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(DeezerJson.json)
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 30000
            }
        }
    }

    fun setArl(value: String) {
        arl = value.trim()
        loggedIn = false
        apiToken = ""
        licenseToken = ""
        sid = ""
    }

    fun isLoggedIn(): Boolean = loggedIn

    suspend fun login(): Result<Unit> = runCatching {
        val url = "$GW_URL?method=deezer.getUserData&input=3&api_version=1.0&api_token="
        log("Login: POST $url")
        val response = client.post(url) {
            header("Cookie", "arl=$arl")
            header("User-Agent", USER_AGENT)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        log("Login response status: ${response.status}")

        val setCookieHeader = response.headers["Set-Cookie"]
        sid = parseSidCookie(setCookieHeader)
        log("Login sid: $sid")

        val body = response.bodyAsText()
        log("Login body len: ${body.length}, preview: ${body.take(300)}")
        if (body.contains("\"error\"")) {
            log("ERR: Login response contains error: ${body.take(500)}")
        }

        val parsed = DeezerJson.json.decodeFromString<LoginResponse>(body)
        val results = parsed.results

        val newApiToken = results.checkForm
        val userObj = results.USER ?: throw IllegalStateException("No USER in login response. Body: ${body.take(300)}")
        val newUserId = userObj.userId
        val newLicenseToken = userObj.OPTIONS?.license_token ?: ""
        log("Login parsed: apiToken=$newApiToken, userId=$newUserId, licenseToken=$newLicenseToken")

        require(newApiToken.isNotEmpty() && newUserId.isNotEmpty() && newUserId != "0") {
            "ARL expired or invalid"
        }

        apiToken = newApiToken
        licenseToken = newLicenseToken
        loggedIn = true
        log("Login OK")
    }

    suspend fun search(title: String, artist: String, limit: Int = 20): List<DeezerTrack> {
        val query = "$title $artist"
        log("search: q=$query")
        return try {
            val response = client.get("$REST_URL/search") {
                header("User-Agent", USER_AGENT)
                url {
                    parameters.append("q", query)
                    parameters.append("limit", limit.toString())
                }
            }
            val body = response.bodyAsText()
            log("search response len: ${body.length}")
            if (body.contains("\"error\"")) {
                log("ERR: search error: ${body.take(300)}")
            }
            val searchResponse = DeezerJson.json.decodeFromString<SearchResponse>(body)
            val results = searchResponse.data.map { it.toDeezerTrack() }
            log("search found ${results.size} results")
            results
        } catch (e: Exception) {
            log("ERR: search failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchBestMatch(title: String, artist: String): DeezerTrack? {
        val tracks = search(title, artist, 10)
        if (tracks.isEmpty()) return null

        val normalizedTitle = title.trim().lowercase()
        val normalizedArtist = artist.trim().lowercase()

        return tracks.maxByOrNull { track ->
            val tSim = stringSimilarity(normalizedTitle, track.title.trim().lowercase())
            val aSim = stringSimilarity(normalizedArtist, track.artist.trim().lowercase())
            tSim * 0.6 + aSim * 0.4
        }?.takeIf { track ->
            stringSimilarity(normalizedTitle, track.title.trim().lowercase()) > 0.5
        }
    }

    suspend fun resolveStream(trackId: String, quality: String = "MP3_128"): Result<StreamPlan> = runCatching {
        require(loggedIn) { "Not logged in. Call login() first." }

        val trackToken = getTrackToken(trackId)
        val (cdnUrl, format) = resolveMediaUrl(trackToken, quality)

        StreamPlan(
            cdnUrl = cdnUrl,
            trackId = trackId,
            format = format,
            encrypted = true,
            preview = false,
        )
    }

    suspend fun downloadTrack(
        plan: StreamPlan,
        outputFile: File,
        tagBytes: ByteArray? = null
    ): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                log("downloadTrack: GET ${plan.cdnUrl.take(60)}...")
                val response = client.get(plan.cdnUrl) {
                    header("User-Agent", USER_AGENT)
                }
                val encryptedBytes: ByteArray = response.body()
                log("downloadTrack: received ${encryptedBytes.size} encrypted bytes")
                val tmpFile = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.tmp")
                val decryptor = StripeDecryptor(plan.trackId)

                tmpFile.outputStream().use { out ->
                    if (tagBytes != null && tagBytes.isNotEmpty()) {
                        out.write(tagBytes)
                    }
                    val decrypted = decryptor.feed(encryptedBytes)
                    log("downloadTrack: decrypted feed=${decrypted.size} bytes")
                    if (decrypted.isNotEmpty()) out.write(decrypted)
                    val remaining = decryptor.finish()
                    log("downloadTrack: decrypted finish=${remaining.size} bytes")
                    if (remaining.isNotEmpty()) out.write(remaining)
                }

                if (!tmpFile.renameTo(outputFile)) {
                    throw IllegalStateException("Failed to rename temp file")
                }
                log("downloadTrack: OK -> ${outputFile.name} (${outputFile.length()} bytes)")
                outputFile
            }
        }

    suspend fun searchAndDownload(
        title: String,
        artist: String,
        outputDir: File,
        quality: String = "MP3_128"
    ): Result<File> = runCatching {
        require(loggedIn) { "Not logged in" }
        log("searchAndDownload: '$title' by '$artist' (quality=$quality)")
        val track = searchBestMatch(title, artist)
            ?: throw IllegalStateException("No match found on Deezer for '$title' '$artist'")
        log("Best match: ${track.title} by ${track.artist} (id=${track.id})")
        val plan = resolveStream(track.id, quality).getOrThrow()
        log("Stream plan: format=${plan.format}, url=${plan.cdnUrl.take(50)}...")
        
        var artworkBytes: ByteArray? = null
        val artworkUrl = track.artworkUrl
        if (artworkUrl != null && artworkUrl.isNotEmpty()) {
            try {
                log("Downloading artwork: $artworkUrl")
                val artworkResponse = client.get(artworkUrl)
                artworkBytes = artworkResponse.body()
                log("Artwork downloaded successfully: ${artworkBytes?.size ?: 0} bytes")
            } catch (e: Exception) {
                log("Warning: Failed to download artwork: ${e.message}")
            }
        }

        val tagBytes = buildID3v23Tag(track.title, track.artist, track.album, artworkBytes)
        log("Built ID3v2.3 tag: ${tagBytes.size} bytes")

        val safeTitle = track.title.replace(Regex("""[<>:"/\\|?*]"""), "_").take(100)
        val safeArtist = track.artist.replace(Regex("""[<>:"/\\|?*]"""), "_").take(50)
        outputDir.mkdirs()
        val outputFile = File(outputDir, "$safeArtist - $safeTitle.mp3")
        log("Downloading to: $outputFile")
        downloadTrack(plan, outputFile, tagBytes).getOrThrow()
        log("Download OK: ${outputFile.length()} bytes")
        outputFile
    }

    private fun buildID3v23Tag(title: String, artist: String, album: String, imageBytes: ByteArray?): ByteArray {
        val bos = java.io.ByteArrayOutputStream()
        
        fun writeTextFrame(id: String, value: String) {
            val textBytes = value.toByteArray(Charsets.UTF_8)
            val data = ByteArray(1 + textBytes.size)
            data[0] = 3 // UTF-8 encoding
            System.arraycopy(textBytes, 0, data, 1, textBytes.size)
            
            bos.write(id.toByteArray(Charsets.US_ASCII))
            bos.write(toBigEndian(data.size))
            bos.write(byteArrayOf(0, 0)) // Flags
            bos.write(data)
        }
        
        writeTextFrame("TIT2", title)
        writeTextFrame("TPE1", artist)
        writeTextFrame("TALB", album)
        
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val mimeType = "image/jpeg".toByteArray(Charsets.US_ASCII)
            val headerSize = 1 + mimeType.size + 1 + 1 + 1 // Encoding (1) + Mime (mime.size) + Null (1) + PictureType (1) + Description Null (1)
            val data = ByteArray(headerSize + imageBytes.size)
            var offset = 0
            data[offset++] = 3 // UTF-8 encoding
            System.arraycopy(mimeType, 0, data, offset, mimeType.size)
            offset += mimeType.size
            data[offset++] = 0 // Null terminator for mime type
            data[offset++] = 3 // Picture type: Cover (front)
            data[offset++] = 0 // Null terminator for description (empty string)
            System.arraycopy(imageBytes, 0, data, offset, imageBytes.size)
            
            bos.write("APIC".toByteArray(Charsets.US_ASCII))
            bos.write(toBigEndian(data.size))
            bos.write(byteArrayOf(0, 0)) // Flags
            bos.write(data)
        }
        
        val framesBytes = bos.toByteArray()
        
        // Build the main tag header
        val header = ByteArray(10)
        header[0] = 'I'.code.toByte()
        header[1] = 'D'.code.toByte()
        header[2] = '3'.code.toByte()
        header[3] = 3 // Version 3 (ID3v2.3)
        header[4] = 0 // Revision 0
        header[5] = 0 // Flags
        
        val synchsafeSize = toSynchsafe(framesBytes.size)
        System.arraycopy(synchsafeSize, 0, header, 6, 4)
        
        val result = ByteArray(header.size + framesBytes.size)
        System.arraycopy(header, 0, result, 0, header.size)
        System.arraycopy(framesBytes, 0, result, header.size, framesBytes.size)
        return result
    }

    private fun toBigEndian(value: Int): ByteArray {
        return byteArrayOf(
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    private fun toSynchsafe(value: Int): ByteArray {
        return byteArrayOf(
            ((value shr 21) and 0x7F).toByte(),
            ((value shr 14) and 0x7F).toByte(),
            ((value shr 7) and 0x7F).toByte(),
            (value and 0x7F).toByte()
        )
    }

    private suspend fun getTrackToken(trackId: String): String {
        val url = "$GW_URL?method=song.getData&input=3&api_version=1.0&api_token=$apiToken"
        log("getTrackToken: trackId=$trackId")
        val response = client.post(url) {
            header("Cookie", "arl=$arl; sid=$sid")
            header("User-Agent", USER_AGENT)
            contentType(ContentType.Application.Json)
            setBody("""{"sng_id":$trackId}""")
        }
        val responseText = response.bodyAsText()
        log("getTrackToken response len: ${responseText.length}")
        if (responseText.contains("\"error\"")) {
            log("ERR: getTrackToken error: ${responseText.take(500)}")
        }
        val parsed = DeezerJson.json.decodeFromString<SongDataResponse>(responseText)
        val token = parsed.results?.trackToken
            ?: throw IllegalStateException("No track token in response. Body: ${responseText.take(200)}")
        log("getTrackToken OK: token=${token.take(20)}...")
        return token
    }

    private suspend fun resolveMediaUrl(trackToken: String, quality: String): Pair<String, String> {
        val formatsJson = when (quality) {
            "FLAC" -> """[
                {"cipher":"BF_CBC_STRIPE","format":"FLAC"},
                {"cipher":"BF_CBC_STRIPE","format":"MP3_320"},
                {"cipher":"BF_CBC_STRIPE","format":"MP3_128"}
            ]"""
            "MP3_320" -> """[
                {"cipher":"BF_CBC_STRIPE","format":"MP3_320"},
                {"cipher":"BF_CBC_STRIPE","format":"MP3_128"}
            ]"""
            else -> """[
                {"cipher":"BF_CBC_STRIPE","format":"MP3_128"},
                {"cipher":"BF_CBC_STRIPE","format":"MP3_64"},
                {"cipher":"BF_CBC_STRIPE","format":"MP3_MISC"}
            ]"""
        }
        val body = """{
            "license_token":"$licenseToken",
            "media":[{"type":"FULL","formats":$formatsJson}],
            "track_tokens":["$trackToken"]
        }"""
        log("resolveMediaUrl: token=${trackToken.take(20)}..., quality=$quality...")

        val response = client.post(MEDIA_URL) {
            header("User-Agent", USER_AGENT)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val responseText = response.bodyAsText()
        log("resolveMediaUrl response len: ${responseText.length}")
        if (responseText.contains("\"error\"")) {
            log("ERR: resolveMediaUrl error: ${responseText.take(500)}")
        }
        val parsed = DeezerJson.json.decodeFromString<MediaResponse>(responseText)

        val firstData = parsed.data?.firstOrNull()
            ?: throw IllegalStateException("No media data: ${parsed.errorMessage()}. Body: ${responseText.take(300)}")
        val firstMedia = firstData.media?.firstOrNull()
            ?: throw IllegalStateException("No media source: ${parsed.errorMessage()}")
        val sourceUrl = firstMedia.sources?.firstOrNull()?.url
            ?: throw IllegalStateException("No CDN URL: ${parsed.errorMessage()}")

        log("resolveMediaUrl OK: url=${sourceUrl.take(60)}..., format=${firstMedia.format}")
        return Pair(sourceUrl, firstMedia.format)
    }

    private fun parseSidCookie(setCookie: String?): String {
        if (setCookie == null) return ""
        for (part in setCookie.split(";")) {
            val trimmed = part.trim()
            if (trimmed.startsWith("sid=", ignoreCase = true)) {
                return trimmed.removePrefix("sid=").trim()
            }
        }
        return ""
    }

    private fun stringSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        return 1.0 - levenshteinDistance(s1, s2).toDouble() / maxOf(s1.length, s2.length)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun DeezerTrackRs.toDeezerTrack() = DeezerTrack(
        id = id.toString(),
        title = title,
        durationSeconds = duration,
        artist = artist?.name ?: "",
        artistId = artist?.id?.toString() ?: "",
        album = album?.title ?: "",
        artworkUrl = album?.cover_medium,
        explicit = explicit_lyrics,
    )
}
