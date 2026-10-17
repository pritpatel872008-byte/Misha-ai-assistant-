package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

class MishaTextToSpeech(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onSpeechStartCallback: (() -> Unit)? = null
    private var onSpeechDoneCallback: (() -> Unit)? = null
    private var onSpeechErrorCallback: (() -> Unit)? = null

    var pitch: Float = 1.0f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    var speed: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("MishaTTS", "Language missing or not supported, trying default")
                tts?.language = Locale.getDefault()
            }
            tts?.setPitch(pitch)
            tts?.setSpeechRate(speed)
            setupListener()
            isInitialized = true
        } else {
            Log.e("MishaTTS", "TTS Initialization failed with code $status")
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeechStartCallback?.invoke()
            }

            override fun onDone(utteranceId: String?) {
                onSpeechDoneCallback?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onSpeechErrorCallback?.invoke()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onSpeechErrorCallback?.invoke()
            }
        })
    }

    fun setCallbacks(
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: () -> Unit
    ) {
        this.onSpeechStartCallback = onStart
        this.onSpeechDoneCallback = onDone
        this.onSpeechErrorCallback = onError
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Log.w("MishaTTS", "TTS not yet initialized, dropping speech: $text")
            return
        }
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
