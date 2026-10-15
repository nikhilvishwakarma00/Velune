package com.nikhil.yt.together

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TogetherClockTest {
    // Guest booted 900s before the host: host elapsed = guest elapsed - 900_000.
    private fun pong(clock: TogetherClock, guestSentAt: Long, outMs: Long = 10L, backMs: Long = 10L) =
        clock.onPong(
            sentAtElapsedMs = guestSentAt,
            receivedAtElapsedMs = guestSentAt + outMs + backMs,
            serverElapsedMs = guestSentAt - 900_000L + outMs,
        )

    @Test
    fun noConversionBeforeFirstPong() {
        assertEquals(null, TogetherClock().toLocalElapsed(100_500L))
    }

    @Test
    fun hostTimeConvertsToGuestTime() {
        val clock = TogetherClock()
        pong(clock, guestSentAt = 1_000_000L)
        // Host stamped a room state at host time 100_500 -> guest time 1_000_500.
        assertEquals(1_000_500L, clock.toLocalElapsed(100_500L))
    }

    @Test
    fun oneJitterySampleOnlyNudgesASteadyOffset() {
        val clock = TogetherClock()
        pong(clock, guestSentAt = 1_000_000L)
        // Asymmetric delay makes this sample read 100ms off; smoothing should move the estimate ~20ms,
        // not 60ms just because the boot-time difference between phones is large.
        pong(clock, guestSentAt = 1_002_000L, outMs = 210L, backMs = 10L)
        val error = abs(clock.snapshot().estimatedOffsetMs + 900_000L)
        assertTrue("offset moved ${error}ms", error <= 30L)
    }
}
