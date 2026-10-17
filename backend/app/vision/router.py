import os
import uuid
import shutil
import logging
from typing import List, Optional
from fastapi import APIRouter, HTTPException, UploadFile, File, Form, Depends
from fastapi.responses import FileResponse
from pydantic import BaseModel

from app.vision.models import (
    Project,
    GenerationJob,
    GenerateImageRequest,
    InpaintRequest,
    ImageToImageRequest,
    RemoveBackgroundRequest,
    UpscaleRequest,
    ImageAdjustments,
    ProjectVersion
)
from app.vision.models_manager import ModelManager, ModelInfo
from app.vision.hardware import detect_hardware
from app.vision.services.generation_service import GenerationService
from app.vision.services.project_service import ProjectService
from app.vision.services.editing_service import EditingService

logger = logging.getLogger("MishaVisionRouter")

router = APIRouter(prefix="/api/vision", tags=["Misha Vision Studio"])

# Singletons
STORAGE_DIR = "/tmp/misha_storage"
PROJECTS_DIR = "/tmp/misha_projects"
MODELS_DIR = "/tmp/misha_models"

model_manager = ModelManager(models_dir=MODELS_DIR)
gen_service = GenerationService(storage_dir=STORAGE_DIR)
project_service = ProjectService(projects_dir=PROJECTS_DIR, storage_dir=STORAGE_DIR)

class PromptEnhanceRequest(BaseModel):
    prompt: str

class PromptEnhanceResponse(BaseModel):
    original_prompt: str
    enhanced_prompt: str

# 1. Hardware & Model Management
@router.get("/hardware")
def get_hardware_info():
    return detect_hardware()

@router.get("/models", response_model=List[ModelInfo])
def get_models():
    return model_manager.get_models()

@router.post("/models/{model_id}/install")
def install_model(model_id: str):
    return model_manager.install_model(model_id)

# 2. Project System
@router.get("/projects", response_model=List[Project])
def list_projects():
    return project_service.list_projects()

@router.post("/projects", response_model=Project)
def create_project(name: str = "Untitled Project", width: int = 1024, height: int = 1024):
    return project_service.create_project(name=name, width=width, height=height)

@router.get("/projects/{project_id}", response_model=Project)
def get_project(project_id: str):
    proj = project_service.get_project(project_id)
    if not proj:
        raise HTTPException(status_code=404, detail="Project not found")
    return proj

@router.put("/projects/{project_id}", response_model=Project)
def update_project(project_id: str, project: Project):
    if project.id != project_id:
        raise HTTPException(status_code=400, detail="Mismatched project ID")
    return project_service.save_project(project)

@router.delete("/projects/{project_id}")
def delete_project(project_id: str):
    deleted = project_service.delete_project(project_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Project not found")
    return {"status": "DELETED", "project_id": project_id}

@router.post("/projects/{project_id}/versions", response_model=ProjectVersion)
def add_project_version(project_id: str, name: str, operation: str, image_path: str, prompt: Optional[str] = None):
    version = project_service.add_version(project_id, name, operation, image_path, prompt)
    if not version:
        raise HTTPException(status_code=404, detail="Project not found")
    return version

@router.post("/projects/{project_id}/versions/{version_id}/restore")
def restore_project_version(project_id: str, version_id: int):
    version = project_service.restore_version(project_id, version_id)
    if not version:
        raise HTTPException(status_code=404, detail="Version not found")
    return {"status": "RESTORED", "version": version}

# 3. Image Upload & Validation
@router.post("/images/upload")
async def upload_image(file: UploadFile = File(...)):
    # File validation
    allowed_types = ["image/png", "image/jpeg", "image/jpg", "image/webp"]
    if file.content_type not in allowed_types:
        raise HTTPException(status_code=400, detail="Unsupported format. Use PNG, JPG, or WEBP.")

    file_ext = os.path.splitext(file.filename or "upload.png")[1] or ".png"
    safe_filename = f"upload_{uuid.uuid4()}{file_ext}"
    dest_path = os.path.join(STORAGE_DIR, safe_filename)

    with open(dest_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    return {
        "filename": safe_filename,
        "path": dest_path,
        "url": f"/api/images/{safe_filename}",
        "size_bytes": os.path.getsize(dest_path)
    }

# 4. AI Generation & Editing Endpoints
@router.post("/generate", response_model=GenerationJob)
async def generate_image(request: GenerateImageRequest):
    return await gen_service.submit_text_to_image(request)

@router.post("/inpaint", response_model=GenerationJob)
async def inpaint_image(request: InpaintRequest):
    return await gen_service.submit_inpaint(request)

@router.post("/remove-background", response_model=GenerationJob)
async def remove_background(request: RemoveBackgroundRequest):
    return await gen_service.submit_remove_background(request)

@router.post("/upscale", response_model=GenerationJob)
async def upscale_image(request: UpscaleRequest):
    return await gen_service.submit_upscale(request)

@router.post("/adjust")
async def adjust_image(request: ImageAdjustments):
    adjusted = EditingService.apply_adjustments(request)
    filename = f"adj_{uuid.uuid4()}.png"
    path = os.path.join(STORAGE_DIR, filename)
    adjusted.save(path, format="PNG")
    return {
        "filename": filename,
        "path": path,
        "url": f"/api/images/{filename}"
    }

@router.post("/prompt/enhance", response_model=PromptEnhanceResponse)
async def enhance_prompt(request: PromptEnhanceRequest):
    enhanced = await gen_service.local_provider.enhance_prompt(request.prompt)
    return PromptEnhanceResponse(
        original_prompt=request.prompt,
        enhanced_prompt=enhanced
    )

# 5. Job Status & Cancellation
@router.get("/jobs/{job_id}", response_model=GenerationJob)
def get_job(job_id: str):
    job = gen_service.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    return job

@router.post("/jobs/{job_id}/cancel")
def cancel_job(job_id: str):
    success = gen_service.cancel_job(job_id)
    if not success:
        raise HTTPException(status_code=400, detail="Cannot cancel job")
    return {"status": "CANCELLED", "job_id": job_id}
