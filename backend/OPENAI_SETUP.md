# OpenAI API setup for Misha AI

Misha's OpenAI API key belongs on the **backend server**, never inside the Android app, a committed file, or the APK.

## Backend environment variables

Configure these in the hosting provider's **Environment Variables / Secrets** settings:

- `OPENAI_API_KEY` = your secret API key from https://platform.openai.com/api-keys
- `OPENAI_MODEL` = a model enabled for your API project (default in code: `gpt-4.1-mini`)

Do not commit a real key to GitHub. Do not send the key in chat. If a key is ever exposed, revoke it and create a replacement.

## Local `.env` setup

1. Copy `backend/.env.example` to `backend/.env`.
2. Replace the placeholder with your own API key and choose a model available to your API project.
3. `backend/.env` is ignored by Git. Never commit it.

## Run the backend locally

From the repository root, install dependencies and start FastAPI:

```bash
python -m pip install -r backend/requirements.txt
# Set OPENAI_API_KEY in your terminal environment first.
uvicorn backend.app.main:app --host 0.0.0.0 --port 8765
```

PowerShell example (current terminal only):

```powershell
$env:OPENAI_API_KEY = "YOUR_KEY_HERE"
$env:OPENAI_MODEL = "gpt-4.1-mini"
uvicorn backend.app.main:app --host 0.0.0.0 --port 8765
```

The chat endpoint is `POST /api/ai/chat`. Example JSON body:

```json
{
  "messages": [
    { "role": "user", "content": "Hi Misha, introduce yourself in Hinglish." }
  ],
  "system_instruction": "You are Misha AI, Prit's helpful Android assistant. Reply naturally in the user's language."
}
```

Use the deployed backend's HTTPS base URL from Android; never call OpenAI directly from Android with the secret key.

## Important cost note

ChatGPT Free and OpenAI API billing are separate. API usage may require billing/credits and is not guaranteed to be free. Set usage/spend limits in the OpenAI platform before sharing or deploying the app. The backend endpoint still needs hosting, abuse protection/rate limiting, and an app-to-backend authentication strategy before public production use.
