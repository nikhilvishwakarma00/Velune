package com.nikhil.yt.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupDownloadsTest {
    @Test
    fun songIdsRoundTripThroughBackupEntry() {
        val ids = listOf("dQw4w9WgXcQ", "a-B_c1", "x")
        assertEquals(ids, BackupDownloads.decode(BackupDownloads.encode(ids)))
    }

    @Test
    fun decodeIgnoresBlankLinesWindowsLineEndsAndDuplicates() {
        assertEquals(listOf("a", "b"), BackupDownloads.decode("a\r\n\r\nb\na\n  \n"))
    }

    @Test
    fun emptyListDecodesToNothing() {
        assertEquals(emptyList<String>(), BackupDownloads.decode(BackupDownloads.encode(emptyList())))
    }
}
