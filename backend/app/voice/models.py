"""
Audio Format Contract & Specification:
- Sample Rate: 16000 Hz
- Channels: 1 (Mono)
- Bit Depth: 16-bit signed PCM (Little-Endian)
- Frame Size: 1024 samples (2048 bytes) per frame
- Target Wake Word: 'Misha' (openWakeWord model)
"""

from enum import Enum
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field
import time

class VoiceState(str, Enum):
    STANDBY = "STANDBY"
    LISTENING_FOR_WAKE = "LISTENING_FOR_WAKE"
    WAKE_DETECTED = "WAKE_DETECTED"
    LISTENING_FOR_COMMAND = "LISTENING_FOR_COMMAND"
    TRANSCRIBING = "TRANSCRIBING"
    THINKING = "THINKING"
    EXECUTING = "EXECUTING"
    SPEAKING = "SPEAKING"
    COOLDOWN = "COOLDOWN"
    ERROR = "ERROR"

class ModelStatus(str, Enum):
    NOT_INITIALIZED = "NOT_INITIALIZED"
    LOADING = "LOADING"
    LOADED = "LOADED"
    FAILED = "FAILED"
    MISSING = "MISSING"

class ModelInfo(BaseModel):
    name: str
    model_type: str # wake_word, vad, stt, tts
    path: str
    version: str = "1.0.0"
    status: ModelStatus = ModelStatus.NOT_INITIALIZED
    error_message: Optional[str] = None
    memory_mb: float = 0.0

class ParsedActionIntent(BaseModel):
    request_id: str = Field(default_factory=lambda: f"req_{int(time.time()*1000)}")
    action: str # call_contact, send_sms, send_whatsapp, device_control, get_weather, emergency_sos, search_web, general_query
    parameters: Dict[str, Any] = Field(default_factory=dict)
    confidence: float = 1.0
    spoken_confirmation: Optional[str] = None
    requires_sensitive_auth: bool = False
    idempotency_key: Optional[str] = None

class ActionVerificationStatus(str, Enum):
    VERIFIED_SUCCESS = "VERIFIED_SUCCESS"
    SUBMITTED = "SUBMITTED"
    PARTIAL_SUCCESS = "PARTIAL_SUCCESS"
    FAILED = "FAILED"
    UNKNOWN = "UNKNOWN"

class ActionExecutionResult(BaseModel):
    request_id: str
    action: str
    status: ActionVerificationStatus
    message: str
    spoken_feedback: Optional[str] = None
    data: Optional[Dict[str, Any]] = None
