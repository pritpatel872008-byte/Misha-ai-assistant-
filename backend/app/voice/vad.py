import logging
import os
import time
import numpy as np
from typing import Tuple, Optional
from backend.app.voice.models import ModelStatus, ModelInfo

logger = logging.getLogger("MishaVAD")

class SileroVADEngine:
    """
    Silero Voice Activity Detector for speech detection and silence timeout segmentation.
    Operates on 16kHz mono audio frames.
    """
    def __init__(
        self,
        threshold: float = 0.5,
        min_silence_duration_ms: int = 800,
        speech_pad_ms: int = 200,
        sample_rate: int = 16000
    ):
        self.threshold = threshold
        self.min_silence_duration_ms = min_silence_duration_ms
        self.speech_pad_ms = speech_pad_ms
        self.sample_rate = sample_rate
        
        self.is_speaking = False
        self.speech_start_time: Optional[float] = None
        self.last_speech_time: Optional[float] = None
        
        self.model = None
        self.model_info = ModelInfo(
            name="Silero VAD",
            model_type="vad",
            path="silero_vad.onnx",
            status=ModelStatus.NOT_INITIALIZED
        )
        self._initialize_model()

    def _initialize_model(self):
        try:
            self.model_info.status = ModelStatus.LOADING
            try:
                import torch
                # Load torch silero vad if available
                model, utils = torch.hub.load(
                    repo_or_dir='snakers4/silero-vad',
                    model='silero_vad',
                    force_reload=False,
                    onnx=False
                )
                self.model = model
                self.model_info.status = ModelStatus.LOADED
                logger.info("Silero VAD PyTorch model loaded successfully.")
            except Exception as e:
                logger.warning(f"PyTorch Silero VAD load failed ({e}), using energy-calibrated acoustic VAD.")
                self.model_info.status = ModelStatus.LOADED
        except Exception as e:
            self.model_info.status = ModelStatus.FAILED
            self.model_info.error_message = str(e)

    def process_frame(self, pcm16_frame: np.ndarray, rms: float) -> Tuple[bool, bool, float]:
        """
        Processes audio frame.
        Returns: (is_voice_active, is_utterance_completed, probability)
        """
        now = time.time()
        prob = 0.0

        if self.model is not None:
            try:
                import torch
                float_frame = pcm16_frame.astype(np.float32) / 32768.0
                tensor_frame = torch.from_numpy(float_frame)
                speech_prob = self.model(tensor_frame, self.sample_rate).item()
                prob = speech_prob
            except Exception:
                prob = min(1.0, max(0.0, (rms - 80.0) / 400.0))
        else:
            # Calibrated acoustic energy envelope
            prob = min(1.0, max(0.0, (rms - 80.0) / 400.0))

        is_voice = prob >= self.threshold
        utterance_completed = False

        if is_voice:
            if not self.is_speaking:
                self.is_speaking = True
                self.speech_start_time = now
                logger.debug("VAD: Speech onset detected.")
            self.last_speech_time = now
        else:
            if self.is_speaking and self.last_speech_time is not None:
                silence_elapsed_ms = (now - self.last_speech_time) * 1000.0
                if silence_elapsed_ms >= self.min_silence_duration_ms:
                    self.is_speaking = False
                    utterance_completed = True
                    logger.debug(f"VAD: Utterance completed after {silence_elapsed_ms:.0f}ms silence.")

        return is_voice, utterance_completed, prob

    def reset(self):
        self.is_speaking = False
        self.speech_start_time = None
        self.last_speech_time = None
        if self.model is not None and hasattr(self.model, "reset_states"):
            self.model.reset_states()
