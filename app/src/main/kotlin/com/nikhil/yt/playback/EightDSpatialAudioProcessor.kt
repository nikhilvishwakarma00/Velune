/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

/**
 * Real-time "8D" spatial audio effect for Velune.
 *
 * While enabled, the perceived position of the music continuously orbits the
 * listener's head (Front -> Right -> Back -> Left -> Front) using only
 * headphone-friendly cues:
 *
 * - Continuous constant-power stereo panning. The image is never hard-switched
 *   between left and right; gains glide smoothly sample by sample.
 * - Interaural time difference: a short fractional delay (<= 0.65 ms) on the
 *   ear farther from the virtual source, which sells left/right placement.
 * - A gentle high-frequency tilt plus slight attenuation when the image passes
 *   "behind" the listener, which is the main front/back cue on headphones.
 * - A ~0.5 s crossfade whenever the effect is toggled, so enabling/disabling
 *   never clicks or jumps.
 *
 * Runs inside ExoPlayer's audio processor chain, so it applies to everything
 * Velune plays and stacks with the system equalizer. It is intentionally
 * Velune-local: it only touches this app's playback path, never other apps.
 * Works with wired and Bluetooth output alike.
 */
class EightDSpatialAudioProcessor : BaseAudioProcessor() {

    data class Params(
        val enabled: Boolean = EightDAudioDefaults.ENABLED,
        val speedHz: Float = EightDAudioDefaults.SPEED_HZ,
        val intensity: Float = EightDAudioDefaults.INTENSITY,
        val width: Float = EightDAudioDefaults.WIDTH,
        val clockwise: Boolean = EightDAudioDefaults.CLOCKWISE,
        val smoothness: Float = EightDAudioDefaults.SMOOTHNESS,
    )

    /** Written from the service thread, read on the audio thread. */
    @Volatile
    var params: Params = Params()

    private var sampleRateHz = 0
    private var inputChannels = 0
    private var bytesPerSample = 0

    // Rotation state (radians). 0 = front, PI/2 = right, PI = back, 3PI/2 = left.
    private var angleRad = 0.0

    // Current effect mix; glides toward the target so toggles fade smoothly.
    private var wet = 0f

    // Smoothed DSP coefficients (updated per input buffer, interpolated per sample).
    private var sGainL = 1f
    private var sGainR = 1f
    private var sDelayL = 0f
    private var sDelayR = 0f
    private var sTilt = 1f
    private var sAtt = 1f

    // One-pole lowpass state for the "behind" tilt.
    private var tiltStateL = 0f
    private var tiltStateR = 0f

