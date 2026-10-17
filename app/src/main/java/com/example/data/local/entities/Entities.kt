package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_messages")
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "MISHA"
    val message: String,
    val intent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val requestId: String? = null,
    val confidence: Float = 1.0f,
    val sourceCitations: String? = null
)

@Entity(tableName = "assistant_actions")
data class AssistantActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val intent: String,
    val argumentsJson: String,
    val status: String, // "VERIFIED_SUCCESS", "SUBMITTED", "PARTIAL_SUCCESS", "FAILED", "CANCELLED", "UNKNOWN"
    val resultSummary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val requestId: String? = null,
    val latencyMs: Long = 0
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "PREFERENCE", "CONTACT", "ROUTINE", "LANGUAGE", "FACT"
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val triggerPhrase: String,
    val isEnabled: Boolean = true,
    val actionsJson: String, // List of intent actions to execute
    val scheduledTime: String? = null,
    val iconName: String = "routine"
)

@Entity(tableName = "security_logs")
data class SecurityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val severity: String, // "SAFE", "SUSPICIOUS", "UNKNOWN", "DANGEROUS"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val wakeWordEnabled: Boolean = true,
    val wakeSensitivity: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH"
    val doubleClapEnabled: Boolean = true,
    val sttEngine: String = "AUTO", // "OFFLINE", "ONLINE", "AUTO"
    val ttsPitch: Float = 1.0f,
    val ttsSpeed: Float = 1.0f,
    val memoryEnabled: Boolean = true,
    val dailySecurityCheck: Boolean = true,
    val performance3DMode: String = "HIGH", // "HIGH", "MEDIUM", "LOW"
    val confirmationMode: String = "SMART", // "ALWAYS", "SMART", "NEVER"
    // V4 Intelligence Extensions
    val followUpModeEnabled: Boolean = true,
    val conversationTimeoutSeconds: Int = 10, // 5, 10, 20, 30
    val batteryAdaptiveMode: String = "BALANCED", // "PERFORMANCE", "BALANCED", "BATTERY_SAVER"
    val languagePreference: String = "AUTO", // "AUTO", "HINDI", "ENGLISH", "HINGLISH"
    val voicePersonality: String = "BALANCED", // "FRIENDLY", "PROFESSIONAL", "MINIMAL"
    val responseStyle: String = "BALANCED", // "SHORT", "BALANCED", "DETAILED"
    val screenUnderstandingEnabled: Boolean = true,
    val visionEnabled: Boolean = true,
    val activeVoiceProfile: String = "Default User"
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val priority: Int = 1, // 1: Primary, 2: Secondary, 3: Third, etc.
    val relationship: String = "Emergency Contact",
    val isEnabled: Boolean = true
)

@Entity(tableName = "sos_settings")
data class SosSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isSosEnabled: Boolean = true,
    val requireVoiceConfirmation: Boolean = true,
    val isOneTapEnabled: Boolean = false,
    val countdownSeconds: Int = 10, // 0 = OFF, 5, 10, 15
    val includeLocation: Boolean = true,
    val includeBattery: Boolean = true,
    val includeUserName: Boolean = true,
    val userName: String = "User",
    val customMessage: String = "EMERGENCY: I may need help. This SOS was sent by Misha AI."
)

@Entity(tableName = "sos_events")
data class SosEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // SOS_SENT, SOS_PARTIALLY_SENT, SOS_FAILED, SOS_CANCELLED, SOS_TEST
    val recipients: String,
    val messageBody: String,
    val locationInfo: String? = null,
    val batteryInfo: String? = null,
    val isTest: Boolean = false,
    val deliverySummary: String
)

@Entity(tableName = "proactive_settings")
data class ProactiveSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val proactiveAssistantEnabled: Boolean = true,
    val lowBatteryAlerts: Boolean = true,
    val chargingCompletedAlerts: Boolean = true,
    val missedCallAlerts: Boolean = true,
    val importantNotificationAlerts: Boolean = true,
    val routineAlerts: Boolean = true,
    val sosReadinessAlerts: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00"
)

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "Default User", "Guest"
    val isDefault: Boolean = false,
    val requiresPinForSensitiveActions: Boolean = true,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val preferredLanguage: String = "AUTO",
    val isEnabled: Boolean = true
)

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val triggerPhrase: String,
    val actionsJson: String, // List of action intents e.g. [{"intent": "FLASHLIGHT_ON"}, {"intent": "SET_TIMER", "args": {"minutes": "25"}}]
    val customSpokenResponse: String = "Custom command executed.",
    val isEnabled: Boolean = true,
    val confirmationRequired: Boolean = false
)

@Entity(tableName = "knowledge_documents")
data class KnowledgeDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileName: String,
    val mimeType: String, // "text/plain", "text/markdown", "application/pdf"
    val content: String,
    val summary: String = "",
    val chunksCount: Int = 0,
    val fileSize: Long = 0,
    val indexedAt: Long = System.currentTimeMillis(),
    val category: String = "General"
)

@Entity(tableName = "knowledge_chunks")
data class KnowledgeChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val documentTitle: String,
    val chunkIndex: Int,
    val textSnippet: String,
    val keywords: String // Comma separated keywords for lexical search
)

@Entity(tableName = "agent_plans")
data class AgentPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: String,
    val goalPrompt: String,
    val planSummary: String,
    val stepsJson: String, // List of steps with status
    val status: String, // "CREATED", "WAITING_CONFIRMATION", "IN_PROGRESS", "COMPLETED", "FAILED", "CANCELLED"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val resultSummary: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String, // "VOICE", "WHATSAPP", "CALL", "SMS", "SOS", "DEVICE", "AGENT", "KNOWLEDGE", "SECURITY"
    val commandText: String,
    val outcomeStatus: String, // "VERIFIED_SUCCESS", "SUBMITTED", "PARTIAL_SUCCESS", "FAILED", "CANCELLED", "UNKNOWN"
    val detailsSummary: String,
    val latencyMs: Long = 0
)

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val securityQuestion: String = "What is your primary emergency keyword?",
    val securityAnswerHash: String = "",
    val recoveryPin: String = "1234",
    val resetOtpCode: String? = null,
    val resetOtpExpiry: Long = 0L,
    val failedLoginAttempts: Int = 0,
    val lockedUntilTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isEmailVerified: Boolean = true,
    val avatarColorHex: String = "#00F0FF"
)

@Entity(tableName = "user_sessions")
data class UserSessionEntity(
    @PrimaryKey val sessionId: String = "active_session",
    val userId: Long,
    val email: String,
    val displayName: String,
    val loginTimestamp: Long = System.currentTimeMillis(),
    val rememberMe: Boolean = true
)

