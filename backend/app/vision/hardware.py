import os
import platform
import logging
from typing import Dict, Any

logger = logging.getLogger("MishaHardware")

# psutil is optional: the API must still boot on minimal cloud containers.
try:
    import psutil
except ImportError:
    psutil = None


def _memory_gb() -> tuple[float, float]:
    """Return (total, available) RAM in GB, with portable fallbacks."""
    if psutil is not None:
        try:
            mem = psutil.virtual_memory()
            return round(mem.total / (1024 ** 3), 2), round(mem.available / (1024 ** 3), 2)
        except Exception:
            logger.exception("psutil memory detection failed")

    # Linux containers commonly expose memory limits via cgroups.
    for filename in ("/sys/fs/cgroup/memory.max", "/sys/fs/cgroup/memory/memory.limit_in_bytes"):
        try:
            raw = open(filename, "r", encoding="utf-8").read().strip()
            if raw != "max":
                total = int(raw)
                if 0 < total < (1 << 60):
                    gb = round(total / (1024 ** 3), 2)
                    return gb, gb
        except (OSError, ValueError):
            continue
    return 0.0, 0.0


def detect_hardware() -> Dict[str, Any]:
    """Detect available host hardware without making optional packages fatal."""
    if psutil is not None:
        try:
            cpu_count_logical = psutil.cpu_count(logical=True) or os.cpu_count() or 2
            cpu_count_physical = psutil.cpu_count(logical=False) or 1
        except Exception:
            cpu_count_logical = os.cpu_count() or 2
            cpu_count_physical = 1
    else:
        cpu_count_logical = os.cpu_count() or 2
        cpu_count_physical = 1

    ram_gb, ram_available_gb = _memory_gb()
    device = "cpu"
    gpu_name = "None (CPU Execution)"
    vram_gb = 0.0
    cuda_available = False
    mps_available = False

    try:
        import torch
        if torch.cuda.is_available():
            cuda_available = True
            device = "cuda"
            gpu_name = torch.cuda.get_device_name(0)
            vram_gb = round(torch.cuda.get_device_properties(0).total_memory / (1024 ** 3), 2)
        elif hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
            mps_available = True
            device = "mps"
            gpu_name = "Apple Silicon (MPS)"
            vram_gb = round(ram_gb * 0.7, 2)
    except ImportError:
        pass
    except Exception:
        logger.exception("GPU detection failed; continuing with CPU defaults")

    if vram_gb >= 12.0 or (ram_gb >= 32.0 and cuda_available):
        preset, recommended_model = "QUALITY", "sdxl-turbo / flux-schnell"
    elif vram_gb >= 6.0 or (ram_gb >= 16.0 and (cuda_available or mps_available)):
        preset, recommended_model = "BALANCED", "stable-diffusion-v1-5 / sd-turbo"
    else:
        preset, recommended_model = "LIGHT", "sd-tiny / onnx-fast"

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
        "recommended_model": recommended_model,
    }
