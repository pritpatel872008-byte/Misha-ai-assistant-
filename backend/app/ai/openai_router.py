import asyncio
import json
import os
import urllib.error
import urllib.request
from typing import List, Literal, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

router = APIRouter(prefix="/api/ai", tags=["AI"])

class ChatMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=12000)

class ChatRequest(BaseModel):
    messages: List[ChatMessage] = Field(min_length=1, max_length=30)
    system_instruction: Optional[str] = Field(default=None, max_length=4000)

class ChatResponse(BaseModel):
    reply: str
    model: str

def _request_openai(payload: dict, api_key: str) -> dict:
    request = urllib.request.Request(
        "https://api.openai.com/v1/responses",
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=35) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        # Do not return provider details or secrets to the client.
        if exc.code in (401, 403):
            raise HTTPException(status_code=503, detail="AI provider credentials are not configured correctly.")
        if exc.code == 429:
            raise HTTPException(status_code=429, detail="AI usage limit reached. Try again later.")
        raise HTTPException(status_code=502, detail="AI provider request failed.")
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError):
        raise HTTPException(status_code=502, detail="AI provider is temporarily unavailable.")

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    api_key = os.environ.get("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise HTTPException(status_code=503, detail="OpenAI API is not configured on the server.")

    model = os.environ.get("OPENAI_MODEL", "gpt-6-luna").strip()
    input_messages = []
    for message in request.messages:
        input_messages.append({"role": message.role, "content": message.content})

    payload = {
        "model": model,
        "input": input_messages,
        "max_output_tokens": 700,
    }
    if request.system_instruction:
        payload["instructions"] = request.system_instruction

    result = await asyncio.to_thread(_request_openai, payload, api_key)
    reply = result.get("output_text", "").strip()
    if not reply:
        chunks = []
        for item in result.get("output", []):
            for content in item.get("content", []):
                if content.get("type") == "output_text" and content.get("text"):
                    chunks.append(content["text"])
        reply = "\n".join(chunks).strip()

    if not reply:
        raise HTTPException(status_code=502, detail="AI provider returned an empty response.")
    return ChatResponse(reply=reply, model=model)
