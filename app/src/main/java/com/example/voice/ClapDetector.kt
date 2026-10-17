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
    private var lastFrameWasClap = false

    override fun onAudioFrame(buffer: ShortArray, readSize: Int, rms: Float) {
        if (!isEnabled) return

        val now = SystemClock.uptimeMillis()
        if (now - lastTriggerTime < cooldownMs) return

        // Speech syllables can be loud too; require a short, sharp transient,
        // not just a high frame RMS, before treating audio as a clap.
        val peak = (0 until readSize).maxOfOrNull { kotlin.math.abs(buffer[it].toInt()).toFloat() } ?: 0f

        // Learn only from quiet frames so ordinary speech doesn't inflate the floor.
        if (rms < noiseFloor * 1.5f) {
            noiseFloor = 0.95f * noiseFloor + 0.05f * rms
        }

        val sensitivityMultiplier = when (sensitivity) {
            "HIGH" -> 9f
            "LOW" -> 16f
            else -> 12f
        }
        val peakThreshold = maxOf(6000f, noiseFloor * sensitivityMultiplier)
        val crestFactor = peak / maxOf(rms, 1f)
        val isSharpClap = peak >= peakThreshold && crestFactor >= 3.5f

        if (isSharpClap && !lastFrameWasClap) {
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
        lastFrameWasClap = isSharpClap
    }
}
