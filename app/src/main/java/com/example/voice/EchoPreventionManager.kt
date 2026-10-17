package com.example.voice

import android.os.Handler
import android.os.Looper

class EchoPreventionManager(
    private val onStateTransition: ((MishaState) -> Unit)? = null
) {
    private var isTtsSpeaking = false
    private val handler = Handler(Looper.getMainLooper())
    private val postSpeechCooldownDurationMs = 800L

    val isSpeaking: Boolean
        get() = isTtsSpeaking || globalIsSpeaking

    fun onSpeechStart() {
        isTtsSpeaking = true
        globalIsSpeaking = true
        handler.removeCallbacksAndMessages(null)
        onStateTransition?.invoke(MishaState.SPEAKING)
    }

    fun onSpeechFinish() {
        isTtsSpeaking = false
        onStateTransition?.invoke(MishaState.POST_SPEECH_COOLDOWN)
        handler.postDelayed({
            if (!isTtsSpeaking) {
                globalIsSpeaking = false
                onStateTransition?.invoke(MishaState.STANDBY)
            }
        }, postSpeechCooldownDurationMs)
    }

    fun onSpeechError() {
        isTtsSpeaking = false
        globalIsSpeaking = false
        onStateTransition?.invoke(MishaState.STANDBY)
    }

    companion object {
        @Volatile
        var globalIsSpeaking: Boolean = false
            private set

        private val globalHandler = Handler(Looper.getMainLooper())

        fun onTtsStarted() {
            globalIsSpeaking = true
            globalHandler.removeCallbacksAndMessages(null)
        }

        fun onTtsFinished() {
            globalHandler.postDelayed({
                globalIsSpeaking = false
            }, 800L)
        }
    }
}