    // Fractional delay lines for the interaural time difference.
    private var delayLineL = FloatArray(0)
    private var delayLineR = FloatArray(0)
    private var delayPos = 0

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val supportedEncoding =
            inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
                inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val supportedChannels = inputAudioFormat.channelCount == 1 || inputAudioFormat.channelCount == 2
        if (!supportedEncoding || !supportedChannels || inputAudioFormat.sampleRate <= 0) {
            // Unsupported stream: stay inactive so ExoPlayer routes audio around us.
            return AudioFormat.NOT_SET
        }
        sampleRateHz = inputAudioFormat.sampleRate
        inputChannels = inputAudioFormat.channelCount
        bytesPerSample = if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) 2 else 4

        val delayCap = (sampleRateHz * MAX_ITD_SECONDS).toInt() + 16
        delayLineL = FloatArray(delayCap)
        delayLineR = FloatArray(delayCap)
        delayPos = 0
        tiltStateL = 0f
        tiltStateR = 0f

        // Mono is upmixed so the orbit still works; stereo passes through.
        // NOTE: positional args - the Java constructor has no Kotlin metadata.
        return AudioFormat(inputAudioFormat.sampleRate, 2, inputAudioFormat.encoding)
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        delayLineL.fill(0f)
        delayLineR.fill(0f)
        delayPos = 0
        tiltStateL = 0f
        tiltStateR = 0f
        // angleRad and wet are intentionally kept so track changes don't jolt.
    }

    override fun onReset() {
        onFlush(AudioProcessor.StreamMetadata.DEFAULT)
        angleRad = 0.0
        wet = 0f
        sGainL = 1f
        sGainR = 1f
        sDelayL = 0f
        sDelayR = 0f
        sTilt = 1f
        sAtt = 1f
        sampleRateHz = 0
        inputChannels = 0
        bytesPerSample = 0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        // Never clobber output the sink hasn't drained yet; it will call back
        // with the same buffer after getOutput() is consumed.
        if (hasPendingOutput() || !inputBuffer.hasRemaining() || bytesPerSample == 0) {
            return
        }
        val p = params // single volatile read for this buffer
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val frames = (limit - position) / (bytesPerSample * inputChannels)
        if (frames <= 0) {
            return
        }
        val output = replaceOutputBuffer(frames * bytesPerSample * 2)

        val bufferSeconds = frames.toDouble() / sampleRateHz
        val direction = if (p.clockwise) 1.0 else -1.0
        val speedHz = p.speedHz.coerceIn(
            EightDAudioDefaults.SPEED_MIN_HZ,
            EightDAudioDefaults.SPEED_MAX_HZ,
        ).toDouble()

        // --- target mix with a ~0.5 s fade for instant but smooth toggling ---
        val goalWet = if (p.enabled) p.intensity.coerceIn(0f, 1f) else 0f
        val wetStart = wet
        val maxWetStep = (bufferSeconds / FADE_SECONDS).toFloat()
        val wetEnd =
            when {
                goalWet > wetStart -> minOf(goalWet, wetStart + maxWetStep)
                goalWet < wetStart -> maxOf(goalWet, wetStart - maxWetStep)
                else -> wetStart
            }
        wet = wetEnd

        // --- advance rotation and compute raw spatial targets at the new angle ---
        val angleEnd = angleRad + direction * 2.0 * PI * speedHz * bufferSeconds
        val raw = spatialTargets(angleEnd, p, sampleRateHz.toFloat())

        // --- smoothness: one-pole glide of every coefficient ---
        // smoothness 0 -> slow/heavy glide, 1 -> tight tracking; the motion itself
        // is already continuous, this mainly softens slider moves and fades.
        val cutoffHz = 4.0 + 46.0 * p.smoothness.coerceIn(0f, 1f)
        val alpha = (1.0 - exp(-2.0 * PI * cutoffHz * bufferSeconds)).toFloat()

        val startGainL = sGainL
        val startGainR = sGainR
        val startDelayL = sDelayL
        val startDelayR = sDelayR
        val startTilt = sTilt
        val startAtt = sAtt

        sGainL += alpha * (raw.gainL - sGainL)
        sGainR += alpha * (raw.gainR - sGainR)
        sDelayL += alpha * (raw.delayL - sDelayL)
        sDelayR += alpha * (raw.delayR - sDelayR)
        sTilt += alpha * (raw.tilt - sTilt)
        sAtt += alpha * (raw.att - sAtt)

        val endGainL = sGainL
        val endGainR = sGainR
        val endDelayL = sDelayL
        val endDelayR = sDelayR
        val endTilt = sTilt
        val endAtt = sAtt

        angleRad = angleEnd

        val delayCap = delayLineL.size
        val isFloat = bytesPerSample == 4
        var inPos = position
        val invFrames = if (frames > 1) 1f / (frames - 1) else 0f

        for (i in 0 until frames) {
            val t = i * invFrames
            val gL = startGainL + (endGainL - startGainL) * t
            val gR = startGainR + (endGainR - startGainR) * t
            val dL = startDelayL + (endDelayL - startDelayL) * t
            val dR = startDelayR + (endDelayR - startDelayR) * t
            val tiltA = startTilt + (endTilt - startTilt) * t
            val att = startAtt + (endAtt - startAtt) * t
            val wetS = wetStart + (wetEnd - wetStart) * t
            val dryS = 1f - wetS

            var xl: Float
            var xr: Float
            if (isFloat) {
                xl = inputBuffer.getFloat(inPos)
                inPos += 4
                xr = if (inputChannels == 2) {
                    val v = inputBuffer.getFloat(inPos)
                    inPos += 4
                    v
                } else {
                    xl
                }
            } else {
                xl = inputBuffer.getShort(inPos) / 32768f
                inPos += 2
                xr = if (inputChannels == 2) {
                    val v = inputBuffer.getShort(inPos) / 32768f
                    inPos += 2
                    v
                } else {
                    xl
                }
            }

            // Constant-power panning.
            var pl = xl * gL
            var pr = xr * gR

            // Interaural time difference via fractional delay lines.
            pl = applyDelay(delayLineL, pl, dL)
            pr = applyDelay(delayLineR, pr, dR)
            delayPos++
            if (delayPos >= delayCap) delayPos = 0

            // "Behind" tilt: gentle one-pole lowpass; tiltA == 1 is transparent.
            tiltStateL += tiltA * (pl - tiltStateL)
            tiltStateR += tiltA * (pr - tiltStateR)
            pl = tiltStateL * att
            pr = tiltStateR * att

            // Crossfade dry/wet, with a soft limiter on the processed path only.
            val outL = xl * dryS + tanhFast(pl * HEADROOM) * wetS
            val outR = xr * dryS + tanhFast(pr * HEADROOM) * wetS

            if (isFloat) {
                output.putFloat(outL)
                output.putFloat(outR)
            } else {
                output.putShort((outL.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
                output.putShort((outR.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
        }

        output.flip()
        inputBuffer.position(limit)
    }

    private data class SpatialTargets(
        val gainL: Float,
        val gainR: Float,
        val delayL: Float, // in samples
        val delayR: Float, // in samples
        val tilt: Float,   // one-pole coefficient, 1 = transparent
        val att: Float,    // behind attenuation
    )

    private fun spatialTargets(theta: Double, p: Params, sampleRate: Float): SpatialTargets {
        val width = p.width.coerceIn(0f, 1f)
        // Lateral position: +1 = hard right, -1 = hard left.
        val pan = (sin(theta) * width).toFloat()
        // Constant-power mapping of pan in [-1, 1] to channel gains.
        val panAngle = ((pan * 0.5f + 0.5f) * (PI / 2.0)).toFloat()
        val gainL = cos(panAngle)
        val gainR = sin(panAngle)

        // 0 at front, 1 fully behind.
        val behind = max(0f, -cos(theta).toFloat())

        // Delay the ear farther from the virtual source (<= 0.65 ms).
        val maxDelaySamples = (MAX_ITD_SECONDS * sampleRate).toFloat()
        val delayL = maxDelaySamples * max(0f, sin(theta).toFloat())
        val delayR = maxDelaySamples * max(0f, -sin(theta).toFloat())

        // Darken + dip slightly when behind: the main front/back headphone cue.
        val tilt = 1f - 0.72f * behind
        val att = 1f - 0.12f * behind

        return SpatialTargets(gainL, gainR, delayL, delayR, tilt, att)
    }

    /** Write-through fractional delay line with linear interpolation. */
    private fun applyDelay(line: FloatArray, x: Float, delaySamples: Float): Float {
        val cap = line.size
        line[delayPos] = x
        if (delaySamples <= 0.001f) {
            return x
        }
        val readPos = delayPos - delaySamples
        val idx0 = ((readPos.toInt() % cap) + cap) % cap
        val idx1 = (idx0 + 1) % cap
        val frac = readPos - kotlin.math.floor(readPos).toFloat()
        return line[idx0] * (1f - frac) + line[idx1] * frac
    }

    /** Fast tanh approximation for gentle limiting (keeps peaks musical). */
    private fun tanhFast(x: Float): Float {
        val x2 = x * x
        return x * (27f + x2) / (27f + 9f * x2)
    }

    private companion object {
        /** Max interaural time difference: ~0.65 ms. */
        const val MAX_ITD_SECONDS = 0.00065
        /** Toggle crossfade duration: instant-feeling but click-free. */
        const val FADE_SECONDS = 0.5
        /** Headroom before the soft limiter on the processed path. */
        const val HEADROOM = 0.9f
    }
}
