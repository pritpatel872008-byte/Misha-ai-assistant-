package com.example.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.ai.MishaEvent
import com.example.ai.MishaEventBus
import com.example.data.local.entities.ProactiveSettingsEntity
import com.example.data.repository.MishaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.*

enum class NotificationPriority {
    IMPORTANT,
    NORMAL,
    IGNORE
}

data class ProactiveAlertItem(
    val id: String,
    val category: String,
    val title: String,
    val message: String,
    val priority: NotificationPriority,
    val timestamp: Long = System.currentTimeMillis()
)

class NotificationPolicyEngine {
    fun evaluate(
        category: String,
        settings: ProactiveSettingsEntity,
        isQuietHoursActive: Boolean
    ): NotificationPriority {
        if (!settings.proactiveAssistantEnabled) return NotificationPriority.IGNORE

        if (isQuietHoursActive) {
            // Only critical alerts pass during quiet hours
            return if (category == "SOS_READINESS" || category == "CRITICAL_BATTERY") {
                NotificationPriority.IMPORTANT
            } else {
                NotificationPriority.IGNORE
            }
        }

        return when (category) {
            "LOW_BATTERY" -> if (settings.lowBatteryAlerts) NotificationPriority.IMPORTANT else NotificationPriority.IGNORE
            "CHARGING_COMPLETE" -> if (settings.chargingCompletedAlerts) NotificationPriority.NORMAL else NotificationPriority.IGNORE
            "MISSED_CALL" -> if (settings.missedCallAlerts) NotificationPriority.IMPORTANT else NotificationPriority.IGNORE
            "ROUTINE" -> if (settings.routineAlerts) NotificationPriority.NORMAL else NotificationPriority.IGNORE
            "SOS_READINESS" -> if (settings.sosReadinessAlerts) NotificationPriority.IMPORTANT else NotificationPriority.IGNORE
            else -> NotificationPriority.NORMAL
        }
    }
}

class ProactiveIntelligenceManager(
    private val context: Context,
    private val repository: MishaRepository,
    private val scope: CoroutineScope
) {
    private val policyEngine = NotificationPolicyEngine()
    private var isLowBatteryAlertFired = false
    private var isChargeAlertFired = false

    fun startMonitoring() {
        scope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    checkBatteryAndHardware()
                    checkSosReadiness()
                } catch (e: Exception) {
                    // non-fatal
                }
                delay(30_000) // check every 30 seconds
            }
        }
    }

    private suspend fun checkBatteryAndHardware() {
        val settings = repository.getProactiveSettingsDirect()
        val isQuiet = isCurrentlyQuietHours(settings)

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val statusIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = statusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Low Battery Check (<= 15% and not charging)
        if (level in 1..15 && !isCharging) {
            if (!isLowBatteryAlertFired) {
                val priority = policyEngine.evaluate("LOW_BATTERY", settings, isQuiet)
                if (priority != NotificationPriority.IGNORE) {
                    isLowBatteryAlertFired = true
                    emitProactiveAlert(
                        category = "LOW_BATTERY",
                        title = "Low Battery Warning",
                        message = "Battery level is $level%. Connect charger to prevent shutdown.",
                        priority = priority
                    )
                }
            }
        } else if (level > 20) {
            isLowBatteryAlertFired = false
        }

        // Charging Completed Check (>= 98% while plugged in)
        if (level >= 98 && isCharging) {
            if (!isChargeAlertFired) {
                val priority = policyEngine.evaluate("CHARGING_COMPLETE", settings, isQuiet)
                if (priority != NotificationPriority.IGNORE) {
                    isChargeAlertFired = true
                    emitProactiveAlert(
                        category = "CHARGING_COMPLETE",
                        title = "Charging Complete",
                        message = "Device battery is fully charged ($level%).",
                        priority = priority
                    )
                }
            }
        } else if (!isCharging) {
            isChargeAlertFired = false
        }
    }

    private suspend fun checkSosReadiness() {
        val sosSettings = repository.getSosSettingsDirect()
        val contacts = repository.getEmergencyContactsList()
        val proactiveSettings = repository.getProactiveSettingsDirect()

        if (sosSettings.isSosEnabled && contacts.isEmpty()) {
            val priority = policyEngine.evaluate("SOS_READINESS", proactiveSettings, false)
            if (priority != NotificationPriority.IGNORE) {
                // only log once in audit
            }
        }
    }

    private suspend fun emitProactiveAlert(category: String, title: String, message: String, priority: NotificationPriority) {
        val reqId = MishaEventBus.newRequestId()
        MishaEventBus.emit(
            MishaEvent.ProactiveAlert(
                requestId = reqId,
                category = category,
                title = title,
                description = message,
                priority = priority.name
            )
        )
        repository.logAudit(
            requestId = reqId,
            category = "PROACTIVE",
            commandText = title,
            status = "VERIFIED_SUCCESS",
            details = message
        )
    }

    private fun isCurrentlyQuietHours(settings: ProactiveSettingsEntity): Boolean {
        if (!settings.quietHoursEnabled) return false
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val startHour = settings.quietHoursStart.substringBefore(":").toIntOrNull() ?: 22
        val endHour = settings.quietHoursEnd.substringBefore(":").toIntOrNull() ?: 7

        return if (startHour > endHour) {
            hour >= startHour || hour < endHour
        } else {
            hour in startHour until endHour
        }
    }
}
