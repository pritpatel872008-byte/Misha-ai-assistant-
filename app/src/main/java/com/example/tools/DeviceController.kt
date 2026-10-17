package com.example.tools

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.ai.ToolExecutionResult
import com.example.ai.ToolStatus

class DeviceController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun toggleFlashlight(enable: Boolean): ToolExecutionResult {
        return try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                hasFlash && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager?.cameraIdList?.firstOrNull()

            if (cameraId != null) {
                cameraManager?.setTorchMode(cameraId, enable)
                val state = if (enable) "on" else "off"
                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = "Flashlight turned $state.",
                    spokenResponse = if (enable) "Flashlight is now on." else "Flashlight turned off."
                )
            } else {
                ToolExecutionResult(
                    status = ToolStatus.NOT_SUPPORTED,
                    message = "No flashlight hardware detected.",
                    spokenResponse = "Flashlight hardware is unavailable on this device."
                )
            }
        } catch (e: Exception) {
            Log.e("DeviceController", "Flashlight error", e)
            ToolExecutionResult(
                status = ToolStatus.FAILED,
                message = "Failed to switch flashlight: ${e.message}",
                spokenResponse = "Couldn't toggle the flashlight."
            )
        }
    }

    fun adjustVolume(increase: Boolean): ToolExecutionResult {
        return try {
            val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
            val percent = (currentVol.toFloat() / maxVol.toFloat() * 100).toInt()

            ToolExecutionResult(
                status = ToolStatus.SUCCESS,
                message = "Volume adjusted to $percent%.",
                spokenResponse = "Volume set to $percent percent."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                status = ToolStatus.FAILED,
                message = "Volume control failed: ${e.message}",
                spokenResponse = "Unable to adjust volume."
            )
        }
    }

    fun getBatteryInfo(): ToolExecutionResult {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val isCharging = bm?.isCharging == true

            val statusText = if (isCharging) "charging" else "discharging"
            ToolExecutionResult(
                status = ToolStatus.SUCCESS,
                message = "Battery level: $batteryLevel%, $statusText.",
                spokenResponse = "Battery is at $batteryLevel percent and $statusText.",
                data = mapOf("level" to batteryLevel, "isCharging" to isCharging)
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                status = ToolStatus.FAILED,
                message = "Could not read battery: ${e.message}",
                spokenResponse = "Couldn't read battery status."
            )
        }
    }

    fun getDeviceInfo(): ToolExecutionResult {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val androidVersion = Build.VERSION.RELEASE
        val sdk = Build.VERSION.SDK_INT

        return ToolExecutionResult(
            status = ToolStatus.SUCCESS,
            message = "Device: $manufacturer $model, Android $androidVersion (API $sdk)",
            spokenResponse = "This is a $manufacturer $model running Android $androidVersion.",
            data = mapOf(
                "model" to model,
                "manufacturer" to manufacturer,
                "androidVersion" to androidVersion,
                "sdk" to sdk
            )
        )
    }

    fun openSettingsScreen(action: String = Settings.ACTION_SETTINGS): ToolExecutionResult {
        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecutionResult(
                status = ToolStatus.SUCCESS,
                message = "Opened settings.",
                spokenResponse = "Opening settings."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                status = ToolStatus.FAILED,
                message = "Failed to open settings: ${e.message}",
                spokenResponse = "Could not open settings."
            )
        }
    }

    fun launchAppByName(appName: String): ToolExecutionResult {
        return try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(0)
            val cleanName = appName.lowercase().trim()

            val matchedApp = installedApps.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                label.contains(cleanName) || appInfo.packageName.lowercase().contains(cleanName)
            }

            if (matchedApp != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    val label = pm.getApplicationLabel(matchedApp).toString()
                    ToolExecutionResult(
                        status = ToolStatus.SUCCESS,
                        message = "Launched $label.",
                        spokenResponse = "Opening $label."
                    )
                } else {
                    ToolExecutionResult(
                        status = ToolStatus.NOT_SUPPORTED,
                        message = "No launchable activity found for $appName.",
                        spokenResponse = "Cannot launch $appName directly."
                    )
                }
            } else {
                ToolExecutionResult(
                    status = ToolStatus.FAILED,
                    message = "App \"$appName\" not found on device.",
                    spokenResponse = "I couldn't find $appName installed on your phone."
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                status = ToolStatus.FAILED,
                message = "Error launching app: ${e.message}",
                spokenResponse = "Error opening $appName."
            )
        }
    }

    fun lockDevice(): ToolExecutionResult {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComponent = com.example.security.MishaDeviceAdminReceiver.getComponentName(context)
            if (dpm != null && dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = "Device locked successfully via Device Administrator.",
                    spokenResponse = "Phone locked."
                )
            } else {
                ToolExecutionResult(
                    status = ToolStatus.PERMISSION_REQUIRED,
                    message = "Device Administrator permission is required to lock the screen. Please activate it in Misha Settings.",
                    spokenResponse = "Please enable Device Administrator in settings to lock the phone."
                )
            }
        } catch (e: SecurityException) {
            ToolExecutionResult(
                status = ToolStatus.PERMISSION_REQUIRED,
                message = "Device Admin permission needed: ${e.message}",
                spokenResponse = "Please grant Device Admin permission to lock your phone."
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                status = ToolStatus.FAILED,
                message = "Lock device failed: ${e.message}",
                spokenResponse = "Unable to lock the phone."
            )
        }
    }
}
