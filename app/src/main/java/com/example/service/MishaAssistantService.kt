package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.MishaApplication
import com.example.voice.MicCaptureEngine

class MishaAssistantService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasMicPerm = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasMicPerm) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("MishaAssistantService", "Foreground service start failed", e)
        }

        MicCaptureEngine.instance.startCapture(this)

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, MishaApplication.CHANNEL_ASSISTANT_SERVICE)
            .setContentTitle("MISHA AI Active")
            .setContentText("Listening for wake word \"Misha\" & acoustic triggers")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        MicCaptureEngine.instance.stopCapture()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val WAKE_NOTIFICATION_ID = 1002
        private const val WAKE_CHANNEL_ID = "misha_wake_alerts"
        var isServiceRunning = false
            private set


        fun notifyWakeDetected(context: Context) {
            val appContext = context.applicationContext
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    WAKE_CHANNEL_ID,
                    "Misha wake alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Heads-up alert when Misha hears its wake word"
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
                manager.createNotificationChannel(channel)
            }

            val openAssistant = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                1002,
                openAssistant,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(appContext, WAKE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Misha is listening")
                .setContentText("Tap to open your assistant and see the voice response.")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .build()
            manager.notify(WAKE_NOTIFICATION_ID, notification)
        }

        fun start(context: Context) {
            try {
                val intent = Intent(context, MishaAssistantService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("MishaAssistantService", "Unable to start foreground service", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, MishaAssistantService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e("MishaAssistantService", "Unable to stop service", e)
            }
        }
    }
}

