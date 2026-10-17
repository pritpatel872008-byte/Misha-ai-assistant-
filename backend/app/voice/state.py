import asyncio
import logging
import time
from typing import Callable, List, Optional
from backend.app.voice.models import VoiceState

logger = logging.getLogger("MishaVoiceState")

class VoiceStateMachine:
    def __init__(self, on_state_changed: Optional[Callable[[VoiceState, VoiceState], None]] = None):
        self._state: VoiceState = VoiceState.STANDBY
        self._last_state_change: float = time.time()
        self._state_callbacks: List[Callable[[VoiceState], None]] = []
        if on_state_changed:
            self._state_callbacks.append(on_state_changed)
        self._tts_suppression_active: bool = False
        self._cooldown_duration_sec: float = 1.0

    @property
    def current_state(self) -> VoiceState:
        return self._state

    @property
    def is_tts_suppressed(self) -> bool:
        return self._tts_suppression_active

    def register_callback(self, callback: Callable[[VoiceState], None]):
        if callback not in self._state_callbacks:
            self._state_callbacks.append(callback)

    def set_state(self, new_state: VoiceState, reason: str = ""):
        if self._state == new_state:
            return
        
        old_state = self._state
        self._state = new_state
        self._last_state_change = time.time()
        logger.info(f"State transition: {old_state.value} -> {new_state.value} (Reason: {reason})")

        for cb in self._state_callbacks:
            try:
                cb(new_state)
            except Exception as e:
                logger.error(f"Error in state callback: {e}")

    def on_tts_started(self):
        """Suppresses wake word and VAD triggers while TTS audio is playing."""
        self._tts_suppression_active = True
        self.set_state(VoiceState.SPEAKING, "TTS Playback Started")

    def on_tts_finished(self):
        """Enters post-TTS cooldown to absorb acoustic echoes before reactivating wake word."""
        self.set_state(VoiceState.COOLDOWN, "TTS Playback Complete - Cooldown")
        asyncio.create_task(self._cooldown_coroutine())

    async def _cooldown_coroutine(self):
        await asyncio.sleep(self._cooldown_duration_sec)
        self._tts_suppression_active = False
        if self._state == VoiceState.COOLDOWN:
            self.set_state(VoiceState.LISTENING_FOR_WAKE, "Cooldown elapsed, resuming wake listening")
