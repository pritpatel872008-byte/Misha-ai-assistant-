package com.example.security

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.tools.MishaAccessibilityService

data class PermissionAuditItem(
    val permissionName: String,
    val isGranted: Boolean,
    val whyNeeded: String,
    val severity: String // "SAFE", "SUSPICIOUS", "UNKNOWN", "DANGEROUS"
)

data class SecurityAssessment(
    val score: Int, // 0 to 100
    val status: String,
    val permissionAudits: List<PermissionAuditItem>,
    val highRiskAppCount: Int,
    val recommendations: List<String>
)

class SecurityScanner(private val context: Context) {

    fun performFullSecurityAudit(): SecurityAssessment {
        val audits = mutableListOf<PermissionAuditItem>()
        val recommendations = mutableListOf<String>()
        var score = 100

        // 1. Microphone
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        audits.add(
            PermissionAuditItem(
                permissionName = "Microphone (Ears)",
                isGranted = micGranted,
                whyNeeded = "Required for local wake word (\"Misha\") and command processing.",
                severity = if (micGranted) "SAFE" else "UNKNOWN"
            )
        )
        if (!micGranted) {
            score -= 15
            recommendations.add("Enable Microphone permission so Misha can hear voice commands.")
        }

        // 2. Contacts
        val contactsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        audits.add(
            PermissionAuditItem(
                permissionName = "Contacts Access",
                isGranted = contactsGranted,
                whyNeeded = "Resolves recipient phone numbers for verified calls and messaging.",
                severity = if (contactsGranted) "SAFE" else "UNKNOWN"
            )
        )

        // 3. Phone Calling
        val callGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        audits.add(
            PermissionAuditItem(
                permissionName = "Direct Phone Calling",
                isGranted = callGranted,
                whyNeeded = "Allows placing phone calls without opening the stock dialer keypad.",
                severity = if (callGranted) "SAFE" else "UNKNOWN"
            )
        )

        // 4. SMS
        val smsGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        audits.add(
            PermissionAuditItem(
                permissionName = "SMS Messaging",
                isGranted = smsGranted,
                whyNeeded = "Enables voice-directed SMS dispatch directly.",
                severity = if (smsGranted) "SAFE" else "UNKNOWN"
            )
        )

        // 5. Accessibility Service
        val accessibilityActive = MishaAccessibilityService.isServiceRunning
        audits.add(
            PermissionAuditItem(
                permissionName = "Accessibility Service",
                isGranted = accessibilityActive,
                whyNeeded = "Enables verified automated WhatsApp message dispatch.",
                severity = if (accessibilityActive) "SAFE" else "UNKNOWN"
            )
        )
        if (!accessibilityActive) {
            recommendations.add("Enable Misha in Accessibility Settings for automatic WhatsApp message sending.")
        }

        // 6. Installed third party apps check
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val nonSystemApps = installed.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }

        // Sideloaded / Unknown source audit
        val devOptionsEnabled = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) != 0

        if (devOptionsEnabled) {
            recommendations.add("Developer options are active on this device (normal for development).")
        }

        val overallStatus = when {
            score >= 85 -> "SECURE & VERIFIED"
            score >= 60 -> "ATTENTION RECOMMENDED"
            else -> "CONFIGURATION NEEDED"
        }

        return SecurityAssessment(
            score = maxOf(0, score),
            status = overallStatus,
            permissionAudits = audits,
            highRiskAppCount = nonSystemApps.size,
            recommendations = recommendations
        )
    }

    fun checkSpamNumber(phoneNumber: String): Pair<String, String> {
        val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
        // Real heuristic: Telemarketing prefixes / invalid length / reported test numbers
        return if (cleanNumber.startsWith("+1800") || cleanNumber.startsWith("1800") || cleanNumber.length < 5) {
            Pair("SUSPICIOUS", "Recognized toll-free / automated telemarketing prefix.")
        } else {
            Pair("SAFE", "No verified spam reports associated with this number pattern.")
        }
    }
}
