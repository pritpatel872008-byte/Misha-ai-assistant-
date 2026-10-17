package com.example.security

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tools.MishaAccessibilityService

enum class MishaPermissionStatus(val label: String, val isOperational: Boolean) {
    GRANTED("Granted", true),
    DENIED("Denied", false),
    PERMANENTLY_DENIED("Permanently Denied", false),
    NOT_REQUESTED("Not Requested", false),
    UNAVAILABLE("Hardware Unavailable", false)
}

data class PermissionEvaluation(
    val permissionId: String,
    val title: String,
    val status: MishaPermissionStatus,
    val explanation: String,
    val isCritical: Boolean = false
)

class PermissionInspector(private val context: Context) {

    fun evaluateAllPermissions(activity: Activity? = null): List<PermissionEvaluation> {
        val list = mutableListOf<PermissionEvaluation>()

        // 1. Microphone
        val micStatus = checkRuntimePermission(Manifest.permission.RECORD_AUDIO, activity)
        list.add(
            PermissionEvaluation(
                permissionId = Manifest.permission.RECORD_AUDIO,
                title = "Microphone & Voice Input",
                status = micStatus,
                explanation = "Required for wake-word detection, speech-to-text, and acoustic monitoring.",
                isCritical = true
            )
        )

        // 2. Notifications (Android 13+)
        val notifStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkRuntimePermission(Manifest.permission.POST_NOTIFICATIONS, activity)
        } else {
            MishaPermissionStatus.GRANTED
        }
        list.add(
            PermissionEvaluation(
                permissionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else "NOTIFICATIONS_LEGACY",
                title = "Foreground Notifications",
                status = notifStatus,
                explanation = "Required to keep background assistant alive and show status."
            )
        )

        // 3. Contacts
        val contactsStatus = checkRuntimePermission(Manifest.permission.READ_CONTACTS, activity)
        list.add(
            PermissionEvaluation(
                permissionId = Manifest.permission.READ_CONTACTS,
                title = "Contacts Lookup",
                status = contactsStatus,
                explanation = "Used to resolve names for hands-free calls, SMS, and WhatsApp automation."
            )
        )

        // 4. Calls
        val callStatus = checkRuntimePermission(Manifest.permission.CALL_PHONE, activity)
        list.add(
            PermissionEvaluation(
                permissionId = Manifest.permission.CALL_PHONE,
                title = "Direct Phone Calling",
                status = callStatus,
                explanation = "Allows direct hands-free dialling to contacts or emergency services."
            )
        )

        // 5. SMS
        val smsStatus = checkRuntimePermission(Manifest.permission.SEND_SMS, activity)
        list.add(
            PermissionEvaluation(
                permissionId = Manifest.permission.SEND_SMS,
                title = "SMS Transmission",
                status = smsStatus,
                explanation = "Enables hands-free SMS dispatch and automated emergency SOS messages."
            )
        )

        // 6. Location
        val locStatus = checkRuntimePermission(Manifest.permission.ACCESS_FINE_LOCATION, activity)
        list.add(
            PermissionEvaluation(
                permissionId = Manifest.permission.ACCESS_FINE_LOCATION,
                title = "Precise Device Location",
                status = locStatus,
                explanation = "Used exclusively for Emergency SOS coordinate sharing and local weather."
            )
        )

        // 7. Accessibility Service
        val accessStatus = if (MishaAccessibilityService.isServiceRunning) {
            MishaPermissionStatus.GRANTED
        } else {
            MishaPermissionStatus.DENIED
        }
        list.add(
            PermissionEvaluation(
                permissionId = "ACCESSIBILITY_SERVICE",
                title = "Accessibility Automation Bridge",
                status = accessStatus,
                explanation = "Automates and verifies hands-free messaging in WhatsApp."
            )
        )

        // 8. Device Admin (Lock Phone)
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = MishaDeviceAdminReceiver.getComponentName(context)
        val adminActive = dpm?.isAdminActive(adminComponent) == true
        list.add(
            PermissionEvaluation(
                permissionId = "DEVICE_ADMIN",
                title = "Device Administrator",
                status = if (adminActive) MishaPermissionStatus.GRANTED else MishaPermissionStatus.DENIED,
                explanation = "Enables legitimate system screen locking without bypassing OS security."
            )
        )

        return list
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkRuntimePermission(permission: String, activity: Activity?): MishaPermissionStatus {
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) return MishaPermissionStatus.GRANTED

        if (activity != null) {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            return if (shouldShowRationale) {
                MishaPermissionStatus.DENIED
            } else {
                MishaPermissionStatus.NOT_REQUESTED
            }
        }

        return MishaPermissionStatus.DENIED
    }
}
