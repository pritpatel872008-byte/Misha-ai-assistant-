import os
import io
import base64
import logging
from PIL import Image
from typing import Optional

from backend.app.vision.providers.base import AIProvider
from backend.app.vision.models import (
    GenerateImageRequest,
    InpaintRequest,
    ImageToImageRequest,
    RemoveBackgroundRequest,
    UpscaleRequest
)

logger = logging.getLogger("MishaGeminiProvider")

class GeminiAIProvider(AIProvider):
    """
    Optional Gemini Cloud AI Provider.
    Adheres strictly to the same AIProvider interface.
    Activated only when an API key is provided; otherwise LocalAIProvider remains active.
    """

    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY", "")
        self.client = None
        self._init_client()

    def _init_client(self):
        if not self.api_key:
            return
        try:
            from google import genai
            self.client = genai.Client(api_key=self.api_key)
            logger.info("Gemini Vision AI Provider client initialized.")
        except Exception as e:
            logger.warning(f"Could not initialize Google GenAI client: {e}")

    @property
    def name(self) -> str:
        return "GEMINI"

    @property
    def is_available(self) -> bool:
        return bool(self.api_key and len(self.api_key) > 5)

    async def generate_text_to_image(self, request: GenerateImageRequest) -> Image.Image:
        logger.info(f"Gemini AI generating image: '{request.prompt}'")
        if not self.is_available or not self.client:
            raise ValueError("Gemini API key is not configured.")

        try:
            # Use imagen-3.0 or gemini image generation if available
            response = self.client.models.generate_images(
                model='imagen-3.0-generate-002',
                prompt=request.prompt,
                config=dict(
                    number_of_images=1,
                    aspect_ratio=request.aspect_ratio or "1:1",
                    person_generation="ALLOW_ADULT",
                )
            )
            for generated_image in response.generated_images:
                return Image.open(io.BytesIO(generated_image.image.image_bytes)).convert("RGBA")
        except Exception as e:
            logger.error(f"Gemini generate image error: {e}")
            raise

        raise RuntimeError("No image returned from Gemini.")

    async def image_to_image(self, request: ImageToImageRequest) -> Image.Image:
        # Fallback to local or throw if unavailable
        raise NotImplementedError("Image-to-image with Gemini cloud adapter.")

    async def inpaint(self, request: InpaintRequest) -> Image.Image:
        raise NotImplementedError("Inpainting with Gemini cloud adapter.")

    async def remove_background(self, request: RemoveBackgroundRequest) -> Image.Image:
        raise NotImplementedError("Background removal with Gemini cloud adapter.")

    async def upscale(self, request: UpscaleRequest) -> Image.Image:
        raise NotImplementedError("Upscaling with Gemini cloud adapter.")

    async def enhance_prompt(self, prompt: str) -> str:
        if not self.is_available or not self.client:
            return f"{prompt}, highly detailed, cinematic lighting, 8k resolution"

        try:
            response = self.client.models.generate_content(
                model="gemini-2.5-flash",
                contents=f"Enrich this image generation prompt with vivid visual details, lighting, and textures in under 40 words. Output only the prompt: '{prompt}'"
            )
            return response.text.strip()
        except Exception as e:
            logger.warning(f"Gemini prompt enhancement fallback: {e}")
            return f"{prompt}, highly detailed, dramatic lighting, 8k"
