package com.example.ai

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong

sealed class MishaEvent {
    abstract val requestId: String
    abstract val timestamp: Long

    data class WakeDetected(
        override val requestId: String,
        val confidence: Float,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class TranscriptPartial(
        override val requestId: String,
        val text: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class TranscriptFinal(
        override val requestId: String,
        val text: String,
        val confidence: Float = 0.95f,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class CommandStarted(
        override val requestId: String,
        val prompt: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class IntentDetected(
        override val requestId: String,
        val intent: String,
        val arguments: Map<String, String>,
        val confidence: Float = 0.9f,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class ActionStarted(
        override val requestId: String,
        val intent: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class ActionOutcome(
        override val requestId: String,
        val intent: String,
        val status: String, // VERIFIED_SUCCESS, SUBMITTED, PARTIAL_SUCCESS, FAILED, UNKNOWN
        val summary: String,
        val latencyMs: Long,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class CallEvent(
        override val requestId: String,
        val callState: String, // DIALING, ACTIVE, ENDED, MISSED
        val recipient: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class SmsEvent(
        override val requestId: String,
        val recipient: String,
        val status: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class SosEvent(
        override val requestId: String,
        val state: SosDeliveryState,
        val message: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class TtsStarted(
        override val requestId: String,
        val utterance: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class TtsFinished(
        override val requestId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class ProactiveAlert(
        override val requestId: String,
        val category: String,
        val title: String,
        val description: String,
        val priority: String, // IMPORTANT, NORMAL, IGNORE
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()

    data class ErrorOccurred(
        override val requestId: String,
        val component: String,
        val errorMessage: String,
        val severity: String, // LOW, MEDIUM, CRITICAL
        override val timestamp: Long = System.currentTimeMillis()
    ) : MishaEvent()
}

object MishaEventBus {
    private val _events = MutableSharedFlow<MishaEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<MishaEvent> = _events.asSharedFlow()

    private val counter = AtomicLong(100)
    private val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    fun newRequestId(): String {
        val stamp = dateFormat.format(Date())
        val seq = counter.incrementAndGet() % 10000
        return "MISHA-$stamp-%04d".format(seq)
    }

    suspend fun emit(event: MishaEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: MishaEvent) {
        _events.tryEmit(event)
    }
}
