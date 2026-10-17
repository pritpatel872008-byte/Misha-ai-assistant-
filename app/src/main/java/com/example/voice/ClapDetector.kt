package com.example.voice

import android.os.SystemClock

class ClapDetector(
    private val onDoubleClapDetected: () -> Unit
) : MicCaptureEngine.AudioFrameListener, AudioFrameDispatcher.AudioFrameListener {

    var isEnabled: Boolean = true
    var sensitivity: String = "MEDIUM" // "LOW", "MEDIUM", "HIGH"

    private var firstClapTime: Long = 0L
    private var lastTriggerTime: Long = 0L
    private val minClapIntervalMs = 120L
    private val maxClapIntervalMs = 750L
    private val cooldownMs = 2000L

    private var noiseFloor: Float = 40f

    override fun onAudioFrame(buffer: ShortArray, readSize: Int, rms: Float) {
        if (!isEnabled) return

        val now = SystemClock.uptimeMillis()
        if (now - lastTriggerTime < cooldownMs) return

        // Update noise floor
        if (rms < noiseFloor * 1.5f) {
            noiseFloor = 0.95f * noiseFloor + 0.05f * rms
        }

        val spikeMultiplier = when (sensitivity) {
            "HIGH" -> 4.5f
            "LOW" -> 9.0f
            else -> 6.5f
        }

        val spikeThreshold = maxOf(450f, noiseFloor * spikeMultiplier)

        // A clap is characterized by a sharp transient peak
        if (rms > spikeThreshold) {
            if (firstClapTime == 0L) {
                // First clap registered
                firstClapTime = now
            } else {
                val interval = now - firstClapTime
                if (interval in minClapIntervalMs..maxClapIntervalMs) {
                    // Double clap confirmed
                    firstClapTime = 0L
                    lastTriggerTime = now
                    onDoubleClapDetected()
                } else if (interval > maxClapIntervalMs) {
                    // Stale, reset to this new clap
                    firstClapTime = now
                }
            }
        } else {
            if (firstClapTime > 0L && (now - firstClapTime > maxClapIntervalMs)) {
                firstClapTime = 0L
            }
        }
    }
}
