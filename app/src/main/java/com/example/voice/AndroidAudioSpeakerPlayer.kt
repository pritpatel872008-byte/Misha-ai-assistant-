package com.example.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * High-fidelity Android speaker audio player for streaming PCM/WAV synthesized by Python Piper TTS.
 * Strictly enforces echo suppression during playback.
 */
class AndroidAudioSpeakerPlayer private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playbackJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = maxOf(
        AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat),
        4096
    )

    private val audioQueue = ConcurrentLinkedQueue<ByteArray>()

    @Synchronized
    fun playPcmStream(pcmBytes: ByteArray, onPlaybackComplete: (() -> Unit)? = null) {
        if (pcmBytes.isEmpty()) return

        stopPlayback()
        EchoPreventionManager.onTtsStarted()

        playbackJob = scope.launch {
            try {
                val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(audioFormat)
                                .setSampleRate(sampleRate)
                                .setChannelMask(channelConfig)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                audioTrack = track
                track.play()

                // Write PCM buffer in chunks
                val chunkSize = 2048
                var offset = 0
                while (isActive && offset < pcmBytes.size) {
                    val bytesToWrite = minOf(chunkSize, pcmBytes.size - offset)
                    val written = track.write(pcmBytes, offset, bytesToWrite)
                    if (written > 0) {
                        offset += written
                    } else {
                        delay(10)
                    }
                }

                // Allow final buffer to drain
                delay(150)
                track.stop()
                track.release()
                audioTrack = null

            } catch (e: Exception) {
                Log.e("AndroidSpeakerPlayer", "AudioTrack playback error", e)
            } finally {
                EchoPreventionManager.onTtsFinished()
                withContext(Dispatchers.Main) {
                    onPlaybackComplete?.invoke()
                }
            }
        }
    }

    @Synchronized
    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e("AndroidSpeakerPlayer", "Error stopping AudioTrack", e)
        }
        audioTrack = null
        EchoPreventionManager.onTtsFinished()
    }

    companion object {
        val instance: AndroidAudioSpeakerPlayer by lazy { AndroidAudioSpeakerPlayer() }
    }
}
