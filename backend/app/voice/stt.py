import json
import logging
import os
import wave
import io
import numpy as np
from typing import Optional, Tuple
from backend.app.voice.models import ModelStatus, ModelInfo

logger = logging.getLogger("MishaSTT")

class SpeechToTextEngine:
    """
    Offline-First Speech-To-Text Engine:
    - Primary: Vosk (Lightweight, real-time streaming, zero internet required)
    - Fallback: faster-whisper / whisper
    """
    def __init__(self, vosk_model_path: Optional[str] = None):
        self.vosk_model_path = vosk_model_path or os.environ.get("VOSK_MODEL_PATH", "models/vosk-model-small-en-us")
        self.vosk_model = None
        self.vosk_recognizer = None
        self.whisper_model = None
        self.model_info = ModelInfo(
            name="Vosk STT",
            model_type="stt",
            path=self.vosk_model_path,
            status=ModelStatus.NOT_INITIALIZED
        )
        self._initialize_stt()

    def _initialize_stt(self):
        self.model_info.status = ModelStatus.LOADING
        try:
            import vosk
            vosk.SetLogLevel(-1)
            if os.path.exists(self.vosk_model_path):
                self.vosk_model = vosk.Model(self.vosk_model_path)
                self.vosk_recognizer = vosk.KaldiRecognizer(self.vosk_model, 16000)
                self.model_info.status = ModelStatus.LOADED
                logger.info(f"Vosk STT Model loaded from {self.vosk_model_path}")
            else:
                logger.warning(f"Vosk model path {self.vosk_model_path} not found on disk.")
                self.model_info.status = ModelStatus.MISSING
                self.model_info.error_message = f"Vosk model not found at {self.vosk_model_path}"
                self._try_init_whisper()
        except ImportError:
            logger.warning("Vosk python library not installed. Checking for Whisper fallback.")
            self._try_init_whisper()
        except Exception as e:
            logger.error(f"Vosk initialization error: {e}")
            self.model_info.status = ModelStatus.FAILED
            self.model_info.error_message = str(e)
            self._try_init_whisper()

    def _try_init_whisper(self):
        try:
            from faster_whisper import WhisperModel
            self.whisper_model = WhisperModel("tiny.en", device="cpu", compute_type="int8")
            self.model_info.name = "faster-whisper (tiny.en)"
            self.model_info.status = ModelStatus.LOADED
            logger.info("faster-whisper model loaded as fallback.")
        except Exception as e:
            logger.warning(f"faster-whisper fallback unavailable: {e}")

    def accept_waveform(self, pcm16_bytes: bytes) -> Tuple[bool, str]:
        """
        Feeds streaming audio chunk to Vosk recognizer.
        Returns: (is_final_result, transcript_text)
        """
        if self.vosk_recognizer is not None:
            try:
                if self.vosk_recognizer.AcceptWaveform(pcm16_bytes):
                    res = json.loads(self.vosk_recognizer.Result())
                    text = res.get("text", "").strip()
                    return True, text
                else:
                    partial = json.loads(self.vosk_recognizer.PartialResult())
                    text = partial.get("partial", "").strip()
                    return False, text
            except Exception as e:
                logger.error(f"Vosk streaming STT error: {e}")

        return False, ""

    def transcribe_audio_buffer(self, pcm16_bytes: bytes) -> str:
        """
        Transcribes a complete recorded audio buffer (e.g. at end of speech).
        """
        if self.vosk_model is not None:
            try:
                import vosk
                rec = vosk.KaldiRecognizer(self.vosk_model, 16000)
                rec.AcceptWaveform(pcm16_bytes)
                res = json.loads(rec.FinalResult())
                return res.get("text", "").strip()
            except Exception as e:
                logger.error(f"Vosk final transcription error: {e}")

        if self.whisper_model is not None:
            try:
                audio_np = np.frombuffer(pcm16_bytes, dtype=np.int16).astype(np.float32) / 32768.0
                segments, _ = self.whisper_model.transcribe(audio_np, beam_size=1)
                text = " ".join([seg.text for seg in segments]).strip()
                return text
            except Exception as e:
                logger.error(f"Whisper transcription error: {e}")

        return ""

    def reset(self):
        if self.vosk_model is not None:
            try:
                import vosk
                self.vosk_recognizer = vosk.KaldiRecognizer(self.vosk_model, 16000)
            except Exception:
                pass
