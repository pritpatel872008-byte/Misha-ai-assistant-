import asyncio
import logging
import time
import numpy as np
from typing import Optional, Callable, Dict, Any, Tuple
from app.voice.models import VoiceState, ParsedActionIntent, ActionExecutionResult, ActionVerificationStatus
from app.voice.state import VoiceStateMachine
from app.voice.audio_input import AudioFrameProcessor
from app.voice.wake_word import OpenWakeWordEngine
from app.voice.vad import SileroVADEngine
from app.voice.stt import SpeechToTextEngine
from app.voice.tts import TextToSpeechEngine
from app.voice.models_manager import ModelManager
from app.ai.gemini import GeminiAIEngine
from app.ai.intent_parser import IntentParser

logger = logging.getLogger("MishaVoicePipeline")

class VoicePipeline:
    """
    Authoritative Python Voice Processing Pipeline for MISHA AI.
    Executes:
    AudioFrame -> Preprocessing -> VAD -> WakeWord -> CommandCapture -> STT -> Intent -> Android Action -> TTS Audio Stream
    """
    def __init__(
        self,
        send_to_android_ws: Optional[Callable[[Dict[str, Any]], Any]] = None,
        send_audio_to_android_ws: Optional[Callable[[bytes], Any]] = None
    ):
        self.send_to_android = send_to_android_ws
        self.send_audio_to_android = send_audio_to_android_ws

        # Core Engines
        self.state_machine = VoiceStateMachine(self._on_state_changed)
        self.audio_processor = AudioFrameProcessor(sample_rate=16000, frame_size=1024)
        self.wake_engine = OpenWakeWordEngine()
        self.vad_engine = SileroVADEngine()
        self.stt_engine = SpeechToTextEngine()
        self.tts_engine = TextToSpeechEngine()
        
        self.model_manager = ModelManager()
        self.model_manager.register_model(self.wake_engine.model_info)
        self.model_manager.register_model(self.vad_engine.model_info)
        self.model_manager.register_model(self.stt_engine.model_info)
        self.model_manager.register_model(self.tts_engine.model_info)

        self.gemini_ai = GeminiAIEngine()
        self.intent_parser = IntentParser(self.gemini_ai)

        # Audio Command Recording Buffer
        self._command_audio_chunks = []
        self._max_command_duration_sec = 8.0
        self._command_start_time = 0.0
        self._pending_actions: Dict[str, asyncio.Future] = {}

    def _on_state_changed(self, new_state: VoiceState):
        logger.info(f"Pipeline state updated to: {new_state.value}")
        if self.send_to_android:
            asyncio.create_task(self._notify_android_state(new_state))

    async def _notify_android_state(self, state: VoiceState):
        if self.send_to_android:
            try:
                await self.send_to_android({
                    "type": "state_changed",
                    "state": state.value,
                    "timestamp": int(time.time() * 1000)
                })
            except Exception as e:
                logger.error(f"Failed to notify Android of state change: {e}")

    async def process_incoming_audio_frame(self, raw_pcm_bytes: bytes):
        """
        Main entry point for incoming 16kHz 16-bit Mono PCM audio frame from Android Mic.
        """
        if self.state_machine.is_tts_suppressed:
            # Drop audio while TTS is actively playing to prevent acoustic self-triggering
            return

        pcm16_data = self.audio_processor.bytes_to_pcm16(raw_pcm_bytes)
        rms = self.audio_processor.compute_rms(pcm16_data)
        current_state = self.state_machine.current_state

        # 1. Standby / Listening for Wake Word
        if current_state in (VoiceState.STANDBY, VoiceState.LISTENING_FOR_WAKE):
            is_wake, confidence = self.wake_engine.process_frame(pcm16_data)
            if is_wake:
                logger.info(f"WAKE WORD DETECTED: 'Misha' (confidence={confidence:.2f})")
                self.state_machine.set_state(VoiceState.WAKE_DETECTED, "Wake word matched")
                
                if self.send_to_android:
                    await self.send_to_android({
                        "type": "wake_detected",
                        "confidence": float(confidence),
                        "timestamp": int(time.time() * 1000)
                    })

                # Transition to listening for command
                await asyncio.sleep(0.3)
                self.state_machine.set_state(VoiceState.LISTENING_FOR_COMMAND, "Listening for user command")
                self.vad_engine.reset()
                self.stt_engine.reset()
                self._command_audio_chunks = []
                self._command_start_time = time.time()
                return

        # 2. Listening for user command & VAD tracking
        elif current_state == VoiceState.LISTENING_FOR_COMMAND:
            self._command_audio_chunks.append(raw_pcm_bytes)
            is_voice, utterance_ended, prob = self.vad_engine.process_frame(pcm16_data, rms)
            
            # Incremental streaming STT
            is_final_part, partial_text = self.stt_engine.accept_waveform(raw_pcm_bytes)
            if partial_text and self.send_to_android:
                await self.send_to_android({
                    "type": "stt_transcript",
                    "transcript": partial_text,
                    "is_final": False
                })

            time_elapsed = time.time() - self._command_start_time
            if utterance_ended or time_elapsed >= self._max_command_duration_sec:
                logger.info(f"Utterance complete (duration: {time_elapsed:.2f}s). Starting transcription.")
                self.state_machine.set_state(VoiceState.TRANSCRIBING, "Utterance ended")
                
                # Combine accumulated PCM
                full_audio = b"".join(self._command_audio_chunks)
                final_transcript = self.stt_engine.transcribe_audio_buffer(full_audio)
                if not final_transcript and partial_text:
                    final_transcript = partial_text

                logger.info(f"Final Transcript: '{final_transcript}'")
                if self.send_to_android:
                    await self.send_to_android({
                        "type": "stt_transcript",
                        "transcript": final_transcript,
                        "is_final": True
                    })

                if final_transcript.strip():
                    await self._process_command(final_transcript)
                else:
                    self.state_machine.set_state(VoiceState.STANDBY, "No speech detected in utterance")

    async def _process_command(self, transcript: str):
        self.state_machine.set_state(VoiceState.THINKING, "Parsing intent with AI engine")
        
        # Parse intent
        intent = await self.intent_parser.parse(transcript)
        logger.info(f"Parsed Intent: {intent.action} with params: {intent.parameters}")

        # Send intent to Android for execution
        self.state_machine.set_state(VoiceState.EXECUTING, f"Executing action: {intent.action}")
        
        if self.send_to_android:
            await self.send_to_android({
                "type": "intent_parsed",
                "intent": {
                    "request_id": intent.request_id,
                    "action": intent.action,
                    "parameters": intent.parameters,
                    "spoken_confirmation": intent.spoken_confirmation
                }
            })

        # Generate conversational response via Gemini if general query
        spoken_response = intent.spoken_confirmation
        if intent.action == "general_query" or not spoken_response:
            ai_reply = await self.gemini_ai.generate_response(transcript)
            spoken_response = ai_reply if ai_reply else f"I heard you say: {transcript}"

        if spoken_response:
            await self.synthesize_and_stream_tts(spoken_response)
        else:
            self.state_machine.set_state(VoiceState.STANDBY, "Command completed")

    async def handle_android_action_result(self, result_dict: Dict[str, Any]):
        """
        Receives action execution result from Android (VERIFIED_SUCCESS, FAILED, etc.)
        """
        status = result_dict.get("status", "UNKNOWN")
        msg = result_dict.get("message", "")
        spoken = result_dict.get("spoken_feedback")
        logger.info(f"Android Action Result: {status} - {msg}")

        if spoken:
            await self.synthesize_and_stream_tts(spoken)
        else:
            self.state_machine.set_state(VoiceState.STANDBY, "Action result processed")

    async def synthesize_and_stream_tts(self, text: str):
        """
        Synthesizes speech using Piper TTS, enforces echo suppression, and streams PCM to Android speaker.
        """
        logger.info(f"Synthesizing TTS: '{text}'")
        self.state_machine.on_tts_started()
        
        if self.send_to_android:
            await self.send_to_android({
                "type": "tts_started",
                "text": text,
                "timestamp": int(time.time() * 1000)
            })

        pcm_audio = self.tts_engine.synthesize_to_pcm16(text)
        
        if pcm_audio and self.send_audio_to_android:
            # Stream binary PCM audio buffer to Android speaker
            await self.send_audio_to_android(pcm_audio)

        # Notify playback completion
        self.state_machine.on_tts_finished()
        if self.send_to_android:
            await self.send_to_android({
                "type": "tts_finished",
                "timestamp": int(time.time() * 1000)
            })
