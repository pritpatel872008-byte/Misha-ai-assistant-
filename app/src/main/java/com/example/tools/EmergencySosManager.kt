package com.example.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ai.SosDeliveryState
import com.example.ai.SosDispatchResult
import com.example.data.local.entities.EmergencyContactEntity
import com.example.data.local.entities.SosEventEntity
import com.example.data.local.entities.SosSettingsEntity
import com.example.data.repository.MishaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EmergencySosManager(
    private val context: Context,
    private val repository: MishaRepository
) {

    suspend fun dispatchSos(
        isTest: Boolean = false,
        specificContacts: List<EmergencyContactEntity>? = null
    ): SosDispatchResult = withContext(Dispatchers.IO) {
        val settings = repository.getSosSettingsDirect()
        val activeContacts = specificContacts ?: repository.getActiveEmergencyContacts()

        if (!settings.isSosEnabled && !isTest) {
            return@withContext SosDispatchResult(
                state = SosDeliveryState.SOS_FAILED,
                message = "Emergency SOS is currently disabled in settings.",
                spokenResponse = "Emergency SOS is disabled in settings.",
                isTest = isTest
            )
        }

        if (activeContacts.isEmpty()) {
            val failedResult = SosDispatchResult(
                state = SosDeliveryState.SOS_FAILED,
                message = "No emergency contacts configured. Please add contacts in Emergency SOS settings.",
                spokenResponse = "Please configure an emergency contact first.",
                isTest = isTest
            )
            repository.recordSosEvent(
                SosEventEntity(
                    status = SosDeliveryState.SOS_FAILED.name,
                    recipients = "None",
                    messageBody = "No configured emergency contacts",
                    isTest = isTest,
                    deliverySummary = failedResult.message
                )
            )
            return@withContext failedResult
        }

        // Check SMS permission
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSmsPermission) {
            val permResult = SosDispatchResult(
                state = SosDeliveryState.SOS_PERMISSION_REQUIRED,
                message = "SEND_SMS permission is not granted. Cannot dispatch automated SOS messages.",
                spokenResponse = "SMS permission is required to send your emergency SOS.",
                totalRecipients = activeContacts.size,
                isTest = isTest
            )
            repository.recordSosEvent(
                SosEventEntity(
                    status = SosDeliveryState.SOS_PERMISSION_REQUIRED.name,
                    recipients = activeContacts.joinToString { "${it.name} (${it.phoneNumber})" },
                    messageBody = "Aborted: SMS Permission missing",
                    isTest = isTest,
                    deliverySummary = "SMS permission is required to send emergency SOS."
                )
            )
            return@withContext permResult
        }

        // Gather real telemetry
        val batteryPct = if (settings.includeBattery) getRealBatteryPercentage() else null
        val (locationCoord, locationUrl, locationNote) = if (settings.includeLocation) {
            getRealDeviceLocation()
        } else {
            Triple(null, null, "Location sharing disabled by user")
        }

        // Construct message payload
        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val messageBody = buildString {
            if (isTest) {
                append("TEST SOS — This is only a test of Misha's emergency system.")
            } else {
                append(settings.customMessage.ifBlank { "EMERGENCY: I may need help. This SOS was sent by Misha AI." })
            }
            append("\n")
            if (settings.includeUserName && settings.userName.isNotBlank()) {
                append("\nUser: ${settings.userName}")
            }
            append("\nTime: $timeString")
            if (batteryPct != null) {
                append("\nBattery: $batteryPct%")
            }
            if (locationUrl != null) {
                append("\nLocation: $locationUrl")
            }
        }.trim()

        val recipientNames = mutableListOf<String>()
        val successfulRecipients = mutableListOf<String>()
        val failedRecipients = mutableListOf<String>()

        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        for (contact in activeContacts) {
            recipientNames.add("${contact.name} (${contact.phoneNumber})")
            val number = contact.phoneNumber.trim().replace(" ", "").replace("-", "")
            try {
                val parts = smsManager.divideMessage(messageBody)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(number, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(number, null, messageBody, null, null)
                }
                successfulRecipients.add(contact.name)
                Log.i("EmergencySosManager", "SOS SMS submitted to ${contact.name} ($number)")
            } catch (e: Exception) {
                Log.e("EmergencySosManager", "Failed to send SOS SMS to ${contact.name} ($number)", e)
                failedRecipients.add("${contact.name} (${e.localizedMessage ?: "Unknown error"})")
            }
        }

        val total = activeContacts.size
        val successCount = successfulRecipients.size
        val failedCount = failedRecipients.size

        val state: SosDeliveryState
        val spoken: String
        val detailSummary: String

        when {
            successCount == total -> {
                state = SosDeliveryState.SOS_SENT
                val names = successfulRecipients.joinToString(", ")
                spoken = if (isTest) {
                    "Test SOS successfully sent to $names."
                } else {
                    if (locationUrl == null && settings.includeLocation) {
                        "I couldn't get your current location, but I've sent the SOS message to $names."
                    } else {
                        "Emergency SOS sent to $names."
                    }
                }
                detailSummary = "Emergency SMS submitted successfully to all $total recipients ($names)."
            }
            successCount > 0 -> {
                state = SosDeliveryState.SOS_PARTIALLY_SENT
                val succ = successfulRecipients.joinToString(", ")
                val fail = failedRecipients.joinToString(", ")
                spoken = "SOS sent to $succ, but failed for $fail."
                detailSummary = "Partially sent ($successCount/$total). Succeeded: $succ. Failed: $fail."
            }
            else -> {
                state = SosDeliveryState.SOS_FAILED
                val fail = failedRecipients.joinToString(", ")
                spoken = "I couldn't send the SOS. Please call your emergency contact directly."
                detailSummary = "Failed to dispatch SOS to any contact ($fail). Direct call required."
            }
        }

        // Record SOS event into local Room database
        repository.recordSosEvent(
            SosEventEntity(
                status = state.name,
                recipients = recipientNames.joinToString(", "),
                messageBody = messageBody,
                locationInfo = locationCoord ?: locationNote,
                batteryInfo = batteryPct?.let { "$it%" },
                isTest = isTest,
                deliverySummary = detailSummary
            )
        )

        return@withContext SosDispatchResult(
            state = state,
            message = detailSummary,
            spokenResponse = spoken,
            totalRecipients = total,
            successCount = successCount,
            failedCount = failedCount,
            details = if (locationUrl != null) "Location: $locationCoord | Battery: $batteryPct%" else "Location: $locationNote | Battery: $batteryPct%",
            isTest = isTest
        )
    }

    private fun getRealBatteryPercentage(): Int? {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("EmergencySosManager", "Battery fetch error", e)
            null
        }
    }

    private fun getRealDeviceLocation(): Triple<String?, String?, String> {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return Triple(null, null, "Location permission not granted")
        }

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                return Triple(null, null, "LocationManager unavailable")
            }

            var bestLocation: Location? = null

            // Check GPS provider first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) bestLocation = loc
            }

            // Check Network provider if GPS is null
            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                val loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) bestLocation = loc
            }

            // Check Passive provider
            if (bestLocation == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                val loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                if (loc != null) bestLocation = loc
            }

            if (bestLocation != null) {
                val lat = String.format(Locale.US, "%.6f", bestLocation.latitude)
                val lng = String.format(Locale.US, "%.6f", bestLocation.longitude)
                val coords = "$lat, $lng"
                val mapUrl = "https://maps.google.com/?q=$lat,$lng"
                Triple(coords, mapUrl, "Acquired from location provider")
            } else {
                Triple(null, null, "Location fix unavailable (GPS/Network returned null)")
            }
        } catch (e: SecurityException) {
            Triple(null, null, "SecurityException: ${e.message}")
        } catch (e: Exception) {
            Triple(null, null, "Location error: ${e.message}")
        }
    }
}
