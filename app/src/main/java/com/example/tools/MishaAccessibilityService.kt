package com.example.tools

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.ai.ToolExecutionResult
import com.example.ai.ToolStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MishaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true
        Log.i("MishaAccessibility", "Service connected and active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: ""

        if ((packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") && pendingWhatsAppTask != null) {
            handleWhatsAppAutomation()
        }
    }

    private fun handleWhatsAppAutomation() {
        val task = pendingWhatsAppTask ?: return
        val rootNode = rootInActiveWindow ?: return

        try {
            // Step 1: Look for the message entry field
            val editTexts = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
            if (editTexts.isNotEmpty()) {
                val inputNode = editTexts[0]
                val arguments = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        task.message
                    )
                }
                inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                // Give UI time to update send button state and refresh node hierarchy
                Handler(Looper.getMainLooper()).postDelayed({
                    val freshRoot = rootInActiveWindow
                    if (freshRoot != null) {
                        freshRoot.refresh()
                        var sendBtn: AccessibilityNodeInfo? = null

                        val sendButtons = freshRoot.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
                        if (sendButtons.isNotEmpty()) {
                            sendBtn = sendButtons[0]
                        } else {
                            // Fallback scan by text/description
                            val sendDescList = freshRoot.findAccessibilityNodeInfosByText("Send")
                            if (sendDescList.isNotEmpty()) {
                                sendBtn = sendDescList[0]
                            }
                        }

                        if (sendBtn != null && sendBtn.isClickable) {
                            val clicked = sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            if (clicked) {
                                pendingWhatsAppTask = null
                                _automationResults.tryEmit(
                                    ToolExecutionResult(
                                        status = ToolStatus.SUCCESS,
                                        message = "WhatsApp message to ${task.recipient} successfully dispatched and verified.",
                                        spokenResponse = "Maine ${task.recipient} ko WhatsApp message bhej diya."
                                    )
                                )
                            }
                        }
                    }
                }, 350)
            }
        } catch (e: Exception) {
            Log.e("MishaAccessibility", "WhatsApp automation error", e)
        }
    }

    override fun onInterrupt() {
        isServiceRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (instance == this) instance = null
    }

    data class WhatsAppTask(val recipient: String, val message: String, val timestamp: Long = System.currentTimeMillis())

    companion object {
        @Volatile
        var instance: MishaAccessibilityService? = null
            private set

        var isServiceRunning = false
            private set

        var pendingWhatsAppTask: WhatsAppTask? = null

        private val _automationResults = MutableSharedFlow<ToolExecutionResult>(extraBufferCapacity = 1)
        val automationResults = _automationResults.asSharedFlow()

        fun isWhatsAppInstalled(context: Context): Boolean {
            val pm = context.packageManager
            return try {
                pm.getPackageInfo("com.whatsapp", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                try {
                    pm.getPackageInfo("com.whatsapp.w4b", 0)
                    true
                } catch (e2: Exception) {
                    false
                }
            }
        }

        fun executeWhatsAppMessage(
            context: Context,
            recipient: String,
            message: String
        ): ToolExecutionResult {
            if (!isWhatsAppInstalled(context)) {
                return ToolExecutionResult(
                    status = ToolStatus.FAILED,
                    message = "WhatsApp is not installed on this device.",
                    spokenResponse = "WhatsApp isn't installed on this phone."
                )
            }

            if (message.isBlank()) {
                return ToolExecutionResult(
                    status = ToolStatus.FAILED,
                    message = "Message cannot be empty.",
                    spokenResponse = "Please provide the message text."
                )
            }

            val contactsHelper = ContactsHelper(context)
            var targetNumber = recipient.trim()

            if (!targetNumber.all { it.isDigit() || it == '+' || it == ' ' }) {
                when (val res = contactsHelper.findContact(recipient)) {
                    is ContactResolutionResult.SingleMatch -> {
                        targetNumber = res.contact.phoneNumber
                    }
                    is ContactResolutionResult.AmbiguousMatches -> {
                        val names = res.candidates.joinToString(", ") { it.name }
                        return ToolExecutionResult(
                            status = ToolStatus.CONFIRMATION_REQUIRED,
                            message = "Multiple WhatsApp contacts found: $names",
                            spokenResponse = "Found multiple contacts: $names. Which one?"
                        )
                    }
                    is ContactResolutionResult.NotFound -> {
                        return ToolExecutionResult(
                            status = ToolStatus.FAILED,
                            message = "Contact \"$recipient\" not found.",
                            spokenResponse = "I couldn't find $recipient in your contacts."
                        )
                    }
                    is ContactResolutionResult.PermissionMissing -> {
                        return ToolExecutionResult(
                            status = ToolStatus.PERMISSION_REQUIRED,
                            message = "Contacts permission needed to resolve WhatsApp number.",
                            spokenResponse = "Please allow Contacts permission."
                        )
                    }
                }
            }

            // Clean number for WhatsApp URL
            val cleanPhone = targetNumber.replace("+", "").replace(" ", "").replace("-", "")

            if (instance != null && isServiceRunning) {
                // Register task for accessibility service verification
                pendingWhatsAppTask = WhatsAppTask(recipient, message)
            }

            try {
                val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)

                return if (instance != null && isServiceRunning) {
                    ToolExecutionResult(
                        status = ToolStatus.USER_ACTION_REQUIRED,
                        message = "WhatsApp opened. Accessibility automation verifying send action...",
                        spokenResponse = "Opening WhatsApp to send message to $recipient."
                    )
                } else {
                    ToolExecutionResult(
                        status = ToolStatus.USER_ACTION_REQUIRED,
                        message = "WhatsApp conversation opened. Enable Misha Accessibility Service in Settings for fully verified auto-send.",
                        spokenResponse = "WhatsApp conversation opened for $recipient."
                    )
                }
            } catch (e: Exception) {
                pendingWhatsAppTask = null
                return ToolExecutionResult(
                    status = ToolStatus.FAILED,
                    message = "Could not open WhatsApp: ${e.message}",
                    spokenResponse = "Failed to launch WhatsApp."
                )
            }
        }
    }
}
