import asyncio
import json
import logging
import os
from typing import Dict, Any, Optional
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from dotenv import load_dotenv
from backend.app.voice.pipeline import VoicePipeline
from backend.app.vision.router import router as vision_router, STORAGE_DIR
from backend.app.ai.openai_router import router as openai_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [%(name)s] %(message)s"
)
logger = logging.getLogger("MishaVoiceServer")

# Load local development secrets from backend/.env; hosting secrets remain environment variables.
load_dotenv(dotenv_path=os.path.join(os.path.dirname(os.path.dirname(__file__)), ".env"))

app = FastAPI(title="MISHA AI Core & Vision Studio", version="4.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register API routers
app.include_router(vision_router)
app.include_router(openai_router)

@app.get("/api/images/{filename}")
async def get_stored_image(filename: str):
    file_path = os.path.join(STORAGE_DIR, filename)
    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Image not found")
    return FileResponse(file_path)

active_pipelines: Dict[WebSocket, VoicePipeline] = {}

@app.get("/health")
async def health_check():
    return {
        "status": "HEALTHY",
        "service": "MISHA AI Python Voice Core",
        "version": "4.0.0",
        "supported_audio_format": {
            "sample_rate": 16000,
            "channels": 1,
            "bit_depth": 16,
            "encoding": "PCM_SIGNED_LITTLE_ENDIAN",
            "frame_size_samples": 1024
        }
    }

@app.websocket("/voice")
async def voice_websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    logger.info(f"Android client connected from {websocket.client.host}:{websocket.client.port}")

    async def send_json_to_android(data: Dict[str, Any]):
        try:
            await websocket.send_text(json.dumps(data))
        except Exception as e:
            logger.error(f"Error sending JSON to Android: {e}")

    async def send_audio_to_android(audio_bytes: bytes):
        try:
            await websocket.send_bytes(audio_bytes)
        except Exception as e:
            logger.error(f"Error streaming audio to Android speaker: {e}")

    pipeline = VoicePipeline(
        send_to_android_ws=send_json_to_android,
        send_audio_to_android_ws=send_audio_to_android
    )
    active_pipelines[websocket] = pipeline

    # Send initial ready state
    await send_json_to_android({
        "type": "state_changed",
        "state": pipeline.state_machine.current_state.value,
        "message": "Python Voice Core ready."
    })

    try:
        while True:
            message = await websocket.receive()
            
            # 1. Binary message: Raw 16kHz PCM audio frame from Android MicCaptureEngine
            if "bytes" in message and message["bytes"]:
                raw_pcm = message["bytes"]
                await pipeline.process_incoming_audio_frame(raw_pcm)

            # 2. Text message: Control messages, heartbeats, action results
            elif "text" in message and message["text"]:
                try:
                    payload = json.loads(message["text"])
                    msg_type = payload.get("type")

                    if msg_type == "ping":
                        await send_json_to_android({"type": "pong", "timestamp": payload.get("timestamp")})
                    
                    elif msg_type == "action_result":
                        await pipeline.handle_android_action_result(payload)

                    elif msg_type == "set_wake_sensitivity":
                        sens = payload.get("sensitivity", "MEDIUM")
                        thresh = 0.55 if sens == "HIGH" else (0.75 if sens == "LOW" else 0.65)
                        pipeline.wake_engine.confidence_threshold = thresh
                        logger.info(f"Updated wake word threshold to {thresh}")

                except json.JSONDecodeError:
                    logger.warning(f"Invalid JSON message received: {message['text']}")

    except WebSocketDisconnect:
        logger.info("Android client disconnected.")
    except Exception as e:
        logger.error(f"Unexpected WebSocket error: {e}")
    finally:
        active_pipelines.pop(websocket, None)

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8765))
    uvicorn.run("backend.app.main:app", host="0.0.0.0", port=port, reload=False)
