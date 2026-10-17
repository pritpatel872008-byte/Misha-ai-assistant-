import os
import platform
import psutil
import logging
from typing import Dict, Any

logger = logging.getLogger("MishaHardware")

def detect_hardware() -> Dict[str, Any]:
    """
    Detects real host hardware capabilities:
    - CPU cores, threads & model
    - System RAM
    - GPU device detection (CUDA, Apple Silicon MPS, or CPU)
    - Available VRAM
    - OS and Python version
    - Recommended Preset (LIGHT, BALANCED, QUALITY)
    """
    cpu_count_logical = psutil.cpu_count(logical=True) or 2
    cpu_count_physical = psutil.cpu_count(logical=False) or 1
    ram_gb = round(psutil.virtual_memory().total / (1024 ** 3), 2)
    ram_available_gb = round(psutil.virtual_memory().available / (1024 ** 3), 2)

    device = "cpu"
    gpu_name = "None (CPU Execution)"
    vram_gb = 0.0
    cuda_available = False
    mps_available = False

    # Check PyTorch CUDA / MPS
    try:
        import torch
        if torch.cuda.is_available():
            cuda_available = True
            device = "cuda"
            gpu_name = torch.cuda.get_device_name(0)
            vram_bytes = torch.cuda.get_device_properties(0).total_memory
            vram_gb = round(vram_bytes / (1024 ** 3), 2)
        elif hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
            mps_available = True
            device = "mps"
            gpu_name = "Apple Silicon (MPS)"
            vram_gb = round(ram_gb * 0.7, 2) # Unified memory approximation
    except ImportError:
        pass

    # Preset recommendation logic
    if vram_gb >= 12.0 or (ram_gb >= 32.0 and cuda_available):
        preset = "QUALITY"
        recommended_model = "sdxl-turbo / flux-schnell"
    elif vram_gb >= 6.0 or (ram_gb >= 16.0 and (cuda_available or mps_available)):
        preset = "BALANCED"
        recommended_model = "stable-diffusion-v1-5 / sd-turbo"
    else:
        preset = "LIGHT"
        recommended_model = "sd-tiny / onnx-fast"

    return {
        "os": platform.system(),
        "os_version": platform.release(),
        "python_version": platform.python_version(),
        "cpu_cores_logical": cpu_count_logical,
        "cpu_cores_physical": cpu_count_physical,
        "ram_total_gb": ram_gb,
        "ram_available_gb": ram_available_gb,
        "device": device,
        "gpu_name": gpu_name,
        "cuda_available": cuda_available,
        "mps_available": mps_available,
        "vram_gb": vram_gb,
        "recommended_preset": preset,
        "recommended_model": recommended_model
    }
