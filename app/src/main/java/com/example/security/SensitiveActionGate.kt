package com.example.security

import android.content.Context
import com.example.data.repository.MishaRepository

enum class SensitivityLevel {
    NORMAL,
    ELEVATED,
    HIGH_IMPACT
}

data class SecurityGateDecision(
    val isAllowed: Boolean,
    val requiresPinOrBiometric: Boolean,
    val requiresUserConfirmation: Boolean,
    val reason: String
)

class SensitiveActionGate(
    private val context: Context,
    private val repository: MishaRepository
) {

    fun evaluateAction(intent: String, isGuestProfile: Boolean = false): SecurityGateDecision {
        return when (intent) {
            "RESET_MISHA", "WIPE_KNOWLEDGE_VAULT", "CLEAR_HISTORY" -> {
                SecurityGateDecision(
                    isAllowed = true,
                    requiresPinOrBiometric = true,
                    requiresUserConfirmation = true,
                    reason = "Destructive data deletion requires explicit verification."
                )
            }

            "TRIGGER_SOS", "UPDATE_SOS_SETTINGS" -> {
                SecurityGateDecision(
                    isAllowed = true,
                    requiresPinOrBiometric = false,
                    requiresUserConfirmation = true,
                    reason = "Emergency SOS configuration changes require confirmation."
                )
            }

            "SEND_SMS", "SEND_WHATSAPP", "MAKE_CALL" -> {
                if (isGuestProfile) {
                    SecurityGateDecision(
                        isAllowed = false,
                        requiresPinOrBiometric = true,
                        requiresUserConfirmation = true,
                        reason = "Telephony and messaging actions are restricted for Guest profile."
                    )
                } else {
                    SecurityGateDecision(
                        isAllowed = true,
                        requiresPinOrBiometric = false,
                        requiresUserConfirmation = true,
                        reason = "Communication tool execution confirmed."
                    )
                }
            }

            else -> {
                SecurityGateDecision(
                    isAllowed = true,
                    requiresPinOrBiometric = false,
                    requiresUserConfirmation = false,
                    reason = "Standard safe tool execution."
                )
            }
        }
    }
}
