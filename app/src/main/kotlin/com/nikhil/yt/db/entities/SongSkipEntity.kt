/*
 * Velune - by Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_skip")
data class SongSkipEntity(
    @PrimaryKey val songId: String,
    val skipCount: Int = 0,
    val lastSkippedAt: Long = System.currentTimeMillis()
)
