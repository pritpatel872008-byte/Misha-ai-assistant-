import numpy as np
import logging
from typing import Tuple

logger = logging.getLogger("MishaAudioInput")

class AudioFrameProcessor:
    """
    Standard Audio Format Contract:
    - 16000 Hz sample rate
    - 1 channel (Mono)
    - 16-bit signed integer PCM (little endian)
    - 1024 samples (2048 bytes) per frame
    """
    def __init__(self, sample_rate: int = 16000, frame_size: int = 1024):
        self.sample_rate = sample_rate
        self.frame_size = frame_size

    def bytes_to_pcm16(self, raw_bytes: bytes) -> np.ndarray:
        """Converts raw Little-Endian byte buffer into int16 numpy array."""
        return np.frombuffer(raw_bytes, dtype=np.int16)

    def pcm16_to_float32(self, pcm16_data: np.ndarray) -> np.ndarray:
        """Converts int16 PCM array to normalized [-1.0, 1.0] float32 array for ML inference."""
        return pcm16_data.astype(np.float32) / 32768.0

    def compute_rms(self, pcm16_data: np.ndarray) -> float:
        """Computes Root Mean Square (RMS) energy level of an audio frame."""
        if len(pcm16_data) == 0:
            return 0.0
        float_data = pcm16_data.astype(np.float64)
        mean_square = np.mean(float_data ** 2)
        return float(np.sqrt(mean_square))

    def compute_db(self, rms: float) -> float:
        """Computes Decibels relative to full scale (dBFS)."""
        if rms <= 0.0:
            return -100.0
        return float(20.0 * np.log10(max(rms / 32767.0, 1e-5)))
