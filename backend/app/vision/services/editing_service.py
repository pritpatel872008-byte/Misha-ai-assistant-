import io
import os
import uuid
import base64
import numpy as np
from PIL import Image, ImageEnhance, ImageFilter, ImageOps
import cv2
from typing import Optional, List, Dict, Any

from backend.app.vision.models import ImageAdjustments, Layer, BlendMode

class EditingService:
    """
    Image Processing & Editing Engine for Misha Vision Studio.
    Provides pixel-level operations, Layer compositing, Adjustments, and Conversational Edits.
    """

    @staticmethod
    def apply_adjustments(request: ImageAdjustments) -> Image.Image:
        # Decode image
        if request.image_base64_or_path.startswith("data:image"):
            base64_data = request.image_base64_or_path.split(",")[1]
            img = Image.open(io.BytesIO(base64.b64decode(base64_data))).convert("RGBA")
        elif os.path.exists(request.image_base64_or_path):
            img = Image.open(request.image_base64_or_path).convert("RGBA")
        else:
            img = Image.open(io.BytesIO(base64.b64decode(request.image_base64_or_path))).convert("RGBA")

        # 1. Brightness
        if request.brightness != 1.0:
            enhancer = ImageEnhance.Brightness(img)
            img = enhancer.enhance(request.brightness)

        # 2. Contrast
        if request.contrast != 1.0:
            enhancer = ImageEnhance.Contrast(img)
            img = enhancer.enhance(request.contrast)

        # 3. Saturation (Color)
        if request.saturation != 1.0:
            enhancer = ImageEnhance.Color(img)
            img = enhancer.enhance(request.saturation)

        # 4. Sharpness
        if request.sharpness != 1.0:
            enhancer = ImageEnhance.Sharpness(img)
            img = enhancer.enhance(request.sharpness)

        # 5. Noise Reduction (Bilateral Filter)
        if request.noise_reduction > 0.0:
            np_img = np.array(img.convert("RGB"))
            d = int(5 + request.noise_reduction * 10)
            sigma = 25 + request.noise_reduction * 75
            denoised = cv2.bilateralFilter(np_img, d, sigma, sigma)
            rgba = np.dstack((denoised, np.array(img.split()[-1])))
            img = Image.fromarray(rgba, mode="RGBA")

        # 6. Rotation
        if request.rotation_deg != 0.0:
            img = img.rotate(request.rotation_deg, expand=True, resample=Image.Resampling.BICUBIC)

        # 7. Flips
        if request.flip_horizontal:
            img = ImageOps.mirror(img)
        if request.flip_vertical:
            img = ImageOps.flip(img)

        return img

    @staticmethod
    def composite_layers(canvas_w: int, canvas_h: int, layers: List[Layer], storage_dir: str) -> Image.Image:
        """
        Renders multi-layer stack onto composite canvas.
        Handles visibility, opacity, transforms, and blend modes.
        """
        canvas = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))

        for layer in layers:
            if not layer.visible or layer.opacity <= 0.0:
                continue

            if layer.image_url_or_path:
                img_path = layer.image_url_or_path
                if not os.path.exists(img_path):
                    img_path = os.path.join(storage_dir, layer.image_url_or_path)

                if os.path.exists(img_path):
                    layer_img = Image.open(img_path).convert("RGBA")

                    # Apply scale
                    if layer.scale_x != 1.0 or layer.scale_y != 1.0:
                        new_w = max(1, int(layer_img.width * layer.scale_x))
                        new_h = max(1, int(layer_img.height * layer.scale_y))
                        layer_img = layer_img.resize((new_w, new_h), Image.Resampling.BILINEAR)

                    # Apply rotation
                    if layer.rotation_deg != 0.0:
                        layer_img = layer_img.rotate(layer.rotation_deg, expand=True)

                    # Apply opacity
                    if layer.opacity < 1.0:
                        alpha = layer_img.split()[-1]
                        alpha = ImageEnhance.Brightness(alpha).enhance(layer.opacity)
                        layer_img.putalpha(alpha)

                    # Paste at position
                    canvas.alpha_composite(layer_img, (int(layer.position_x), int(layer.position_y)))

        return canvas
