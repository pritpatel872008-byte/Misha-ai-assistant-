package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class MishaTextToSpeech(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var triedGoogleEngine = true
    private var onSpeechStartCallback: (() -> Unit)? = null
    private var onSpeechDoneCallback: (() -> Unit)? = null
    private var onSpeechErrorCallback: (() -> Unit)? = null

    var pitch: Float = 1.0f
        set(value) { field = value; tts?.setPitch(value) }

    var speed: Float = 1.0f
        set(value) { field = value; tts?.setSpeechRate(value) }

    init {
        // Prefer Google's installed TTS engine, then fall back to the device default.
        tts = TextToSpeech(appContext, this, GOOGLE_TTS_PACKAGE)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            if (triedGoogleEngine) {
                triedGoogleEngine = false
                Log.w(TAG, "Google TTS unavailable; using device default engine")
                tts?.shutdown()
                tts = TextToSpeech(appContext, this)
            } else {
                Log.e(TAG, "Text-to-speech initialization failed: $status")
            }
            return
        }

        val languageResult = tts?.setLanguage(Locale.forLanguageTag("en-IN"))
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) tts?.setLanguage(Locale.ENGLISH)

        tts?.setPitch(pitch)
        tts?.setSpeechRate(speed)
        setupListener()
        isInitialized = true
        Log.i(TAG, "TTS initialized (engine=${tts?.defaultEngine ?: "device default"})")
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { onSpeechStartCallback?.invoke() }
            override fun onDone(utteranceId: String?) { onSpeechDoneCallback?.invoke() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { onSpeechErrorCallback?.invoke() }
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "TTS utterance failed: $errorCode")
                onSpeechErrorCallback?.invoke()
            }
        })
    }

    fun setCallbacks(onStart: () -> Unit, onDone: () -> Unit, onError: () -> Unit) {
        onSpeechStartCallback = onStart
        onSpeechDoneCallback = onDone
        onSpeechErrorCallback = onError
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!isInitialized) {
            Log.w(TAG, "TTS is not ready yet")
            onSpeechErrorCallback?.invoke()
            return
        }
        if (tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString()) == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS rejected utterance")
            onSpeechErrorCallback?.invoke()
        }
    }

    fun stop() { tts?.stop() }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private companion object {
        const val TAG = "MishaTTS"
        const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
    }
}
