import json
import logging
import re
from typing import Optional, Dict, Any
from backend.app.voice.models import ParsedActionIntent

logger = logging.getLogger("MishaIntentParser")

class IntentParser:
    """
    Parses natural speech into verified Android action payloads.
    Provides offline regex-based intent extraction + Gemini AI structured extraction.
    """
    def __init__(self, gemini_engine=None):
        self.gemini_engine = gemini_engine

    async def parse(self, transcript: str) -> ParsedActionIntent:
        cleaned = transcript.strip().lower()
        if not cleaned:
            return ParsedActionIntent(action="general_query", parameters={"query": ""})

        # 1. Phone Call
        call_match = re.search(r"(?:call|dial|phone|ring|lagao)\s+([a-zA-Z0-9\s]+)", cleaned)
        if call_match:
            contact = call_match.group(1).replace("ko", "").replace("call", "").strip()
            return ParsedActionIntent(
                action="call_contact",
                parameters={"contact_name": contact.title()},
                spoken_confirmation=f"Calling {contact.title()}."
            )

        # 2. SMS
        sms_match = re.search(r"(?:sms|text|message)\s+(?:to\s+)?([a-zA-Z\s]+?)\s+(?:that|saying|message|ki)?\s*(.+)", cleaned)
        if sms_match:
            recipient = sms_match.group(1).replace("ko", "").strip()
            msg = sms_match.group(2).strip()
            return ParsedActionIntent(
                action="send_sms",
                parameters={"recipient": recipient.title(), "message": msg},
                spoken_confirmation=f"Sending SMS to {recipient.title()}."
            )

        # 3. WhatsApp Message
        wa_match = re.search(r"whatsapp\s+(?:message\s+)?(?:to\s+)?([a-zA-Z\s]+?)\s+(?:that|saying|ko)?\s*(.+)", cleaned)
        if wa_match:
            recipient = wa_match.group(1).replace("ko", "").strip()
            msg = wa_match.group(2).strip()
            return ParsedActionIntent(
                action="send_whatsapp",
                parameters={"recipient": recipient.title(), "message": msg},
                spoken_confirmation=f"Sending WhatsApp message to {recipient.title()}."
            )

        # 4. Flashlight
        if "flashlight" in cleaned or "torch" in cleaned:
            turn_on = "on" in cleaned or "chalu" in cleaned or "jalao" in cleaned
            return ParsedActionIntent(
                action="toggle_flashlight",
                parameters={"state": turn_on},
                spoken_confirmation=f"Turning {'on' if turn_on else 'off'} the flashlight."
            )

        # 5. Lock Screen
        if "lock" in cleaned and ("phone" in cleaned or "screen" in cleaned or "device" in cleaned):
            return ParsedActionIntent(
                action="lock_device",
                parameters={},
                spoken_confirmation="Locking device."
            )

        # 6. Emergency SOS
        if "sos" in cleaned or "emergency" in cleaned or "save me" in cleaned or "bachao" in cleaned:
            return ParsedActionIntent(
                action="emergency_sos",
                parameters={"trigger_type": "VOICE_COMMAND"},
                spoken_confirmation="Emergency SOS protocol initiated.",
                requires_sensitive_auth=False
            )

        # 7. Weather
        if "weather" in cleaned or "mausam" in cleaned or "temperature" in cleaned:
            city = "Current Location"
            city_match = re.search(r"(?:in|of|at)\s+([a-zA-Z\s]+)", cleaned)
            if city_match:
                city = city_match.group(1).strip().title()
            return ParsedActionIntent(
                action="get_weather",
                parameters={"location": city},
                spoken_confirmation=f"Checking weather for {city}."
            )

        # 8. Web Search
        if "search" in cleaned or "google" in cleaned:
            query = cleaned.replace("search", "").replace("google", "").replace("for", "").strip()
            return ParsedActionIntent(
                action="search_web",
                parameters={"query": query},
                spoken_confirmation=f"Searching for {query}."
            )

        # Default query intent
        return ParsedActionIntent(
            action="general_query",
            parameters={"query": transcript},
            spoken_confirmation=None
        )
