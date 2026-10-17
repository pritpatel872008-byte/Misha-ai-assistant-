package com.example.voice

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.ai.MishaParsedIntent
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

enum class PythonBridgeState(val displayName: String) {
    DISCONNECTED("Disconnected (Offline Mode)"),
    CONNECTING("Connecting to Python Voice Engine..."),
    CONNECTED("Connected to Python Engine"),
    RECONNECTING("Reconnecting to Python Engine..."),
    ERROR("Engine Error (Offline Fallback Active)")
}

data class PythonVoiceMessage(
    val type: String, // "heartbeat", "audio_frame", "wake_detected", "vad_event", "stt_transcript", "intent_parsed"
    val payload: String = "",
    val confidence: Float = 0f,
    val intent: MishaParsedIntent? = null
)

/**
 * Robust Client Bridge connecting Android Mic & Tools with Python Voice Engine.
 * Supports Heartbeats, Exponential Backoff, Offline Fallback, and PCM Audio Streaming.
 */
class PythonVoiceBridge(
    private val context: Context,
    private val backendUrl: String = BuildConfig.MISHA_BACKEND_URL.ifBlank { "https://misha-ai-backend.onrender.com" },
    private val onWakeWordDetected: (confidence: Float) -> Unit = {},
    private val onTranscriptReceived: (text: String, isFinal: Boolean) -> Unit = { _, _ -> },
    private val onIntentReceived: (MishaParsedIntent) -> Unit = {},
    private val onStateUpdated: (MishaState) -> Unit = {}
) : MicCaptureEngine.AudioFrameListener {

    private val _connectionState = MutableStateFlow(PythonBridgeState.DISCONNECTED)
    val connectionState: StateFlow<PythonBridgeState> = _connectionState.asStateFlow()

    private val _isBackendAvailable = MutableStateFlow(false)
    val isBackendAvailable: StateFlow<Boolean> = _isBackendAvailable.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private val reconnectAttempts = AtomicInteger(0)
    private val maxReconnectAttempts = 5
    private val baseReconnectDelayMs = 1500L
    private val maxReconnectDelayMs = 30000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var isExplicitlyStopped = false

    fun connect() {
        if (_connectionState.value == PythonBridgeState.CONNECTED ||
            _connectionState.value == PythonBridgeState.CONNECTING
        ) {
            return
        }

        isExplicitlyStopped = false
        _connectionState.value = PythonBridgeState.CONNECTING
        val baseUrl = backendUrl.trim().trimEnd('/')
        val wsBase = when {
            baseUrl.startsWith("https://", ignoreCase = true) -> "wss://" + baseUrl.substring(8)
            baseUrl.startsWith("http://", ignoreCase = true) -> "ws://" + baseUrl.substring(7)
            baseUrl.startsWith("wss://", ignoreCase = true) || baseUrl.startsWith("ws://", ignoreCase = true) -> baseUrl
            else -> "wss://$baseUrl"
        }
        val wsUrl = "$wsBase/voice"
        Log.i("PythonVoiceBridge", "Connecting to Python Voice Engine at $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i("PythonVoiceBridge", "Python Voice Engine WebSocket connected.")
                _connectionState.value = PythonBridgeState.CONNECTED
                _isBackendAvailable.value = true
                reconnectAttempts.set(0)
                startHeartbeat()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                // Binary PCM audio from Python Piper TTS -> Stream to Android speaker
                val pcmBytes = bytes.toByteArray()
                AndroidAudioSpeakerPlayer.instance.playPcmStream(pcmBytes)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.w("PythonVoiceBridge", "WebSocket closing: $code / $reason")
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.i("PythonVoiceBridge", "WebSocket closed.")
                _isBackendAvailable.value = false
                if (!isExplicitlyStopped) {
                    scheduleReconnect()
                } else {
                    _connectionState.value = PythonBridgeState.DISCONNECTED
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w("PythonVoiceBridge", "WebSocket failure: ${t.message}. Operating in Local Fallback mode.")
                _isBackendAvailable.value = false
                _connectionState.value = PythonBridgeState.ERROR
                if (!isExplicitlyStopped) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "pong" -> {
                    // Heartbeat acknowledged
                }
                "wake_detected" -> {
                    val conf = json.optDouble("confidence", 0.9).toFloat()
                    scope.launch(Dispatchers.Main) {
                        onWakeWordDetected(conf)
                    }
                }
                "stt_transcript" -> {
                    val transcript = json.optString("transcript")
                    val isFinal = json.optBoolean("is_final", true)
                    scope.launch(Dispatchers.Main) {
                        onTranscriptReceived(transcript, isFinal)
                    }
                }
                "state_changed" -> {
                    val stateStr = json.optString("state")
                    val mappedState = mapPythonStateToMishaState(stateStr)
                    scope.launch(Dispatchers.Main) {
                        onStateUpdated(mappedState)
                    }
                }
                "tts_started" -> {
                    EchoPreventionManager.onTtsStarted()
                    scope.launch(Dispatchers.Main) {
                        onStateUpdated(MishaState.SPEAKING)
                    }
                }
                "tts_finished" -> {
                    EchoPreventionManager.onTtsFinished()
                    scope.launch(Dispatchers.Main) {
                        onStateUpdated(MishaState.POST_SPEECH_COOLDOWN)
                    }
                }
                "intent_parsed" -> {
                    val intentObj = json.optJSONObject("intent")
                    if (intentObj != null) {
                        val parsed = moshi.adapter(MishaParsedIntent::class.java).fromJson(intentObj.toString())
                        if (parsed != null) {
                            scope.launch(Dispatchers.Main) {
                                onIntentReceived(parsed)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PythonVoiceBridge", "Error parsing message: $text", e)
        }
    }

    private fun mapPythonStateToMishaState(pythonState: String): MishaState {
        return when (pythonState.uppercase()) {
            "STANDBY" -> MishaState.STANDBY
            "LISTENING_FOR_WAKE" -> MishaState.WAKE_LISTENING
            "WAKE_DETECTED" -> MishaState.WAKE_DETECTED
            "LISTENING_FOR_COMMAND" -> MishaState.COMMAND_LISTENING
            "TRANSCRIBING" -> MishaState.PROCESSING
            "THINKING" -> MishaState.THINKING
            "EXECUTING" -> MishaState.EXECUTING
            "SPEAKING" -> MishaState.SPEAKING
            "COOLDOWN" -> MishaState.POST_SPEECH_COOLDOWN
            "ERROR" -> MishaState.ERROR_RECOVERY
            else -> MishaState.STANDBY
        }
    }

    fun sendActionResult(
        requestId: String,
        action: String,
        status: String,
        message: String,
        spokenFeedback: String? = null
    ) {
        if (_connectionState.value != PythonBridgeState.CONNECTED || webSocket == null) {
            return
        }

        try {
            val json = JSONObject().apply {
                put("type", "action_result")
                put("request_id", requestId)
                put("action", action)
                put("status", status)
                put("message", message)
                if (spokenFeedback != null) {
                    put("spoken_feedback", spokenFeedback)
                }
            }.toString()
            webSocket?.send(json)
        } catch (e: Exception) {
            Log.e("PythonVoiceBridge", "Failed to send action result to Python", e)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value == PythonBridgeState.CONNECTED) {
                delay(10000)
                try {
                    val pingJson = JSONObject().apply {
                        put("type", "ping")
                        put("timestamp", System.currentTimeMillis())
                    }.toString()
                    webSocket?.send(pingJson)
                } catch (e: Exception) {
                    Log.w("PythonVoiceBridge", "Failed to send heartbeat ping", e)
                }
            }
        }
    }

    private fun scheduleReconnect() {
        heartbeatJob?.cancel()
        val attempt = reconnectAttempts.incrementAndGet()

        if (attempt > maxReconnectAttempts) {
            Log.i("PythonVoiceBridge", "Max reconnect attempts ($maxReconnectAttempts) reached. Settling into Offline Local Fallback mode.")
            _connectionState.value = PythonBridgeState.DISCONNECTED
            _isBackendAvailable.value = false
            return
        }

        val backoffDelay = minOf(
            baseReconnectDelayMs * (1L shl (attempt - 1)),
            maxReconnectDelayMs
        )

        _connectionState.value = PythonBridgeState.RECONNECTING
        Log.i("PythonVoiceBridge", "Scheduling reconnect attempt #$attempt in ${backoffDelay}ms")

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(backoffDelay)
            if (!isExplicitlyStopped) {
                connect()
            }
        }
    }

    /**
     * Streams raw PCM audio buffer from MicCaptureEngine to Python Voice Engine.
     */
    override fun onAudioFrame(buffer: ShortArray, readSize: Int, rms: Float) {
        if (_connectionState.value != PythonBridgeState.CONNECTED || webSocket == null) {
            return
        }

        try {
            val byteBuffer = ByteBuffer.allocate(readSize * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until readSize) {
                byteBuffer.putShort(buffer[i])
            }
            webSocket?.send(byteBuffer.array().toByteString())
        } catch (e: Exception) {
            Log.e("PythonVoiceBridge", "Audio frame transmission error", e)
        }
    }

    fun disconnect() {
        isExplicitlyStopped = true
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        heartbeatJob = null
        reconnectJob = null

        try {
            webSocket?.close(1000, "App shutdown")
        } catch (e: Exception) {
            Log.e("PythonVoiceBridge", "Error closing WebSocket", e)
        }

        webSocket = null
        _connectionState.value = PythonBridgeState.DISCONNECTED
        _isBackendAvailable.value = false
    }
}
