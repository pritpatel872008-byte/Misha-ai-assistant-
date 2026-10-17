import os
import json
import time
import uuid
import shutil
from typing import List, Optional, Dict, Any
from PIL import Image

from app.vision.models import Project, Layer, ProjectVersion

class ProjectService:
    """
    Project Persistence & Version History Manager for Misha Vision Studio.
    Handles projects, layers, undo/redo version snapshots, autosave, and safe storage.
    """

    def __init__(self, projects_dir: str = "/tmp/misha_projects", storage_dir: str = "/tmp/misha_storage"):
        self.projects_dir = projects_dir
        self.storage_dir = storage_dir
        os.makedirs(self.projects_dir, exist_ok=True)
        os.makedirs(self.storage_dir, exist_ok=True)
        self._memory_projects: Dict[str, Project] = {}
        self._load_all()

    def _project_file(self, project_id: str) -> str:
        return os.path.join(self.projects_dir, f"{project_id}.json")

    def _load_all(self):
        for fname in os.listdir(self.projects_dir):
            if fname.endswith(".json"):
                fpath = os.path.join(self.projects_dir, fname)
                try:
                    with open(fpath, "r", encoding="utf-8") as f:
                        data = json.load(f)
                        proj = Project(**data)
                        self._memory_projects[proj.id] = proj
                except Exception:
                    pass

    def list_projects(self) -> List[Project]:
        return sorted(list(self._memory_projects.values()), key=lambda p: p.updated_at, reverse=True)

    def get_project(self, project_id: str) -> Optional[Project]:
        return self._memory_projects.get(project_id)

    def create_project(self, name: str = "Untitled Project", width: int = 1024, height: int = 1024) -> Project:
        project_id = str(uuid.uuid4())
        initial_layer = Layer(
            id=str(uuid.uuid4()),
            name="Background Layer",
            visible=True,
            opacity=1.0
        )
        proj = Project(
            id=project_id,
            name=name,
            width=width,
            height=height,
            layers=[initial_layer],
            active_layer_id=initial_layer.id,
            versions=[]
        )
        self._memory_projects[project_id] = proj
        self.save_project(proj)
        return proj

    def save_project(self, project: Project) -> Project:
        project.updated_at = time.time()
        self._memory_projects[project.id] = project
        with open(self._project_file(project.id), "w", encoding="utf-8") as f:
            f.write(project.model_dump_json(indent=2))
        return project

    def delete_project(self, project_id: str) -> bool:
        if project_id in self._memory_projects:
            del self._memory_projects[project_id]
        pfile = self._project_file(project_id)
        if os.path.exists(pfile):
            os.remove(pfile)
            return True
        return False

    def add_version(self, project_id: str, name: str, operation: str, image_path: str, prompt: Optional[str] = None) -> Optional[ProjectVersion]:
        project = self.get_project(project_id)
        if not project:
            return None

        ver_id = len(project.versions) + 1
        version = ProjectVersion(
            version_id=ver_id,
            name=name,
            operation=operation,
            prompt=prompt,
            image_path=image_path,
            timestamp=time.time()
        )
        project.versions.append(version)
        project.current_version_id = ver_id
        self.save_project(project)
        return version

    def restore_version(self, project_id: str, version_id: int) -> Optional[ProjectVersion]:
        project = self.get_project(project_id)
        if not project:
            return None

        target = next((v for v in project.versions if v.version_id == version_id), None)
        if target:
            project.current_version_id = version_id
            self.save_project(project)
        return target
