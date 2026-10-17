package com.example.data.repository

import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

class MishaRepository(
    private val historyDao: HistoryDao,
    private val memoryDao: MemoryDao,
    private val routineDao: RoutineDao,
    private val securityLogDao: SecurityLogDao,
    private val settingsDao: SettingsDao,
    private val emergencyContactDao: EmergencyContactDao,
    private val sosSettingsDao: SosSettingsDao,
    private val sosEventDao: SosEventDao,
    private val proactiveSettingsDao: ProactiveSettingsDao,
    private val voiceProfileDao: VoiceProfileDao,
    private val customCommandDao: CustomCommandDao,
    private val knowledgeDao: KnowledgeDao,
    private val agentPlanDao: AgentPlanDao,
    private val auditLogDao: AuditLogDao,
    private val userDao: UserDao
) {
    // User & Authentication
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val activeSession: Flow<UserSessionEntity?> = userDao.getActiveSession()

    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    suspend fun getUserById(id: Long): UserEntity? = userDao.getUserById(id)
    fun getUserByIdFlow(id: Long): Flow<UserEntity?> = userDao.getUserByIdFlow(id)
    suspend fun getUserCount(): Int = userDao.getUserCount()
    suspend fun insertUser(user: UserEntity): Long = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun deleteUser(id: Long) = userDao.deleteUser(id)
    suspend fun getActiveSessionDirect(): UserSessionEntity? = userDao.getActiveSessionDirect()
    suspend fun saveSession(session: UserSessionEntity) = userDao.saveSession(session)
    suspend fun clearSession() = userDao.clearSession()

    // History
    val messages: Flow<List<ConversationMessageEntity>> = historyDao.getAllMessages()
    val actions: Flow<List<AssistantActionEntity>> = historyDao.getAllActions()

    suspend fun addMessage(sender: String, text: String, intent: String? = null, requestId: String? = null, citations: String? = null) {
        historyDao.insertMessage(
            ConversationMessageEntity(
                sender = sender,
                message = text,
                intent = intent,
                requestId = requestId,
                sourceCitations = citations
            )
        )
    }

    suspend fun addAction(intent: String, argsJson: String, status: String, summary: String, requestId: String? = null, latencyMs: Long = 0) {
        historyDao.insertAction(
            AssistantActionEntity(
                intent = intent,
                argumentsJson = argsJson,
                status = status,
                resultSummary = summary,
                requestId = requestId,
                latencyMs = latencyMs
            )
        )
    }

    suspend fun deleteMessage(id: Long) = historyDao.deleteMessage(id)
    suspend fun clearHistory() {
        historyDao.clearAllMessages()
        historyDao.clearAllActions()
    }

    // Memory
    val memories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    suspend fun addMemory(category: String, key: String, value: String) {
        memoryDao.insertMemory(MemoryEntity(category = category, key = key, value = value))
    }
    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemory(id)
    suspend fun clearMemories() = memoryDao.clearAllMemories()

    // Routines
    val routines: Flow<List<RoutineEntity>> = routineDao.getAllRoutines()
    suspend fun saveRoutine(routine: RoutineEntity) = routineDao.insertRoutine(routine)
    suspend fun updateRoutine(routine: RoutineEntity) = routineDao.updateRoutine(routine)
    suspend fun deleteRoutine(id: Long) = routineDao.deleteRoutine(id)

    // Security
    val securityLogs: Flow<List<SecurityLogEntity>> = securityLogDao.getAllLogs()
    suspend fun logSecurity(title: String, description: String, severity: String) {
        securityLogDao.insertLog(SecurityLogEntity(title = title, description = description, severity = severity))
    }
    suspend fun clearSecurityLogs() = securityLogDao.clearAllLogs()

    // Settings
    val settings: Flow<AppSettingsEntity?> = settingsDao.getSettings()
    suspend fun saveSettings(settings: AppSettingsEntity) = settingsDao.saveSettings(settings)

    // Emergency Contacts
    val emergencyContacts: Flow<List<EmergencyContactEntity>> = emergencyContactDao.getAllContacts()
    suspend fun getEmergencyContactsList(): List<EmergencyContactEntity> = emergencyContactDao.getAllContactsList()
    suspend fun getActiveEmergencyContacts(): List<EmergencyContactEntity> = emergencyContactDao.getActiveContacts()
    suspend fun addEmergencyContact(contact: EmergencyContactEntity): Long = emergencyContactDao.insertContact(contact)
    suspend fun updateEmergencyContact(contact: EmergencyContactEntity) = emergencyContactDao.updateContact(contact)
    suspend fun deleteEmergencyContact(id: Long) = emergencyContactDao.deleteContact(id)
    suspend fun reorderEmergencyContacts(contacts: List<EmergencyContactEntity>) {
        contacts.forEachIndexed { index, contact ->
            emergencyContactDao.updateContact(contact.copy(priority = index + 1))
        }
    }

    // SOS Settings
    val sosSettings: Flow<SosSettingsEntity?> = sosSettingsDao.getSettings()
    suspend fun getSosSettingsDirect(): SosSettingsEntity = sosSettingsDao.getSettingsDirect() ?: SosSettingsEntity()
    suspend fun saveSosSettings(settings: SosSettingsEntity) = sosSettingsDao.saveSettings(settings)

    // SOS Events
    val sosEvents: Flow<List<SosEventEntity>> = sosEventDao.getAllEvents()
    suspend fun recordSosEvent(event: SosEventEntity): Long = sosEventDao.insertEvent(event)
    suspend fun deleteSosEvent(id: Long) = sosEventDao.deleteEvent(id)
    suspend fun clearSosEvents() = sosEventDao.clearAllEvents()

    // Proactive Settings
    val proactiveSettings: Flow<ProactiveSettingsEntity?> = proactiveSettingsDao.getSettings()
    suspend fun getProactiveSettingsDirect(): ProactiveSettingsEntity = proactiveSettingsDao.getSettingsDirect() ?: ProactiveSettingsEntity()
    suspend fun saveProactiveSettings(settings: ProactiveSettingsEntity) = proactiveSettingsDao.saveSettings(settings)

    // Voice Profiles
    val voiceProfiles: Flow<List<VoiceProfileEntity>> = voiceProfileDao.getAllProfiles()
    suspend fun getVoiceProfilesList(): List<VoiceProfileEntity> = voiceProfileDao.getAllProfilesList()
    suspend fun addVoiceProfile(profile: VoiceProfileEntity): Long = voiceProfileDao.insertProfile(profile)
    suspend fun updateVoiceProfile(profile: VoiceProfileEntity) = voiceProfileDao.updateProfile(profile)
    suspend fun deleteVoiceProfile(id: Long) = voiceProfileDao.deleteProfile(id)

    // Custom Commands
    val customCommands: Flow<List<CustomCommandEntity>> = customCommandDao.getAllCommands()
    suspend fun getActiveCustomCommands(): List<CustomCommandEntity> = customCommandDao.getActiveCommands()
    suspend fun saveCustomCommand(cmd: CustomCommandEntity): Long = customCommandDao.insertCommand(cmd)
    suspend fun updateCustomCommand(cmd: CustomCommandEntity) = customCommandDao.updateCommand(cmd)
    suspend fun deleteCustomCommand(id: Long) = customCommandDao.deleteCommand(id)

    // Knowledge Vault
    val knowledgeDocuments: Flow<List<KnowledgeDocumentEntity>> = knowledgeDao.getAllDocuments()
    suspend fun getKnowledgeDocumentsList(): List<KnowledgeDocumentEntity> = knowledgeDao.getAllDocumentsList()
    suspend fun getDocumentById(id: Long): KnowledgeDocumentEntity? = knowledgeDao.getDocumentById(id)
    suspend fun addDocumentWithChunks(doc: KnowledgeDocumentEntity, chunks: List<KnowledgeChunkEntity>): Long {
        val docId = knowledgeDao.insertDocument(doc)
        val chunksWithDocId = chunks.map { it.copy(documentId = docId) }
        knowledgeDao.insertChunks(chunksWithDocId)
        return docId
    }
    suspend fun deleteDocument(id: Long) {
        knowledgeDao.deleteDocument(id)
        knowledgeDao.deleteChunksByDocId(id)
    }
    suspend fun clearKnowledgeVault() {
        knowledgeDao.clearAllDocuments()
        knowledgeDao.clearAllChunks()
    }
    suspend fun searchKnowledge(query: String): List<KnowledgeChunkEntity> = knowledgeDao.searchChunks(query)
    suspend fun getAllChunks(): List<KnowledgeChunkEntity> = knowledgeDao.getAllChunks()

    // Agent Plans
    val agentPlans: Flow<List<AgentPlanEntity>> = agentPlanDao.getAllPlans()
    suspend fun getPlanByRequestId(requestId: String): AgentPlanEntity? = agentPlanDao.getPlanByRequestId(requestId)
    suspend fun saveAgentPlan(plan: AgentPlanEntity): Long = agentPlanDao.insertPlan(plan)
    suspend fun updateAgentPlan(plan: AgentPlanEntity) = agentPlanDao.updatePlan(plan)
    suspend fun clearAgentPlans() = agentPlanDao.clearAllPlans()

    // Audit Logs
    val auditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogs()
    suspend fun logAudit(
        requestId: String,
        category: String,
        commandText: String,
        status: String,
        details: String,
        latencyMs: Long = 0
    ): Long {
        return auditLogDao.insertLog(
            AuditLogEntity(
                requestId = requestId,
                category = category,
                commandText = commandText,
                outcomeStatus = status,
                detailsSummary = details,
                latencyMs = latencyMs
            )
        )
    }
    suspend fun deleteAuditLog(id: Long) = auditLogDao.deleteLog(id)
    suspend fun clearAuditLogs() = auditLogDao.clearAllLogs()
}
