import os
import json
import logging
from typing import Optional, Dict, Any
from backend.app.voice.models import ParsedActionIntent

logger = logging.getLogger("MishaGeminiAI")

class GeminiAIEngine:
    """
    Python Gemini AI Engine:
    Processes natural language commands, structures device actions, and produces conversational reasoning.
    """
    def __init__(self, api_key: Optional[str] = None, model_name: str = "gemini-2.5-flash"):
        self.api_key = api_key or os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY", "")
        self.model_name = model_name
        self.client = None
        self._init_client()

    def _init_client(self):
        if not self.api_key:
            logger.warning("Gemini API key is not configured. Local intent heuristics active.")
            return

        try:
            from google import genai
            self.client = genai.Client(api_key=self.api_key)
            logger.info("Google GenAI client initialized successfully.")
        except ImportError:
            try:
                import google.generativeai as legacy_genai
                legacy_genai.configure(api_key=self.api_key)
                self.client = legacy_genai.GenerativeModel(self.model_name)
                logger.info("Legacy google-generativeai client initialized.")
            except Exception as e:
                logger.warning(f"Could not initialize Gemini SDK: {e}")

    async def generate_response(self, prompt: str, system_instruction: str = "") -> str:
        if not self.client:
            return ""

        try:
            # Check for modern google-genai
            if hasattr(self.client, "models"):
                response = self.client.models.generate_content(
                    model=self.model_name,
                    contents=prompt,
                    config={"system_instruction": system_instruction} if system_instruction else None
                )
                return response.text.strip()
            elif hasattr(self.client, "generate_content"):
                response = self.client.generate_content(prompt)
                return response.text.strip()
        except Exception as e:
            logger.error(f"Gemini generation error: {e}")
            return ""
        return ""
