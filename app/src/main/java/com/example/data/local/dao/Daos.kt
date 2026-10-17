package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM conversation_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ConversationMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationMessageEntity): Long

    @Query("DELETE FROM conversation_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM conversation_messages")
    suspend fun clearAllMessages()

    @Query("SELECT * FROM assistant_actions ORDER BY timestamp DESC")
    fun getAllActions(): Flow<List<AssistantActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: AssistantActionEntity): Long

    @Query("DELETE FROM assistant_actions WHERE id = :id")
    suspend fun deleteAction(id: Long)

    @Query("DELETE FROM assistant_actions")
    suspend fun clearAllActions()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()

    @Query("SELECT * FROM memories WHERE category = :category")
    suspend fun getMemoriesByCategory(category: String): List<MemoryEntity>
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Query("SELECT * FROM routines WHERE isEnabled = 1")
    suspend fun getActiveRoutines(): List<RoutineEntity>
}

@Dao
interface SecurityLogDao {
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SecurityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLogEntity): Long

    @Query("DELETE FROM security_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("DELETE FROM security_logs")
    suspend fun clearAllLogs()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC, id ASC")
    fun getAllContacts(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC, id ASC")
    suspend fun getAllContactsList(): List<EmergencyContactEntity>

    @Query("SELECT * FROM emergency_contacts WHERE isEnabled = 1 ORDER BY priority ASC, id ASC")
    suspend fun getActiveContacts(): List<EmergencyContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Update
    suspend fun updateContact(contact: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteContact(id: Long)

    @Query("DELETE FROM emergency_contacts")
    suspend fun clearAllContacts()
}

@Dao
interface SosSettingsDao {
    @Query("SELECT * FROM sos_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SosSettingsEntity?>

    @Query("SELECT * FROM sos_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): SosSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SosSettingsEntity)
}

@Dao
interface SosEventDao {
    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<SosEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SosEventEntity): Long

    @Query("DELETE FROM sos_events WHERE id = :id")
    suspend fun deleteEvent(id: Long)

    @Query("DELETE FROM sos_events")
    suspend fun clearAllEvents()
}

@Dao
interface ProactiveSettingsDao {
    @Query("SELECT * FROM proactive_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<ProactiveSettingsEntity?>

    @Query("SELECT * FROM proactive_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): ProactiveSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: ProactiveSettingsEntity)
}

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM voice_profiles ORDER BY isDefault DESC, name ASC")
    fun getAllProfiles(): Flow<List<VoiceProfileEntity>>

    @Query("SELECT * FROM voice_profiles ORDER BY isDefault DESC, name ASC")
    suspend fun getAllProfilesList(): List<VoiceProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VoiceProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: VoiceProfileEntity)

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)
}

@Dao
interface CustomCommandDao {
    @Query("SELECT * FROM custom_commands ORDER BY id DESC")
    fun getAllCommands(): Flow<List<CustomCommandEntity>>

    @Query("SELECT * FROM custom_commands WHERE isEnabled = 1")
    suspend fun getActiveCommands(): List<CustomCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CustomCommandEntity): Long

    @Update
    suspend fun updateCommand(command: CustomCommandEntity)

    @Query("DELETE FROM custom_commands WHERE id = :id")
    suspend fun deleteCommand(id: Long)
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_documents ORDER BY indexedAt DESC")
    fun getAllDocuments(): Flow<List<KnowledgeDocumentEntity>>

    @Query("SELECT * FROM knowledge_documents ORDER BY indexedAt DESC")
    suspend fun getAllDocumentsList(): List<KnowledgeDocumentEntity>

    @Query("SELECT * FROM knowledge_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): KnowledgeDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: KnowledgeDocumentEntity): Long

    @Query("DELETE FROM knowledge_documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)

    @Query("DELETE FROM knowledge_documents")
    suspend fun clearAllDocuments()

    // Chunks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<KnowledgeChunkEntity>)

    @Query("DELETE FROM knowledge_chunks WHERE documentId = :docId")
    suspend fun deleteChunksByDocId(docId: Long)

    @Query("DELETE FROM knowledge_chunks")
    suspend fun clearAllChunks()

    @Query("SELECT * FROM knowledge_chunks WHERE textSnippet LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchChunks(query: String): List<KnowledgeChunkEntity>

    @Query("SELECT * FROM knowledge_chunks LIMIT 50")
    suspend fun getAllChunks(): List<KnowledgeChunkEntity>
}

@Dao
interface AgentPlanDao {
    @Query("SELECT * FROM agent_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<AgentPlanEntity>>

    @Query("SELECT * FROM agent_plans WHERE requestId = :requestId LIMIT 1")
    suspend fun getPlanByRequestId(requestId: String): AgentPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: AgentPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: AgentPlanEntity)

    @Query("DELETE FROM agent_plans WHERE id = :id")
    suspend fun deletePlan(id: Long)

    @Query("DELETE FROM agent_plans")
    suspend fun clearAllPlans()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE category = :category ORDER BY timestamp DESC")
    fun getLogsByCategory(category: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllLogs()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: Long): Flow<UserEntity?>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Long)

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()

    // Session Management
    @Query("SELECT * FROM user_sessions WHERE sessionId = 'active_session' LIMIT 1")
    fun getActiveSession(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_sessions WHERE sessionId = 'active_session' LIMIT 1")
    suspend fun getActiveSessionDirect(): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: UserSessionEntity)

    @Query("DELETE FROM user_sessions WHERE sessionId = 'active_session'")
    suspend fun clearSession()
}


