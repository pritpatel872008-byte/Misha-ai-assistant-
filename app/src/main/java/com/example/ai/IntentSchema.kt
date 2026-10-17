package com.example.ai

import com.squareup.moshi.JsonClass

object IntentConstants {
    const val GENERAL_AI = "GENERAL_AI"
    const val WEB_SEARCH = "WEB_SEARCH"
    const val WEATHER = "WEATHER"
    const val MAKE_CALL = "MAKE_CALL"
    const val ANSWER_CALL = "ANSWER_CALL"
    const val REJECT_CALL = "REJECT_CALL"
    const val END_CALL = "END_CALL"
    const val SEND_SMS = "SEND_SMS"
    const val SEND_WHATSAPP = "SEND_WHATSAPP"
    const val OPEN_APP = "OPEN_APP"
    const val OPEN_SETTINGS = "OPEN_SETTINGS"
    const val FLASHLIGHT_ON = "FLASHLIGHT_ON"
    const val FLASHLIGHT_OFF = "FLASHLIGHT_OFF"
    const val LOCK_DEVICE = "LOCK_DEVICE"
    const val VOLUME_UP = "VOLUME_UP"
    const val VOLUME_DOWN = "VOLUME_DOWN"
    const val SET_VOLUME = "SET_VOLUME"
    const val SET_BRIGHTNESS = "SET_BRIGHTNESS"
    const val WIFI_SETTINGS = "WIFI_SETTINGS"
    const val BLUETOOTH_SETTINGS = "BLUETOOTH_SETTINGS"
    const val BATTERY_STATUS = "BATTERY_STATUS"
    const val DEVICE_INFO = "DEVICE_INFO"
    const val SET_ALARM = "SET_ALARM"
    const val SET_REMINDER = "SET_REMINDER"
    const val CAMERA = "CAMERA"
    const val MUSIC = "MUSIC"
    const val TRIGGER_SOS = "TRIGGER_SOS"
    const val TEST_SOS = "TEST_SOS"
    const val CANCEL_SOS = "CANCEL_SOS"
    const val CONFIRM_SOS = "CONFIRM_SOS"
    const val STOP = "STOP"
    const val CANCEL = "CANCEL"
    const val HELP = "HELP"
    
    // V4 Intelligence Extensions
    const val KNOWLEDGE_QUERY = "KNOWLEDGE_QUERY"
    const val AGENT_PLAN = "AGENT_PLAN"
    const val VISION_ANALYZE = "VISION_ANALYZE"
    const val SCREEN_UNDERSTAND = "SCREEN_UNDERSTAND"
    const val CUSTOM_COMMAND = "CUSTOM_COMMAND"
    const val DAILY_BRIEFING = "DAILY_BRIEFING"
}

@JsonClass(generateAdapter = true)
data class MishaParsedIntent(
    val intent: String,
    val arguments: Map<String, String> = emptyMap(),
    val spoken_response: String,
    val requires_confirmation: Boolean = false,
    val confirmation_prompt: String? = null
)

data class ToolExecutionResult(
    val status: ToolStatus,
    val message: String,
    val spokenResponse: String,
    val data: Map<String, Any> = emptyMap()
)

enum class ToolStatus {
    SUCCESS,
    FAILED,
    CONFIRMATION_REQUIRED,
    PERMISSION_REQUIRED,
    NOT_SUPPORTED,
    USER_ACTION_REQUIRED
}

enum class SosDeliveryState(val label: String, val description: String) {
    SOS_IDLE("Standby", "Emergency system armed and ready"),
    SOS_PREPARING("Preparing", "Resolving contacts, location and telemetry"),
    SOS_CONFIRMATION_REQUIRED("Confirmation Required", "Awaiting emergency dispatch verification"),
    SOS_COUNTDOWN("Countdown Active", "Dispatching in countdown seconds. Tap cancel to abort."),
    SOS_SENDING("Dispatching SMS", "Transmitting emergency payloads via telephony subsystem"),
    SOS_SENT("SOS Dispatched", "Emergency SMS payload transmitted successfully to all active contacts"),
    SOS_PARTIALLY_SENT("Partially Sent", "SOS transmitted to some contacts, failed for others"),
    SOS_FAILED("Dispatch Failed", "Unable to transmit SMS. Direct call required."),
    SOS_PERMISSION_REQUIRED("Permission Needed", "SMS permission required for emergency dispatch"),
    SOS_CANCELLED("Cancelled", "Emergency SOS was aborted safely by user")
}

data class SosDispatchResult(
    val state: SosDeliveryState,
    val message: String,
    val spokenResponse: String,
    val totalRecipients: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val details: String = "",
    val isTest: Boolean = false
)

