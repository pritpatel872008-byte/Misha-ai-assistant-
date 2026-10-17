package com.example.voice

import com.example.ai.IntentConstants
import com.example.ai.MishaParsedIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveConversationContext(
    val lastIntent: String? = null,
    val lastTopic: String? = null, // e.g. "WEATHER", "CALL", "SMS", "KNOWLEDGE"
    val lastLocation: String? = null, // e.g. "Vadodara", "Ahmedabad"
    val lastRecipient: String? = null, // e.g. "Rahul", "Mom"
    val lastTimeframe: String? = null, // e.g. "today", "tomorrow"
    val lastTimestamp: Long = System.currentTimeMillis(),
    val isWindowActive: Boolean = false
)

class ContextConversationManager {

    private val _contextState = MutableStateFlow(ActiveConversationContext())
    val contextState: StateFlow<ActiveConversationContext> = _contextState.asStateFlow()

    fun updateContext(intent: MishaParsedIntent) {
        val current = _contextState.value
        val loc = intent.arguments["location"] ?: current.lastLocation
        val rec = intent.arguments["recipient"] ?: current.lastRecipient
        val time = intent.arguments["time"] ?: current.lastTimeframe

        _contextState.value = ActiveConversationContext(
            lastIntent = intent.intent,
            lastTopic = mapIntentToTopic(intent.intent),
            lastLocation = loc,
            lastRecipient = rec,
            lastTimeframe = time,
            lastTimestamp = System.currentTimeMillis(),
            isWindowActive = true
        )
    }

    /**
     * Resolves short follow-up phrases using conversational context memory.
     */
    fun resolveContextualFollowUp(userText: String, timeoutSeconds: Int = 10): MishaParsedIntent? {
        val current = _contextState.value
        val elapsedSec = (System.currentTimeMillis() - current.lastTimestamp) / 1000

        if (!current.isWindowActive || elapsedSec > timeoutSeconds) {
            _contextState.value = _contextState.value.copy(isWindowActive = false)
            return null
        }

        val q = userText.trim().lowercase()

        // Stop / End conversation window
        if (q == "bas" || q == "kuch nahi" || q == "chup" || q == "done" || q == "stop" || q == "cancel") {
            _contextState.value = _contextState.value.copy(isWindowActive = false)
            return MishaParsedIntent(
                intent = IntentConstants.STOP,
                spoken_response = "Standby par ja rahi hoon."
            )
        }

        // Weather follow-up: "Kal ka?", "Tomorrow?", "Agle hafte?"
        if (current.lastTopic == "WEATHER") {
            if (q.contains("kal") || q.contains("tomorrow") || q.contains("parso") || q.contains("today") || q.contains("aaj")) {
                val timeframe = if (q.contains("kal") || q.contains("tomorrow")) "tomorrow" else "today"
                val loc = current.lastLocation ?: "Ahmedabad"
                _contextState.value = current.copy(lastTimeframe = timeframe, lastTimestamp = System.currentTimeMillis())
                return MishaParsedIntent(
                    intent = IntentConstants.WEATHER,
                    arguments = mapOf("location" to loc, "timeframe" to timeframe),
                    spoken_response = "Checking weather for $loc ($timeframe)."
                )
            }

            // Location switch: "Ahmedabad ka.", "Mumbai ka?", "In Delhi"
            val potentialCity = q.removeSuffix("ka").removeSuffix("ka?").removePrefix("in ").trim()
            if (potentialCity.isNotBlank() && potentialCity.length in 3..20 && !potentialCity.contains(" ")) {
                _contextState.value = current.copy(lastLocation = potentialCity, lastTimestamp = System.currentTimeMillis())
                return MishaParsedIntent(
                    intent = IntentConstants.WEATHER,
                    arguments = mapOf("location" to potentialCity, "timeframe" to (current.lastTimeframe ?: "today")),
                    spoken_response = "Checking weather for $potentialCity."
                )
            }
        }

        // Call follow-up: "Mom ko?", "Rahul ko?"
        if (current.lastTopic == "CALL" && (q.contains("ko") || q.startsWith("to "))) {
            val rec = q.removeSuffix("ko").removePrefix("to ").trim()
            _contextState.value = current.copy(lastRecipient = rec, lastTimestamp = System.currentTimeMillis())
            return MishaParsedIntent(
                intent = IntentConstants.MAKE_CALL,
                arguments = mapOf("recipient" to rec),
                spoken_response = "Calling $rec."
            )
        }

        return null
    }

    fun endWindow() {
        _contextState.value = _contextState.value.copy(isWindowActive = false)
    }

    private fun mapIntentToTopic(intent: String): String {
        return when (intent) {
            IntentConstants.WEATHER -> "WEATHER"
            IntentConstants.MAKE_CALL -> "CALL"
            IntentConstants.SEND_SMS, IntentConstants.SEND_WHATSAPP -> "MESSAGING"
            IntentConstants.KNOWLEDGE_QUERY -> "KNOWLEDGE"
            else -> "GENERAL"
        }
    }
}
