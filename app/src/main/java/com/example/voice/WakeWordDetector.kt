package com.example.voice

import android.util.Log

/**
 * Wake Word Bridge Adapter:
 * Coordinates wake detection events emitted by the Python Voice Core (openWakeWord engine).
 * Does NOT run simulated or fake heuristics on Android.
 */
class WakeWordDetector(
    private val onWakeDetected: (confidence: Float) -> Unit
) : MicCaptureEngine.AudioFrameListener, AudioFrameDispatcher.AudioFrameListener {

    var isEnabled: Boolean = true
    var sensitivity: String = "MEDIUM" // "LOW", "MEDIUM", "HIGH"
    var cooldownMs: Long = 1500L

    private var lastWakeTime: Long = 0L

    /**
     * Invoked when Python Voice Core detects and verifies the 'Misha' wake word via openWakeWord.
     */
    fun notifyPythonWakeDetected(confidence: Float) {
        if (!isEnabled) {
            Log.d("WakeWordAdapter", "Wake word detected by Python but adapter is disabled.")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastWakeTime < cooldownMs) {
            Log.d("WakeWordAdapter", "Wake word ignored due to cooldown window.")
            return
        }

        lastWakeTime = now
        Log.i("WakeWordAdapter", "Propagating verified Python wake detection (confidence: $confidence)")
        onWakeDetected(confidence)
    }

    override fun onAudioFrame(buffer: ShortArray, readSize: Int, rms: Float) {
        // Audio frames from MicCaptureEngine are streamed to the Python Voice Core via PythonVoiceBridge.
        // On-device audio processing here is strictly passive.
    }
}
