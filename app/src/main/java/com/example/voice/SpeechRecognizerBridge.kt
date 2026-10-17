package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerBridge(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit = {},
    private val onListeningReady: () -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var requestActive = false
    private var speechStarted = false

    private val listeningTimeout = Runnable {
        if (requestActive && !speechStarted) {
            Log.w("SpeechRecognizerBridge", "Speech input timed out; resetting recognizer")
            finishRequest()
            onError("I couldn't hear you. Check microphone access and try again.")
        }
    }

    fun startListening(languageCode: String = "en-IN") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available. Install or enable a speech recognition service.")
            return
        }

        stopListening()
        MicCaptureEngine.instance.pauseForExternalSpeechRecognizer()
        requestActive = true
        speechStarted = false

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (!requestActive) return
                        mainHandler.removeCallbacks(listeningTimeout)
                        mainHandler.postDelayed(listeningTimeout, 12_000L)
                        onListeningReady()
                    }

                    override fun onBeginningOfSpeech() {
                        speechStarted = true
                        mainHandler.removeCallbacks(listeningTimeout)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        onRmsChanged.invoke(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        // Keep the request alive while the recognizer decodes the captured speech.
                    }

                    override fun onError(error: Int) {
                        if (!requestActive) return
                        finishRequest()
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error. Close other apps using the mic and retry."
                            SpeechRecognizer.ERROR_CLIENT -> "Voice recognition was interrupted. Please try again."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                            SpeechRecognizer.ERROR_NETWORK -> "Speech recognition needs a working network connection."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timed out."
                            SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't understand those words. Try speaking closer to the phone."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Wait a moment and try again."
                            SpeechRecognizer.ERROR_SERVER -> "Speech recognition service failed. Please retry."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear speech. Tap the microphone and try again."
                            else -> "Voice recognition failed (error $error). Please try again."
                        }
                        onError(errorMessage)
                    }

                    override fun onResults(results: Bundle?) {
                        if (!requestActive) return
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        finishRequest()
                        if (text.isNotBlank()) onFinalResult(text)
                        else onError("I couldn't detect any words. Please try again.")
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        if (!requestActive) return
                        val text = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) onPartialResult(text)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechRecognizerBridge", "Failed to start speech recognition", e)
            finishRequest()
            onError(e.message ?: "Failed to start voice recognition.")
        }
    }

    private fun finishRequest() {
        requestActive = false
        mainHandler.removeCallbacks(listeningTimeout)
        val recognizer = speechRecognizer
        speechRecognizer = null
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.w("SpeechRecognizerBridge", "Recognizer cleanup failed", e)
        } finally {
            MicCaptureEngine.instance.resumeFromExternalSpeechRecognizer(context)
        }
    }

    fun stopListening() {
        if (!requestActive && speechRecognizer == null) return
        finishRequest()
    }
}
