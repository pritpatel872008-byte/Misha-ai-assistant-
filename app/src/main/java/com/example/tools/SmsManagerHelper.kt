package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.ai.ToolExecutionResult
import com.example.ai.ToolStatus

class SmsManagerHelper(private val context: Context) {

    private val contactsHelper = ContactsHelper(context)

    fun sendSms(recipient: String, message: String): ToolExecutionResult {
        if (message.isBlank()) {
            return ToolExecutionResult(
                status = ToolStatus.FAILED,
                message = "Message text is empty.",
                spokenResponse = "What message would you like me to send?"
            )
        }

        var targetNumber = recipient.trim()
        var displayName = recipient.trim()

        val isNumber = targetNumber.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
        if (!isNumber) {
            when (val resolution = contactsHelper.findContact(targetNumber)) {
                is ContactResolutionResult.SingleMatch -> {
                    targetNumber = resolution.contact.phoneNumber
                    displayName = resolution.contact.name
                }
                is ContactResolutionResult.AmbiguousMatches -> {
                    val names = resolution.candidates.joinToString(", ") { it.name }
                    return ToolExecutionResult(
                        status = ToolStatus.CONFIRMATION_REQUIRED,
                        message = "Ambiguous contact matches for SMS: $names",
                        spokenResponse = "Found multiple contacts named $displayName ($names). Which one?"
                    )
                }
                is ContactResolutionResult.NotFound -> {
                    return ToolExecutionResult(
                        status = ToolStatus.FAILED,
                        message = "Contact \"$displayName\" not found.",
                        spokenResponse = "I couldn't find $displayName in contacts."
                    )
                }
                is ContactResolutionResult.PermissionMissing -> {
                    return ToolExecutionResult(
                        status = ToolStatus.PERMISSION_REQUIRED,
                        message = "Contacts permission needed to resolve phone number.",
                        spokenResponse = "Please grant Contacts permission."
                    )
                }
            }
        }

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSmsPermission) {
            return try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                smsManager.sendTextMessage(targetNumber, null, message, null, null)
                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = "SMS dispatched directly to $displayName ($targetNumber): \"$message\"",
                    spokenResponse = "SMS sent to $displayName."
                )
            } catch (e: Exception) {
                // Fallback to composer
                openSmsComposer(targetNumber, message, displayName)
            }
        } else {
            // Open composer without claiming direct delivery
            return openSmsComposer(targetNumber, message, displayName)
        }
    }

    private fun openSmsComposer(number: String, message: String, displayName: String): ToolExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(number)}")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecutionResult(
                status = ToolStatus.USER_ACTION_REQUIRED,
                message = "Opened SMS composer for $displayName. Delivery not verified until sent by user.",
                spokenResponse = "Opened SMS composer for $displayName."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                status = ToolStatus.FAILED,
                message = "Failed to launch SMS composer: ${e.message}",
                spokenResponse = "Unable to open SMS app."
            )
        }
    }
}
