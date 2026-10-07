/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.playback

/** Default and range values for the 8D spatial audio effect. */
object EightDAudioDefaults {
    const val ENABLED = false
    /** Full rotation every ~6.7 s: noticeable movement without dizziness. */
    const val SPEED_HZ = 0.15f
    const val SPEED_MIN_HZ = 0.05f
    const val SPEED_MAX_HZ = 0.5f
    const val INTENSITY = 0.8f
    const val WIDTH = 1.0f
    const val CLOCKWISE = true
    const val SMOOTHNESS = 0.85f
}
