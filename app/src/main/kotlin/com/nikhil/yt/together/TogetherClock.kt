/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.together

import androidx.compose.runtime.Immutable
import kotlin.math.absoluteValue

@Immutable
data class TogetherClockSnapshot(
    val estimatedOffsetMs: Long = 0L,
    val estimatedRttMs: Long = 0L,
)

class TogetherClock {
    // offset = host elapsed - guest elapsed (phones boot at different times, so this is usually large).
    private var offsetMs: Double = 0.0
    private var rttMs: Double = 0.0
    private var hasSample = false

    fun onPong(
        sentAtElapsedMs: Long,
        receivedAtElapsedMs: Long,
        serverElapsedMs: Long,
    ): TogetherClockSnapshot {
        val rtt = (receivedAtElapsedMs - sentAtElapsedMs).coerceAtLeast(0L)
        val mid = sentAtElapsedMs + rtt / 2L
        val newOffset = (serverElapsedMs - mid).toDouble()

        if (!hasSample) {
            rttMs = rtt.toDouble()
            offsetMs = newOffset
            hasSample = true
        } else {
            // Follow real clock jumps quickly, smooth out network jitter.
            val offsetAlpha = if ((newOffset - offsetMs).absoluteValue > 1500.0) 0.6 else 0.2
            rttMs += (rtt - rttMs) * 0.15
            offsetMs += (newOffset - offsetMs) * offsetAlpha
        }

        return snapshot()
    }

    /** Converts a host elapsedRealtime timestamp to this device's, or null before the first pong. */
    fun toLocalElapsed(serverElapsedMs: Long): Long? =
        if (hasSample) serverElapsedMs - offsetMs.toLong() else null

    fun snapshot(): TogetherClockSnapshot =
        TogetherClockSnapshot(
            estimatedOffsetMs = offsetMs.toLong(),
            estimatedRttMs = rttMs.toLong(),
        )
}
