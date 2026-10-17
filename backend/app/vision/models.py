import time
from enum import Enum
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field

class JobState(str, Enum):
    QUEUED = "QUEUED"
    LOADING = "LOADING"
    RUNNING = "RUNNING"
    POST_PROCESSING = "POST_PROCESSING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"

class ProviderType(str, Enum):
    LOCAL = "LOCAL"
    GEMINI = "GEMINI"
    CUSTOM = "CUSTOM"

class LayerType(str, Enum):
    IMAGE = "IMAGE"
    TEXT = "TEXT"
    SHAPE = "SHAPE"
    AI_GENERATED = "AI_GENERATED"
    MASK = "MASK"

class BlendMode(str, Enum):
    NORMAL = "NORMAL"
    MULTIPLY = "MULTIPLY"
    SCREEN = "SCREEN"
    OVERLAY = "OVERLAY"
    DARKEN = "DARKEN"
    LIGHTEN = "LIGHTEN"

class Layer(BaseModel):
    id: str
    name: str
    type: LayerType = LayerType.IMAGE
    visible: bool = True
    opacity: float = 1.0
    blend_mode: BlendMode = BlendMode.NORMAL
    position_x: float = 0.0
    position_y: float = 0.0
    scale_x: float = 1.0
    scale_y: float = 1.0
    rotation_deg: float = 0.0
    image_url_or_path: Optional[str] = None
    text_content: Optional[str] = None
    text_color: Optional[str] = "#FFFFFF"
    font_size: Optional[float] = 24.0

class ProjectVersion(BaseModel):
    version_id: int
    name: str
    operation: str
    prompt: Optional[str] = None
    provider: str = "LOCAL"
    model_name: Optional[str] = None
    image_path: str
    timestamp: float = Field(default_factory=time.time)

class Project(BaseModel):
    id: str
    name: str
    width: int = 1024
    height: int = 1024
    layers: List[Layer] = Field(default_factory=list)
    active_layer_id: Optional[str] = None
    versions: List[ProjectVersion] = Field(default_factory=list)
    current_version_id: int = 1
    created_at: float = Field(default_factory=time.time)
    updated_at: float = Field(default_factory=time.time)

class GenerateImageRequest(BaseModel):
    prompt: str
    negative_prompt: Optional[str] = "ugly, deformed, blurry, low quality, artifacts"
    width: int = 512
    height: int = 512
    aspect_ratio: str = "1:1"
    steps: int = 20
    guidance_scale: float = 7.5
    seed: Optional[int] = None
    number_of_images: int = 1
    model_name: Optional[str] = "sd-turbo"
    provider: ProviderType = ProviderType.LOCAL

class InpaintRequest(BaseModel):
    image_base64_or_path: str
    mask_base64_or_path: str
    prompt: str
    negative_prompt: Optional[str] = ""
    strength: float = 0.75
    steps: int = 20
    guidance_scale: float = 7.5
    seed: Optional[int] = None
    model_name: Optional[str] = "stable-diffusion-inpainting"
    provider: ProviderType = ProviderType.LOCAL

class ImageToImageRequest(BaseModel):
    image_base64_or_path: str
    prompt: str
    negative_prompt: Optional[str] = ""
    strength: float = 0.6
    steps: int = 20
    guidance_scale: float = 7.5
    seed: Optional[int] = None
    model_name: Optional[str] = "sd-turbo"
    provider: ProviderType = ProviderType.LOCAL

class RemoveBackgroundRequest(BaseModel):
    image_base64_or_path: str
    refine_edges: bool = True
    model_name: Optional[str] = "rembg-u2net"

class ReplaceBackgroundRequest(BaseModel):
    image_base64_or_path: str
    background_prompt_or_image: str
    is_prompt: bool = True
    model_name: Optional[str] = "rembg-u2net"

class UpscaleRequest(BaseModel):
    image_base64_or_path: str
    scale_factor: int = 2 # 2x or 4x
    model_name: Optional[str] = "real-esrgan-4x"

class ImageAdjustments(BaseModel):
    image_base64_or_path: str
    brightness: float = 1.0 # 0.0 to 2.0
    contrast: float = 1.0 # 0.0 to 2.0
    saturation: float = 1.0 # 0.0 to 2.0
    sharpness: float = 1.0 # 0.0 to 2.0
    noise_reduction: float = 0.0 # 0.0 to 1.0
    rotation_deg: float = 0.0
    flip_horizontal: bool = False
    flip_vertical: bool = False

class GenerationJob(BaseModel):
    job_id: str
    state: JobState = JobState.QUEUED
    operation: str
    provider: str
    progress: float = 0.0
    message: str = "Queued"
    output_image_path: Optional[str] = None
    output_image_url: Optional[str] = None
    error: Optional[str] = None
    created_at: float = Field(default_factory=time.time)
    completed_at: Optional[float] = None
    duration_sec: Optional[float] = None
