package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        ConversationMessageEntity::class,
        AssistantActionEntity::class,
        MemoryEntity::class,
        RoutineEntity::class,
        SecurityLogEntity::class,
        AppSettingsEntity::class,
        EmergencyContactEntity::class,
        SosSettingsEntity::class,
        SosEventEntity::class,
        ProactiveSettingsEntity::class,
        VoiceProfileEntity::class,
        CustomCommandEntity::class,
        KnowledgeDocumentEntity::class,
        KnowledgeChunkEntity::class,
        AgentPlanEntity::class,
        AuditLogEntity::class,
        UserEntity::class,
        UserSessionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MishaDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun memoryDao(): MemoryDao
    abstract fun routineDao(): RoutineDao
    abstract fun securityLogDao(): SecurityLogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun sosSettingsDao(): SosSettingsDao
    abstract fun sosEventDao(): SosEventDao
    abstract fun proactiveSettingsDao(): ProactiveSettingsDao
    abstract fun voiceProfileDao(): VoiceProfileDao
    abstract fun customCommandDao(): CustomCommandDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun agentPlanDao(): AgentPlanDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: MishaDatabase? = null

        fun getDatabase(context: Context): MishaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MishaDatabase::class.java,
                    "misha_ai_db"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
