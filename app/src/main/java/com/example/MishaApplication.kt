package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.MishaDatabase
import com.example.data.repository.MishaRepository
import com.example.voice.MishaTextToSpeech

class MishaApplication : Application() {

    lateinit var database: MishaDatabase
        private set

    lateinit var repository: MishaRepository
        private set

    lateinit var tts: MishaTextToSpeech
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = MishaDatabase.getDatabase(this)
        repository = MishaRepository(
            historyDao = database.historyDao(),
            memoryDao = database.memoryDao(),
            routineDao = database.routineDao(),
            securityLogDao = database.securityLogDao(),
            settingsDao = database.settingsDao(),
            emergencyContactDao = database.emergencyContactDao(),
            sosSettingsDao = database.sosSettingsDao(),
            sosEventDao = database.sosEventDao(),
            proactiveSettingsDao = database.proactiveSettingsDao(),
            voiceProfileDao = database.voiceProfileDao(),
            customCommandDao = database.customCommandDao(),
            knowledgeDao = database.knowledgeDao(),
            agentPlanDao = database.agentPlanDao(),
            auditLogDao = database.auditLogDao(),
            userDao = database.userDao()
        )
        tts = MishaTextToSpeech(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ASSISTANT_SERVICE,
                "Misha Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground listening and voice coordination"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ASSISTANT_SERVICE = "misha_voice_service_channel"
        lateinit var instance: MishaApplication
            private set
    }
}
