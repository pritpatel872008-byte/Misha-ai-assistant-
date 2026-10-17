package com.example.voice

enum class MishaState(val displayName: String, val description: String) {
    BOOTING("Booting", "Initializing neural cores & audio subsystem..."),
    STANDBY("Standby", "Say \"Misha\" or double clap"),
    WAKE_LISTENING("Ears Active", "Analyzing background acoustic frames"),
    WAKE_DETECTED("Wake Detected", "Misha recognized! Preparing command stream"),
    COMMAND_LISTENING("Listening", "Misha is listening to your command..."),
    THINKING("Thinking", "Gemini & Intent router parsing request..."),
    PROCESSING("Reasoning", "Gemini & Intent router parsing request..."),
    EXECUTING("Executing", "Dispatching verified device action..."),
    SPEAKING("Speaking", "Misha is responding"),
    POST_SPEECH_COOLDOWN("Cooldown", "Echo suppression active"),
    CALL_ACTIVE("Call Active", "Secure communication link established"),
    ERROR_RECOVERY("Recovering", "Recovering audio pipeline...")
}
