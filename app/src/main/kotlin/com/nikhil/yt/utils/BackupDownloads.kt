/*
 * Velune - by Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.utils

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.nikhil.yt.db.MusicDatabase
import com.nikhil.yt.playback.DownloadUtil
import com.nikhil.yt.playback.ExoDownloadService
import java.io.File

/**
 * Backups don't contain audio files, so they list the downloaded song ids instead.
 * Restore saves that list as "pending" (restore restarts the app) until the user re-downloads or dismisses it.
 */
object BackupDownloads {
    const val ENTRY = "downloads.txt"
    /** Set on the relaunch after a restore so the app asks once. */
    const val EXTRA_OFFER_REDOWNLOAD = "offer_redownload"
    private const val PENDING_FILE = "pending_redownloads.txt"

    fun encode(songIds: Collection<String>): String = songIds.joinToString("\n")

    fun decode(text: String): List<String> =
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.distinct().toList()

    fun completedIds(downloadUtil: DownloadUtil): List<String> =
        downloadUtil.downloads.value.filterValues { it.state == Download.STATE_COMPLETED }.keys.toList()

    private fun pendingFile(context: Context) = File(context.filesDir, PENDING_FILE)

    fun savePending(context: Context, text: String) {
        val ids = decode(text)
        if (ids.isEmpty()) clearPending(context) else pendingFile(context).writeText(encode(ids))
    }

    fun readPending(context: Context): List<String> =
        runCatching { decode(pendingFile(context).readText()) }.getOrDefault(emptyList())

    fun clearPending(context: Context) {
        pendingFile(context).delete()
    }

    /** Queues every pending song that isn't downloaded yet, then clears the pending list. */
    suspend fun redownloadPending(context: Context, database: MusicDatabase, downloadUtil: DownloadUtil) {
        val downloads = downloadUtil.downloads.value
        readPending(context)
            .filter { downloads[it]?.state != Download.STATE_COMPLETED }
            .forEach { id ->
                // Restored songs are in the database; its title labels the download notification.
                val title = database.getSongById(id)?.song?.title ?: id
                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    DownloadRequest.Builder(id, id.toUri())
                        .setCustomCacheKey(id)
                        .setData(title.toByteArray())
                        .build(),
                    false,
                )
            }
        clearPending(context)
    }
}
