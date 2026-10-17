from abc import ABC, abstractmethod
from typing import Optional, Dict, Any, List
from PIL import Image
from backend.app.vision.models import (
    GenerateImageRequest,
    InpaintRequest,
    ImageToImageRequest,
    RemoveBackgroundRequest,
    UpscaleRequest
)

class AIProvider(ABC):
    """
    Abstract AI Provider Interface for Misha Vision Studio.
    Supports Local AI (Diffusers, ONNX, PyTorch) as primary default,
    with pluggable cloud providers (Gemini, Custom).
    """

    @property
    @abstractmethod
    def name(self) -> str:
        pass

    @property
    @abstractmethod
    def is_available(self) -> bool:
        pass

    @abstractmethod
    async def generate_text_to_image(self, request: GenerateImageRequest) -> Image.Image:
        """Generates an image from text prompt."""
        pass

    @abstractmethod
    async def image_to_image(self, request: ImageToImageRequest) -> Image.Image:
        """Transforms an existing image based on prompt and strength."""
        pass

    @abstractmethod
    async def inpaint(self, request: InpaintRequest) -> Image.Image:
        """Performs localized inpainting on masked region."""
        pass

    @abstractmethod
    async def remove_background(self, request: RemoveBackgroundRequest) -> Image.Image:
        """Removes background and produces transparent PNG image."""
        pass

    @abstractmethod
    async def upscale(self, request: UpscaleRequest) -> Image.Image:
        """Performs super-resolution upscaling (2x/4x)."""
        pass

    @abstractmethod
    async def enhance_prompt(self, prompt: str) -> str:
        """Refines and enriches user prompt with stylistic keywords without generating image."""
        pass
