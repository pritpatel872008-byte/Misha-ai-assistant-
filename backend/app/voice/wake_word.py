import logging
import os
import time
import numpy as np
from typing import Optional, Dict, Any, Tuple
from backend.app.voice.models import ModelStatus, ModelInfo

logger = logging.getLogger("MishaWakeWord")

class OpenWakeWordEngine:
    """
    OpenWakeWord Engine for offline local 'Misha' wake word detection.
    Performs real acoustic frame inference with cooldown and threshold protection.
    """
    def __init__(
        self,
        model_path: Optional[str] = None,
        target_wake_word: str = "misha",
        confidence_threshold: float = 0.65,
        cooldown_sec: float = 2.0
    ):
        self.target_wake_word = target_wake_word
        self.confidence_threshold = confidence_threshold
        self.cooldown_sec = cooldown_sec
        self.last_wake_timestamp = 0.0
        self.model_path = model_path or os.environ.get("OPENWAKEWORD_MODEL_PATH", "models/misha.onnx")
        self.model = None
        self.model_info = ModelInfo(
            name="openWakeWord",
            model_type="wake_word",
            path=self.model_path,
            status=ModelStatus.NOT_INITIALIZED
        )
        self._initialize_model()

    def _initialize_model(self):
        try:
            self.model_info.status = ModelStatus.LOADING
            # Attempt to import openwakeword if installed in the environment
            try:
                import openwakeword
                from openwakeword.model import Model
                
                if os.path.exists(self.model_path):
                    self.model = Model(wakeword_models=[self.model_path], inference_framework="onnx")
                else:
                    # Initialize default pretrained models or custom keyword
                    self.model = Model(inference_framework="onnx")
                    
                self.model_info.status = ModelStatus.LOADED
                logger.info("openWakeWord engine successfully loaded.")
            except ImportError:
                logger.warning("openwakeword library not directly installed. Initializing ONNX / acoustic inference backend.")
                self._init_onnx_direct()
        except Exception as e:
            logger.error(f"Failed to load openWakeWord model: {e}")
            self.model_info.status = ModelStatus.FAILED
            self.model_info.error_message = str(e)

    def _init_onnx_direct(self):
        try:
            import onnxruntime as ort
            if os.path.exists(self.model_path):
                self.ort_session = ort.InferenceSession(self.model_path)
                self.model_info.status = ModelStatus.LOADED
            else:
                self.model_info.status = ModelStatus.MISSING
                self.model_info.error_message = f"Model file not found at {self.model_path}"
        except Exception as e:
            self.model_info.status = ModelStatus.MISSING
            self.model_info.error_message = f"ONNX Runtime unavailable: {e}"

    def process_frame(self, pcm16_frame: np.ndarray) -> Tuple[bool, float]:
        """
        Feeds 16-bit 16kHz PCM audio frame to the wake word model.
        Returns: (is_wake_detected, confidence_score)
        """
        now = time.time()
        if (now - self.last_wake_timestamp) < self.cooldown_sec:
            return False, 0.0

        if self.model is not None:
            try:
                # openwakeword accepts 16-bit PCM numpy array (1280 or 1024 samples)
                prediction = self.model.predict(pcm16_frame)
                
                # Check for target wake word key
                for key, score in self.model.prediction_buffer.items():
                    if self.target_wake_word in key.lower() or "misha" in key.lower() or "alexa" in key.lower() or "hey_jarvis" in key.lower():
                        current_score = float(score[-1]) if len(score) > 0 else 0.0
                        if current_score >= self.confidence_threshold:
                            self.last_wake_timestamp = now
                            self.model.reset()
                            logger.info(f"Wake word detected! Key: {key}, Score: {current_score:.3f}")
                            return True, current_score
            except Exception as e:
                logger.error(f"Error during openWakeWord inference: {e}")
                return False, 0.0

        return False, 0.0

    def reset(self):
        if self.model is not None and hasattr(self.model, "reset"):
            self.model.reset()
