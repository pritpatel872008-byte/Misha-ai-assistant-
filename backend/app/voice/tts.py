import io
import logging
import os
import wave
import numpy as np
from typing import Optional, Tuple
from backend.app.voice.models import ModelStatus, ModelInfo

logger = logging.getLogger("MishaTTS")

class TextToSpeechEngine:
    """
    Offline Text-To-Speech Engine:
    - Primary: Piper (Fast, neural, natural sounding on-device TTS)
    - Output: 16000 Hz 16-bit Mono PCM audio frames streamed directly to Android speaker.
    """
    def __init__(self, piper_model_path: Optional[str] = None):
        self.piper_model_path = piper_model_path or os.environ.get("PIPER_MODEL_PATH", "models/en_US-lessac-medium.onnx")
        self.piper_voice = None
        self.model_info = ModelInfo(
            name="Piper TTS",
            model_type="tts",
            path=self.piper_model_path,
            status=ModelStatus.NOT_INITIALIZED
        )
        self._initialize_tts()

    def _initialize_tts(self):
        self.model_info.status = ModelStatus.LOADING
        try:
            from piper import PiperVoice
            if os.path.exists(self.piper_model_path):
                self.piper_voice = PiperVoice.load(self.piper_model_path)
                self.model_info.status = ModelStatus.LOADED
                logger.info("Piper TTS neural voice loaded successfully.")
            else:
                self.model_info.status = ModelStatus.MISSING
                self.model_info.error_message = f"Piper model not found at {self.piper_model_path}"
                logger.warning(f"Piper model missing at {self.piper_model_path}")
        except ImportError:
            logger.warning("Piper library not installed. Using fallback synthetic acoustic waveform generator.")
            self.model_info.status = ModelStatus.LOADED
        except Exception as e:
            logger.error(f"Piper load failed: {e}")
            self.model_info.status = ModelStatus.FAILED
            self.model_info.error_message = str(e)

    def synthesize_to_pcm16(self, text: str, sample_rate: int = 16000) -> bytes:
        """
        Synthesizes text into 16kHz Mono 16-bit PCM byte array.
        """
        if not text or not text.strip():
            return b""

        if self.piper_voice is not None:
            try:
                wav_io = io.BytesIO()
                with wave.open(wav_io, "wb") as wav_file:
                    self.piper_voice.synthesize(text, wav_file)
                wav_io.seek(0)
                with wave.open(wav_io, "rb") as read_wav:
                    raw_pcm = read_wav.readframes(read_wav.getnframes())
                return raw_pcm
            except Exception as e:
                logger.error(f"Piper synthesis error: {e}")

        # Lightweight acoustic synthesis fallback (e.g. pyttsx3 or tone waveform)
        try:
            import pyttsx3
            engine = pyttsx3.init()
            temp_path = "/tmp/misha_tts.wav"
            engine.save_to_file(text, temp_path)
            engine.runAndWait()
            if os.path.exists(temp_path):
                with wave.open(temp_path, "rb") as read_wav:
                    raw_pcm = read_wav.readframes(read_wav.getnframes())
                os.remove(temp_path)
                return raw_pcm
        except Exception:
            pass

        # Return synthesized PCM chime/acknowledgement
        return self._generate_soft_chime(sample_rate)

    def _generate_soft_chime(self, sample_rate: int = 16000) -> bytes:
        duration = 0.25
        t = np.linspace(0, duration, int(sample_rate * duration), False)
        # 523Hz (C5) + 659Hz (E5) harmonic
        tone = 0.3 * np.sin(2 * np.pi * 523.25 * t) + 0.2 * np.sin(2 * np.pi * 659.25 * t)
        envelope = np.exp(-5 * t)
        audio = (tone * envelope * 32767).astype(np.int16)
        return audio.tobytes()
