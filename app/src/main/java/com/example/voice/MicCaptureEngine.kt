package com.example.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

enum class MicState {
    IDLE,
    INITIALIZING,
    RECORDING,
    PAUSED_FOR_EXTERNAL,
    ERROR
}

/**
 * Authoritative Single-Microphone Capture Engine for MISHA AI.
 * Ensures ONE microphone owner across the application lifecycle.
 */
class MicCaptureEngine private constructor() {

    interface AudioFrameListener {
        fun onAudioFrame(buffer: ShortArray, readSize: Int, rms: Float)
        fun onMicStateChanged(state: MicState) {}
    }

    private val listeners = CopyOnWriteArrayList<AudioFrameListener>()
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _micState = MutableStateFlow(MicState.IDLE)
    val micState: StateFlow<MicState> = _micState.asStateFlow()
    val isCapturing: Boolean get() = _micState.value == MicState.RECORDING

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = maxOf(
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
        2048
    )

    private var wasRecordingBeforePause = false

    fun addListener(listener: AudioFrameListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            listener.onMicStateChanged(_micState.value)
        }
    }

    fun removeListener(listener: AudioFrameListener) {
        listeners.remove(listener)
    }

    @Synchronized
    fun startCapture(context: Context? = null): Boolean {
        if (_micState.value == MicState.RECORDING) return true

        if (context != null) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                Log.w("MicCaptureEngine", "Cannot start capture: RECORD_AUDIO permission missing.")
                updateState(MicState.ERROR)
                return false
            }
        }

        updateState(MicState.INITIALIZING)

        try {
            stopAudioRecordInternal()

            @SuppressLint("MissingPermission")
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to standard MIC audio source for broader OEM compatibility
                Log.w("MicCaptureEngine", "VOICE_RECOGNITION source unavailable, falling back to MIC source")
                record.release()

                @SuppressLint("MissingPermission")
                val fallbackRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufferSize * 2
                )

                if (fallbackRecord.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("MicCaptureEngine", "AudioRecord initialization failed completely")
                    fallbackRecord.release()
                    updateState(MicState.ERROR)
                    return false
                }
                audioRecord = fallbackRecord
            } else {
                audioRecord = record
            }

            audioRecord?.startRecording()
            if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e("MicCaptureEngine", "AudioRecord failed to enter RECORDSTATE_RECORDING")
                stopAudioRecordInternal()
                updateState(MicState.ERROR)
                return false
            }

            updateState(MicState.RECORDING)
            startAudioPumpLoop()
            return true

        } catch (e: SecurityException) {
            Log.e("MicCaptureEngine", "SecurityException starting AudioRecord", e)
            stopAudioRecordInternal()
            updateState(MicState.ERROR)
            return false
        } catch (e: Exception) {
            Log.e("MicCaptureEngine", "Exception starting AudioRecord", e)
            stopAudioRecordInternal()
            updateState(MicState.ERROR)
            return false
        }
    }

    private fun startAudioPumpLoop() {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            val buffer = ShortArray(1024)
            while (isActive && _micState.value == MicState.RECORDING) {
                val record = audioRecord
                if (record == null || record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    delay(20)
                    continue
                }

                val read = try {
                    record.read(buffer, 0, buffer.size)
                } catch (e: Exception) {
                    -1
                }

                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        val sample = buffer[i].toDouble()
                        sum += sample * sample
                    }
                    val rms = sqrt(sum / read).toFloat()

                    for (listener in listeners) {
                        try {
                            listener.onAudioFrame(buffer, read, rms)
                        } catch (e: Exception) {
                            Log.e("MicCaptureEngine", "Listener dispatch error", e)
                        }
                    }
                } else if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
                    Log.w("MicCaptureEngine", "AudioRecord read error: $read")
                    delay(50)
                } else {
                    delay(10)
                }
            }
        }
    }

    /**
     * Called when Google SpeechRecognizer needs exclusive microphone access.
     * Prevents hardware conflicts and HAL driver lockouts.
     */
    @Synchronized
    fun pauseForExternalSpeechRecognizer() {
        if (_micState.value == MicState.RECORDING) {
            wasRecordingBeforePause = true
            stopAudioRecordInternal()
            updateState(MicState.PAUSED_FOR_EXTERNAL)
            Log.i("MicCaptureEngine", "Paused mic for SpeechRecognizer")
        }
    }

    /**
     * Resumes background mic capture when SpeechRecognizer completes.
     */
    @Synchronized
    fun resumeFromExternalSpeechRecognizer(context: Context? = null) {
        if (_micState.value == MicState.PAUSED_FOR_EXTERNAL) {
            if (wasRecordingBeforePause) {
                wasRecordingBeforePause = false
                startCapture(context)
                Log.i("MicCaptureEngine", "Resumed mic after SpeechRecognizer")
            } else {
                updateState(MicState.IDLE)
            }
        }
    }

    @Synchronized
    fun stopCapture() {
        wasRecordingBeforePause = false
        stopAudioRecordInternal()
        updateState(MicState.IDLE)
    }

    private fun stopAudioRecordInternal() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("MicCaptureEngine", "Error releasing AudioRecord", e)
        }
        audioRecord = null
    }

    private fun updateState(newState: MicState) {
        _micState.value = newState
        for (listener in listeners) {
            try {
                listener.onMicStateChanged(newState)
            } catch (e: Exception) {
                Log.e("MicCaptureEngine", "Error notifying state change", e)
            }
        }
    }

    companion object {
        val instance: MicCaptureEngine by lazy { MicCaptureEngine() }
    }
}
