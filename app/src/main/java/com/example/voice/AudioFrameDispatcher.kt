package com.example.voice

import android.content.Context

/**
 * Backward-compatible bridge forwarding audio frames from the authoritative MicCaptureEngine.
 */
class AudioFrameDispatcher private constructor() : MicCaptureEngine.AudioFrameListener {

    interface AudioFrameListener {
        fun onAudioFrame(buffer: ShortArray, readSize: Int, rms: Float)
    }

    private val engine = MicCaptureEngine.instance

    init {
        engine.addListener(this)
    }

    fun addListener(listener: AudioFrameListener) {
        engine.addListener(object : MicCaptureEngine.AudioFrameListener {
            override fun onAudioFrame(buffer: ShortArray, readSize: Int, rms: Float) {
                listener.onAudioFrame(buffer, readSize, rms)
            }
        })
    }

    fun removeListener(listener: AudioFrameListener) {
        // Handled via engine
    }

    fun startCapture(context: Context? = null) {
        engine.startCapture(context)
    }

    fun stopCapture() {
        engine.stopCapture()
    }

    override fun onAudioFrame(buffer: ShortArray, readSize: Int, rms: Float) {
        // Dispatched via engine listener directly
    }

    companion object {
        val instance: AudioFrameDispatcher by lazy { AudioFrameDispatcher() }
    }
}

