import os
import logging
from typing import Dict, List, Any
from backend.app.voice.models import ModelInfo, ModelStatus

logger = logging.getLogger("MishaModelManager")

class ModelManager:
    """
    Central registry for all voice AI models:
    - openWakeWord ('Misha')
    - Silero VAD
    - Vosk STT (Offline)
    - Piper TTS (Offline)
    """
    def __init__(self):
        self._models: Dict[str, ModelInfo] = {}

    def register_model(self, model_info: ModelInfo):
        self._models[model_info.name] = model_info
        logger.info(f"Registered model: {model_info.name} [{model_info.status.value}] at {model_info.path}")

    def get_model(self, name: str) -> ModelInfo:
        return self._models.get(name)

    def get_all_models(self) -> List[ModelInfo]:
        return list(self._models.values())

    def get_health_status(self) -> Dict[str, Any]:
        return {
            "models": [m.dict() for m in self._models.values()],
            "all_loaded": all(m.status == ModelStatus.LOADED for m in self._models.values()),
            "missing_models": [m.name for m in self._models.values() if m.status == ModelStatus.MISSING],
            "failed_models": [m.name for m in self._models.values() if m.status == ModelStatus.FAILED]
        }
