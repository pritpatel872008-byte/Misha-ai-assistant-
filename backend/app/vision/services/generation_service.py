import asyncio
import os
import uuid
import time
import io
import logging
from typing import Dict, Optional, Any, Callable
from PIL import Image

from backend.app.vision.models import (
    GenerationJob,
    JobState,
    GenerateImageRequest,
    InpaintRequest,
    ImageToImageRequest,
    RemoveBackgroundRequest,
    UpscaleRequest,
    ProviderType
)
from backend.app.vision.providers.base import AIProvider
from backend.app.vision.providers.local_provider import LocalAIProvider
from backend.app.vision.providers.gemini_provider import GeminiAIProvider

logger = logging.getLogger("MishaGenerationService")

class GenerationService:
    """
    Async Generation Job Pipeline for Misha Vision Studio.
    Handles job queue, status reporting, cancellation, and background execution.
    """

    def __init__(self, storage_dir: str = "/tmp/misha_storage"):
        self.storage_dir = storage_dir
        os.makedirs(self.storage_dir, exist_ok=True)
        self.jobs: Dict[str, GenerationJob] = {}
        self.running_tasks: Dict[str, asyncio.Task] = {}
        
        self.local_provider = LocalAIProvider()
        self.gemini_provider = GeminiAIProvider()

    def get_provider(self, provider_type: ProviderType) -> AIProvider:
        if provider_type == ProviderType.GEMINI and self.gemini_provider.is_available:
            return self.gemini_provider
        return self.local_provider

    def get_job(self, job_id: str) -> Optional[GenerationJob]:
        return self.jobs.get(job_id)

    def cancel_job(self, job_id: str) -> bool:
        job = self.jobs.get(job_id)
        if not job or job.state in [JobState.COMPLETED, JobState.FAILED, JobState.CANCELLED]:
            return False

        task = self.running_tasks.get(job_id)
        if task and not task.done():
            task.cancel()

        job.state = JobState.CANCELLED
        job.message = "Job cancelled by user."
        job.completed_at = time.time()
        return True

    async def submit_text_to_image(self, request: GenerateImageRequest) -> GenerationJob:
        job_id = str(uuid.uuid4())
        job = GenerationJob(
            job_id=job_id,
            state=JobState.QUEUED,
            operation="TEXT_TO_IMAGE",
            provider=request.provider.value,
            message="Queued for generation"
        )
        self.jobs[job_id] = job

        task = asyncio.create_task(self._run_text_to_image(job_id, request))
        self.running_tasks[job_id] = task
        return job

    async def submit_inpaint(self, request: InpaintRequest) -> GenerationJob:
        job_id = str(uuid.uuid4())
        job = GenerationJob(
            job_id=job_id,
            state=JobState.QUEUED,
            operation="INPAINT",
            provider=request.provider.value,
            message="Queued for inpainting"
        )
        self.jobs[job_id] = job

        task = asyncio.create_task(self._run_inpaint(job_id, request))
        self.running_tasks[job_id] = task
        return job

    async def submit_remove_background(self, request: RemoveBackgroundRequest) -> GenerationJob:
        job_id = str(uuid.uuid4())
        job = GenerationJob(
            job_id=job_id,
            state=JobState.QUEUED,
            operation="REMOVE_BACKGROUND",
            provider="LOCAL",
            message="Queued for background segmentation"
        )
        self.jobs[job_id] = job

        task = asyncio.create_task(self._run_remove_background(job_id, request))
        self.running_tasks[job_id] = task
        return job

    async def submit_upscale(self, request: UpscaleRequest) -> GenerationJob:
        job_id = str(uuid.uuid4())
        job = GenerationJob(
            job_id=job_id,
            state=JobState.QUEUED,
            operation="UPSCALE",
            provider="LOCAL",
            message=f"Queued for {request.scale_factor}x upscale"
        )
        self.jobs[job_id] = job

        task = asyncio.create_task(self._run_upscale(job_id, request))
        self.running_tasks[job_id] = task
        return job

    # --- Internal Background Execution ---

    async def _run_text_to_image(self, job_id: str, request: GenerateImageRequest):
        job = self.jobs[job_id]
        start_time = time.time()
        try:
            job.state = JobState.LOADING
            job.message = "Loading AI Model..."
            job.progress = 0.1
            await asyncio.sleep(0.05)

            provider = self.get_provider(request.provider)
            job.state = JobState.RUNNING
            job.message = f"Synthesizing image with {provider.name}..."
            job.progress = 0.4

            image = await provider.generate_text_to_image(request)

            job.state = JobState.POST_PROCESSING
            job.message = "Saving output image..."
            job.progress = 0.9

            out_filename = f"gen_{job_id}.png"
            out_path = os.path.join(self.storage_dir, out_filename)
            image.save(out_path, format="PNG")

            job.output_image_path = out_path
            job.output_image_url = f"/api/images/{out_filename}"
            job.state = JobState.COMPLETED
            job.message = "Completed successfully"
            job.progress = 1.0
            job.completed_at = time.time()
            job.duration_sec = round(job.completed_at - start_time, 2)
        except asyncio.CancelledError:
            job.state = JobState.CANCELLED
            job.message = "Job cancelled."
        except Exception as e:
            logger.error(f"Text to image job failed: {e}", exc_info=True)
            job.state = JobState.FAILED
            job.error = str(e)
            job.message = f"Generation failed: {e}"
            job.completed_at = time.time()

    async def _run_inpaint(self, job_id: str, request: InpaintRequest):
        job = self.jobs[job_id]
        start_time = time.time()
        try:
            job.state = JobState.RUNNING
            job.message = "Processing mask and inpainting..."
            job.progress = 0.3

            provider = self.get_provider(request.provider)
            image = await provider.inpaint(request)

            job.state = JobState.POST_PROCESSING
            out_filename = f"inpaint_{job_id}.png"
            out_path = os.path.join(self.storage_dir, out_filename)
            image.save(out_path, format="PNG")

            job.output_image_path = out_path
            job.output_image_url = f"/api/images/{out_filename}"
            job.state = JobState.COMPLETED
            job.progress = 1.0
            job.completed_at = time.time()
            job.duration_sec = round(job.completed_at - start_time, 2)
        except Exception as e:
            job.state = JobState.FAILED
            job.error = str(e)
            job.message = f"Inpainting failed: {e}"

    async def _run_remove_background(self, job_id: str, request: RemoveBackgroundRequest):
        job = self.jobs[job_id]
        start_time = time.time()
        try:
            job.state = JobState.RUNNING
            job.message = "Segmenting foreground and removing background..."
            job.progress = 0.4

            image = await self.local_provider.remove_background(request)

            job.state = JobState.POST_PROCESSING
            out_filename = f"nobg_{job_id}.png"
            out_path = os.path.join(self.storage_dir, out_filename)
            image.save(out_path, format="PNG")

            job.output_image_path = out_path
            job.output_image_url = f"/api/images/{out_filename}"
            job.state = JobState.COMPLETED
            job.progress = 1.0
            job.completed_at = time.time()
            job.duration_sec = round(job.completed_at - start_time, 2)
        except Exception as e:
            job.state = JobState.FAILED
            job.error = str(e)
            job.message = f"Background removal failed: {e}"

    async def _run_upscale(self, job_id: str, request: UpscaleRequest):
        job = self.jobs[job_id]
        start_time = time.time()
        try:
            job.state = JobState.RUNNING
            job.message = f"Running {request.scale_factor}x Super Resolution Upscale..."
            job.progress = 0.4

            image = await self.local_provider.upscale(request)

            out_filename = f"upscale_{job_id}.png"
            out_path = os.path.join(self.storage_dir, out_filename)
            image.save(out_path, format="PNG")

            job.output_image_path = out_path
            job.output_image_url = f"/api/images/{out_filename}"
            job.state = JobState.COMPLETED
            job.progress = 1.0
            job.completed_at = time.time()
            job.duration_sec = round(job.completed_at - start_time, 2)
        except Exception as e:
            job.state = JobState.FAILED
            job.error = str(e)
            job.message = f"Upscale failed: {e}"
