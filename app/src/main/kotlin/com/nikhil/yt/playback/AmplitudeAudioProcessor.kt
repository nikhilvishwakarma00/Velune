package com.nikhil.yt.playback

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.abs

class AmplitudeAudioProcessor : BaseAudioProcessor() {
    @Volatile var latestAmplitudeL = 0f
    @Volatile var latestAmplitudeR = 0f
    @Volatile var lastBeatTime = 0L
    private var runningAverageEnergy = 0.1f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // We support any PCM input and return it unmodified
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val format = inputAudioFormat
        if (format.encoding == androidx.media3.common.C.ENCODING_PCM_16BIT) {
            val limit = inputBuffer.limit()
            var maxL = 0f
            var maxR = 0f
            val channelCount = format.channelCount

            // Iterate over 16-bit short samples
            var i = inputBuffer.position()
            while (i < limit) {
                for (channel in 0 until channelCount) {
                    if (i + 1 >= limit) break
                    val low = inputBuffer.get(i).toInt() and 0xFF
                    val high = inputBuffer.get(i + 1).toInt()
                    val sample = ((high shl 8) or low).toShort().toFloat() / 32768f
                    val absSample = abs(sample)

                    if (channelCount == 1) {
                        if (absSample > maxL) maxL = absSample
                        maxR = maxL
                    } else {
                        if (channel == 0) {
                            if (absSample > maxL) maxL = absSample
                        } else if (channel == 1) {
                            if (absSample > maxR) maxR = absSample
                        }
                    }
                    i += 2
                }
            }
            
            // Decaying/smoothing filter for VU needle responsiveness
            // We want it to be snappy to rise but smooth to fall
            val decay = 0.85f
            val rise = 0.3f
            
            latestAmplitudeL = if (maxL > latestAmplitudeL) {
                latestAmplitudeL * (1f - rise) + maxL * rise
            } else {
                latestAmplitudeL * decay + maxL * (1f - decay)
            }
            
            latestAmplitudeR = if (maxR > latestAmplitudeR) {
                latestAmplitudeR * (1f - rise) + maxR * rise
            } else {
                latestAmplitudeR * decay + maxR * (1f - decay)
            }

            // Real-time transient beat detection
            val energy = (maxL + maxR) / 2f
            runningAverageEnergy = runningAverageEnergy * 0.98f + energy * 0.02f
            val now = System.currentTimeMillis()
            if (energy > 0.08f && energy > runningAverageEnergy * 1.35f && (now - lastBeatTime > 250)) {
                lastBeatTime = now
            }
        }
        
        // Pass the buffer unmodified
        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }
}
