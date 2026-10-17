package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.ai.ToolExecutionResult
import com.example.ai.ToolStatus

class TelecomCallManager(private val context: Context) {

    private val contactsHelper = ContactsHelper(context)

    fun makeCall(recipientOrNumber: String): ToolExecutionResult {
        val cleanInput = recipientOrNumber.trim()
        val isNumber = cleanInput.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }

        var targetNumber = cleanInput
        var displayName = cleanInput

        if (!isNumber) {
            when (val resolution = contactsHelper.findContact(cleanInput)) {
                is ContactResolutionResult.SingleMatch -> {
                    targetNumber = resolution.contact.phoneNumber
                    displayName = resolution.contact.name
                }
                is ContactResolutionResult.AmbiguousMatches -> {
                    val names = resolution.candidates.joinToString(" or ") { "${it.name} (${it.phoneNumber})" }
                    return ToolExecutionResult(
                        status = ToolStatus.CONFIRMATION_REQUIRED,
                        message = "Ambiguous contact matches: $names",
                        spokenResponse = "I found multiple contacts for $cleanInput: $names. Which one should I call?"
                    )
                }
                is ContactResolutionResult.NotFound -> {
                    return ToolExecutionResult(
                        status = ToolStatus.FAILED,
                        message = "Contact \"$cleanInput\" not found.",
                        spokenResponse = "I couldn't find $cleanInput in your contacts."
                    )
                }
                is ContactResolutionResult.PermissionMissing -> {
                    return ToolExecutionResult(
                        status = ToolStatus.PERMISSION_REQUIRED,
                        message = "Contacts permission required to find $cleanInput.",
                        spokenResponse = "Please allow Contacts permission so I can look up phone numbers."
                    )
                }
            }
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(targetNumber)}"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(targetNumber)}"))
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ToolExecutionResult(
                status = ToolStatus.SUCCESS,
                message = "Calling $displayName ($targetNumber).",
                spokenResponse = "Calling $displayName."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                status = ToolStatus.FAILED,
                message = "Failed to initiate call: ${e.message}",
                spokenResponse = "Unable to start call."
            )
        }
    }

    /**
     * Cellular Call audio limitation disclaimer as required:
     * Normal Android third-party apps cannot inject raw synthesized AI speech into active cellular calls.
     */
    fun getCellularCallLimitationNotice(): String {
        return "Note: AI conversation injection into active cellular calls is restricted by Android OS for privacy and hardware security."
    }
}
