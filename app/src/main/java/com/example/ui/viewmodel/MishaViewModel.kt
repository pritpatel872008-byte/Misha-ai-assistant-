package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MishaApplication
import com.example.ai.*
import com.example.data.local.entities.*
import com.example.memory.KnowledgeQueryResult
import com.example.memory.KnowledgeVaultManager
import com.example.memory.MemoryManager
import com.example.routines.RoutineManager
import com.example.security.AuthManager
import com.example.security.SecurityAssessment
import com.example.security.SecurityScanner
import com.example.security.SensitiveActionGate
import com.example.service.MishaAssistantService
import com.example.service.ProactiveIntelligenceManager
import com.example.tools.*
import com.example.voice.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MishaUiState(
    val mishaState: MishaState = MishaState.STANDBY,
    val liveTranscript: String = "",
    val assistantResponse: String = "",
    val activeIntent: String = "",
    val currentRms: Float = 0f,
    val wakeConfidence: Float = 0f,
    val isMicActive: Boolean = false,
    val confirmationPrompt: String? = null,
    val pendingIntentForConfirmation: MishaParsedIntent? = null,
    val lastExecutionSummary: String = "",
    val securityAssessment: SecurityAssessment? = null,
    val spamCheckResult: Pair<String, String>? = null,
    val currentTab: String = "home",
    val showOnboarding: Boolean = false,
    val diagnosticsLogs: List<String> = emptyList(),
    val sosDeliveryState: SosDeliveryState = SosDeliveryState.SOS_IDLE,
    val sosCountdownSeconds: Int? = null,
    val lastSosResult: SosDispatchResult? = null,
    val showSosConfirmationDialog: Boolean = false,
    val activePlan: PlanGenerationResult? = null,
    val lastKnowledgeQueryResult: KnowledgeQueryResult? = null,
    val activeVoiceProfileName: String = "Default Owner",
    val pythonBridgeState: PythonBridgeState = PythonBridgeState.DISCONNECTED,
    val isPythonConnected: Boolean = false,
    val graphicsMode: String = "HIGH"
)

class MishaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MishaApplication
    private val repository = app.repository

    private val deviceController = DeviceController(application)
    private val telecomCallManager = TelecomCallManager(application)
    private val smsManagerHelper = SmsManagerHelper(application)
    private val emergencySosManager = EmergencySosManager(application, repository)
    private val weatherHelper = WeatherHelper()
    private val webSearchHelper = WebSearchHelper()
    private val securityScanner = SecurityScanner(application)
    private val routineManager = RoutineManager(repository, deviceController, weatherHelper)
    private val memoryManager = MemoryManager(repository)
    private val geminiEngine = GeminiIntentEngine()
    val permissionInspector = com.example.security.PermissionInspector(application)

    // V4 Architecture Subsystems
    val knowledgeVaultManager = KnowledgeVaultManager(application, repository)
    val agentPlanner = AgentPlanner(repository)
    val actionVerificationEngine = ActionVerificationEngine(application)
    val contextConversationManager = ContextConversationManager()
    val sensitiveActionGate = SensitiveActionGate(application, repository)
    val visionEngine = VisionEngine(application)
    val screenUnderstandingEngine = ScreenUnderstandingEngine(application)
    val authManager = AuthManager(repository)
    private val proactiveIntelligenceManager = ProactiveIntelligenceManager(application, repository, viewModelScope)

    private var countdownJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(MishaUiState())
    val uiState: StateFlow<MishaUiState> = _uiState.asStateFlow()

    private lateinit var pythonVoiceBridge: PythonVoiceBridge

    // Auth & Session Streams
    val currentUserSession = repository.activeSession.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val allUsers = repository.allUsers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val conversationMessages = repository.messages.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val assistantActions = repository.actions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val routines = repository.routines.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val securityLogs = repository.securityLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val memories = repository.memories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val appSettings = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettingsEntity()
    )

    val emergencyContacts = repository.emergencyContacts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val sosSettings = repository.sosSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SosSettingsEntity()
    )

    val sosEvents = repository.sosEvents.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // V4 Data Streams
    val knowledgeDocuments = repository.knowledgeDocuments.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val proactiveSettings = repository.proactiveSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProactiveSettingsEntity()
    )

    val voiceProfiles = repository.voiceProfiles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val customCommands = repository.customCommands.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val agentPlans = repository.agentPlans.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val auditLogs = repository.auditLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Voice Components
    private lateinit var wakeWordDetector: WakeWordDetector
    private lateinit var clapDetector: ClapDetector
    private lateinit var echoPreventionManager: EchoPreventionManager
    private lateinit var speechRecognizerBridge: SpeechRecognizerBridge

    init {
        initVoiceEngines()
        observeSettings()
        observeWhatsAppAutomation()
        refreshSecurityAudit()
        proactiveIntelligenceManager.startMonitoring()

        viewModelScope.launch {
            authManager.seedInitialUserIfEmpty()
            routineManager.initializeDefaultRoutines()
            initDefaultEmergencyContacts()
            initDefaultKnowledgeIfEmpty()
        }
    }

    private suspend fun initDefaultKnowledgeIfEmpty() {
        val docs = repository.getKnowledgeDocumentsList()
        if (docs.isEmpty()) {
            knowledgeVaultManager.ingestDocument(
                title = "Software Engineering Notes",
                content = "COCOMO Model: Basic COCOMO Formula is E = a * (KLOC)^b, where E is effort in person-months. Software engineering principles prioritize modularity, cohesion, loose coupling, and verification.",
                category = "Study Notes"
            )
            knowledgeVaultManager.ingestDocument(
                title = "Home Wi-Fi & Gateway Setup",
                content = "Primary Home SSID: Orbit_5G. Gateway IP is 192.168.1.1. DNS configured to 8.8.8.8 and 1.1.1.1 for low latency.",
                category = "Personal Reference"
            )
        }
    }

    private suspend fun initDefaultEmergencyContacts() {
        val list = repository.getEmergencyContactsList()
        if (list.isEmpty()) {
            repository.addEmergencyContact(
                EmergencyContactEntity(
                    name = "Prit (Primary)",
                    phoneNumber = "+919876543210",
                    priority = 1,
                    relationship = "Emergency Contact"
                )
            )
            repository.addEmergencyContact(
                EmergencyContactEntity(
                    name = "Mom",
                    phoneNumber = "+919876543211",
                    priority = 2,
                    relationship = "Family"
                )
            )
            repository.addEmergencyContact(
                EmergencyContactEntity(
                    name = "Dad",
                    phoneNumber = "+919876543212",
                    priority = 3,
                    relationship = "Family"
                )
            )
        }
    }

    private fun initVoiceEngines() {
        echoPreventionManager = EchoPreventionManager { newState ->
            _uiState.update { it.copy(mishaState = newState) }
        }

        app.tts.setCallbacks(
            onStart = {
                echoPreventionManager.onSpeechStart()
                logDiag("TTS: Started speaking")
            },
            onDone = {
                echoPreventionManager.onSpeechFinish()
                logDiag("TTS: Finished speaking")
            },
            onError = {
                echoPreventionManager.onSpeechError()
                logDiag("TTS: Error speaking")
            }
        )

        wakeWordDetector = WakeWordDetector { confidence ->
            if (!echoPreventionManager.isSpeaking) {
                val reqId = MishaEventBus.newRequestId()
                MishaEventBus.tryEmit(MishaEvent.WakeDetected(reqId, confidence))
                logDiag("WAKE: 'Misha' detected (confidence=${String.format("%.2f", confidence)})")
                _uiState.update {
                    it.copy(
                        mishaState = MishaState.WAKE_DETECTED,
                        wakeConfidence = confidence
                    )
                }
                app.tts.speak("Yes?")
                viewModelScope.launch {
                    kotlinx.coroutines.delay(600)
                    startListeningCommand()
                }
            }
        }

        clapDetector = ClapDetector {
            if (!echoPreventionManager.isSpeaking) {
                logDiag("CLAP: Double clap detected")
                _uiState.update { it.copy(mishaState = MishaState.WAKE_DETECTED) }
                app.tts.speak("Listening")
                viewModelScope.launch {
                    kotlinx.coroutines.delay(600)
                    startListeningCommand()
                }
            }
        }

        pythonVoiceBridge = PythonVoiceBridge(
            context = getApplication(),
            onWakeWordDetected = { confidence ->
                if (!echoPreventionManager.isSpeaking) {
                    val reqId = MishaEventBus.newRequestId()
                    MishaEventBus.tryEmit(MishaEvent.WakeDetected(reqId, confidence))
                    logDiag("PYTHON WAKE: 'Misha' detected (confidence=${String.format("%.2f", confidence)})")
                    wakeWordDetector.notifyPythonWakeDetected(confidence)
                    _uiState.update {
                        it.copy(
                            mishaState = MishaState.WAKE_DETECTED,
                            wakeConfidence = confidence
                        )
                    }
                    app.tts.speak("Yes?")
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(600)
                        startListeningCommand()
                    }
                }
            },
            onTranscriptReceived = { text, isFinal ->
                if (isFinal) {
                    _uiState.update {
                        it.copy(liveTranscript = text, mishaState = MishaState.PROCESSING)
                    }
                    processSpokenText(text)
                } else {
                    _uiState.update { it.copy(liveTranscript = text) }
                }
            },
            onIntentReceived = { parsed ->
                viewModelScope.launch {
                    executeIntent(MishaEventBus.newRequestId(), parsed)
                }
            },
            onStateUpdated = { newState ->
                _uiState.update { it.copy(mishaState = newState) }
            }
        )

        viewModelScope.launch {
            pythonVoiceBridge.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        pythonBridgeState = state,
                        isPythonConnected = state == PythonBridgeState.CONNECTED
                    )
                }
                logDiag("PYTHON BRIDGE: ${state.displayName}")
            }
        }

        // Attach listeners to Authoritative MicCaptureEngine
        MicCaptureEngine.instance.addListener(wakeWordDetector)
        MicCaptureEngine.instance.addListener(clapDetector)
        MicCaptureEngine.instance.addListener(pythonVoiceBridge)
        MicCaptureEngine.instance.addListener(object : MicCaptureEngine.AudioFrameListener {
            override fun onAudioFrame(buffer: ShortArray, readSize: Int, rms: Float) {
                _uiState.update { it.copy(currentRms = rms) }
            }
        })

        // Attempt connecting to Python engine (auto-fallback if offline)
        pythonVoiceBridge.connect()

        speechRecognizerBridge = SpeechRecognizerBridge(
            context = getApplication(),
            onPartialResult = { partial ->
                _uiState.update { it.copy(liveTranscript = partial) }
            },
            onFinalResult = { finalTranscript ->
                _uiState.update {
                    it.copy(
                        liveTranscript = finalTranscript,
                        mishaState = MishaState.PROCESSING
                    )
                }
                processSpokenText(finalTranscript)
            },
            onError = { errMsg ->
                logDiag("STT Error: $errMsg")
                _uiState.update {
                    it.copy(
                        mishaState = MishaState.STANDBY,
                        assistantResponse = errMsg
                    )
                }
            },
            onRmsChanged = { rms ->
                _uiState.update { it.copy(currentRms = rms * 20f) }
            },
            onListeningReady = {
                _uiState.update {
                    it.copy(
                        mishaState = MishaState.COMMAND_LISTENING,
                        liveTranscript = "Speak now…"
                    )
                }
            }
        )
    }

    private fun observeSettings() {
        viewModelScope.launch {
            appSettings.collect { settings ->
                if (settings != null) {
                    wakeWordDetector.isEnabled = settings.wakeWordEnabled
                    wakeWordDetector.sensitivity = settings.wakeSensitivity
                    clapDetector.isEnabled = settings.doubleClapEnabled
                    clapDetector.sensitivity = settings.wakeSensitivity
                    app.tts.pitch = settings.ttsPitch
                    app.tts.speed = settings.ttsSpeed
                    _uiState.update { it.copy(graphicsMode = settings.performance3DMode) }
                }
            }
        }
    }

    private fun observeWhatsAppAutomation() {
        viewModelScope.launch {
            MishaAccessibilityService.automationResults.collect { result ->
                handleToolExecutionResult(MishaEventBus.newRequestId(), IntentConstants.SEND_WHATSAPP, emptyMap(), result)
            }
        }
    }

    fun startListeningCommand() {
        if (echoPreventionManager.isSpeaking) return
        _uiState.update {
            it.copy(
                mishaState = MishaState.STANDBY,
                liveTranscript = "Starting microphone…",
                assistantResponse = ""
            )
        }
        speechRecognizerBridge.startListening("en-IN")
    }

    fun stopListeningCommand() {
        speechRecognizerBridge.stopListening()
        _uiState.update { it.copy(mishaState = MishaState.STANDBY) }
    }

    fun processSpokenText(text: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(mishaState = MishaState.STANDBY) }
            return
        }

        viewModelScope.launch {
            val requestId = MishaEventBus.newRequestId()
            repository.addMessage(sender = "USER", text = text, requestId = requestId)
            MishaEventBus.emit(MishaEvent.TranscriptFinal(requestId, text))
            logDiag("STT FINAL: \"$text\" (Req: ${requestId.takeLast(8)})")

            _uiState.update {
                it.copy(
                    mishaState = MishaState.PROCESSING,
                    liveTranscript = text
                )
            }

            // Check if contextual follow-up applies
            val contextualFollowUp = contextConversationManager.resolveContextualFollowUp(text)
            val parsedIntent = contextualFollowUp ?: geminiEngine.parseUserCommand(text)
            contextConversationManager.updateContext(parsedIntent)

            MishaEventBus.emit(MishaEvent.IntentDetected(requestId, parsedIntent.intent, parsedIntent.arguments))
            logDiag("INTENT: ${parsedIntent.intent} args=${parsedIntent.arguments}")

            if (parsedIntent.requires_confirmation && appSettings.value?.confirmationMode != "NEVER") {
                _uiState.update {
                    it.copy(
                        mishaState = MishaState.STANDBY,
                        confirmationPrompt = parsedIntent.confirmation_prompt ?: "Confirm action?",
                        pendingIntentForConfirmation = parsedIntent,
                        assistantResponse = parsedIntent.spoken_response
                    )
                }
                app.tts.speak(parsedIntent.spoken_response)
            } else {
                executeIntent(requestId, parsedIntent)
            }
        }
    }

    fun confirmPendingAction(confirmed: Boolean) {
        val pending = _uiState.value.pendingIntentForConfirmation
        _uiState.update {
            it.copy(
                confirmationPrompt = null,
                pendingIntentForConfirmation = null
            )
        }

        if (confirmed && pending != null) {
            viewModelScope.launch {
                executeIntent(MishaEventBus.newRequestId(), pending)
            }
        } else {
            app.tts.speak("Action cancelled.")
            _uiState.update {
                it.copy(
                    mishaState = MishaState.STANDBY,
                    assistantResponse = "Action cancelled."
                )
            }
        }
    }

    private suspend fun executeIntent(requestId: String, parsed: MishaParsedIntent) {
        val startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                mishaState = MishaState.EXECUTING,
                activeIntent = parsed.intent
            )
        }

        val result: ToolExecutionResult = when (parsed.intent) {
            IntentConstants.TRIGGER_SOS -> {
                val currentSosSettings = repository.getSosSettingsDirect()
                if (currentSosSettings.requireVoiceConfirmation && !currentSosSettings.isOneTapEnabled) {
                    _uiState.update {
                        it.copy(
                            mishaState = MishaState.STANDBY,
                            confirmationPrompt = "Emergency SOS bhejne wala hoon. Confirm?",
                            pendingIntentForConfirmation = MishaParsedIntent(
                                intent = IntentConstants.CONFIRM_SOS,
                                spoken_response = "Sending SOS."
                            ),
                            assistantResponse = "Emergency mode activated. Do you want me to send an SOS?"
                        )
                    }
                    app.tts.speak("Emergency SOS bhejne wala hoon. Confirm?")
                    return
                } else {
                    triggerEmergencySos(isTest = false)
                    ToolExecutionResult(
                        status = ToolStatus.SUCCESS,
                        message = "Emergency SOS triggered.",
                        spokenResponse = "Emergency SOS triggered."
                    )
                }
            }
            IntentConstants.CONFIRM_SOS -> {
                confirmEmergencySos()
                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = "Sending SOS.",
                    spokenResponse = "Sending SOS."
                )
            }
            IntentConstants.TEST_SOS -> {
                sendTestSos()
                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = "Initiating Test SOS.",
                    spokenResponse = "Initiating Test SOS."
                )
            }
            IntentConstants.CANCEL_SOS -> {
                cancelActiveSos()
                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = "Emergency SOS cancelled.",
                    spokenResponse = "Emergency SOS cancelled."
                )
            }

            // V4 RAG Knowledge Vault Intent
            IntentConstants.KNOWLEDGE_QUERY -> {
                val query = parsed.arguments["query"] ?: parsed.arguments["q"] ?: _uiState.value.liveTranscript
                val ragResult = knowledgeVaultManager.searchAndRetrieve(query)
                _uiState.update { it.copy(lastKnowledgeQueryResult = ragResult) }
                if (ragResult.hasMatch) {
                    val spoken = "Aapki notes ke mutabik: ${ragResult.matchedChunks.first().textSnippet.take(120)}"
                    ToolExecutionResult(
                        status = ToolStatus.SUCCESS,
                        message = "Found in vault: ${ragResult.sourceCitations.joinToString(", ")}\n\n${ragResult.concatenatedContext}",
                        spokenResponse = spoken
                    )
                } else {
                    ToolExecutionResult(
                        status = ToolStatus.SUCCESS,
                        message = "No relevant documents found in Knowledge Vault for \"$query\".",
                        spokenResponse = "Mujhe aapki notes mein is bare mein koi match nahi mila."
                    )
                }
            }

            // V4 Agent Plan Intent
            IntentConstants.AGENT_PLAN -> {
                val goal = parsed.arguments["goal"] ?: _uiState.value.liveTranscript
                val plan = agentPlanner.createPlan(goal, requestId)
                _uiState.update { it.copy(activePlan = plan) }
                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = "Generated Plan: ${plan.summary} (${plan.steps.size} steps)",
                    spokenResponse = "Maine ${plan.steps.size} steps ka plan create kar diya hai. Ready to execute."
                )
            }

            // V4 Screen Understand Intent
            IntentConstants.SCREEN_UNDERSTAND -> {
                val inspection = screenUnderstandingEngine.inspectCurrentScreen()
                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = inspection.summary,
                    spokenResponse = if (inspection.isAccessibilityEnabled) "Screen analysis complete." else "Accessibility service enable kijiye."
                )
            }

            IntentConstants.FLASHLIGHT_ON -> deviceController.toggleFlashlight(true)
            IntentConstants.FLASHLIGHT_OFF -> deviceController.toggleFlashlight(false)
            IntentConstants.VOLUME_UP -> deviceController.adjustVolume(true)
            IntentConstants.VOLUME_DOWN -> deviceController.adjustVolume(false)
            IntentConstants.BATTERY_STATUS -> deviceController.getBatteryInfo()
            IntentConstants.DEVICE_INFO -> deviceController.getDeviceInfo()
            IntentConstants.LOCK_DEVICE -> deviceController.lockDevice()
            IntentConstants.OPEN_SETTINGS -> deviceController.openSettingsScreen()
            IntentConstants.OPEN_APP -> deviceController.launchAppByName(parsed.arguments["app_name"] ?: "")
            IntentConstants.MAKE_CALL -> telecomCallManager.makeCall(parsed.arguments["recipient"] ?: parsed.arguments["number"] ?: "")
            IntentConstants.SEND_SMS -> smsManagerHelper.sendSms(
                parsed.arguments["recipient"] ?: "",
                parsed.arguments["message"] ?: ""
            )
            IntentConstants.SEND_WHATSAPP -> MishaAccessibilityService.executeWhatsAppMessage(
                context = getApplication(),
                recipient = parsed.arguments["recipient"] ?: "",
                message = parsed.arguments["message"] ?: ""
            )
            IntentConstants.WEATHER -> weatherHelper.getWeather(parsed.arguments["location"] ?: "Ahmedabad")
            IntentConstants.WEB_SEARCH -> webSearchHelper.performSearch(parsed.arguments["query"] ?: parsed.arguments["q"] ?: "")
            IntentConstants.STOP, IntentConstants.CANCEL -> {
                cancelActiveSos()
                app.tts.stop()
                ToolExecutionResult(ToolStatus.SUCCESS, "Stopped.", "Stopped.")
            }
            else -> {
                // GENERAL_AI
                ToolExecutionResult(
                    status = ToolStatus.SUCCESS,
                    message = parsed.spoken_response,
                    spokenResponse = parsed.spoken_response
                )
            }
        }

        val latencyMs = System.currentTimeMillis() - startTime
        handleToolExecutionResult(requestId, parsed.intent, parsed.arguments, result, latencyMs)
    }

    // ==========================================
    // EMERGENCY SOS SYSTEM METHODS
    // ==========================================

    fun triggerEmergencySos(isTest: Boolean = false, bypassCountdown: Boolean = false) {
        countdownJob?.cancel()
        val settings = sosSettings.value ?: SosSettingsEntity()

        if (!settings.isSosEnabled && !isTest) {
            val msg = "Emergency SOS is disabled in settings."
            app.tts.speak(msg)
            _uiState.update {
                it.copy(
                    sosDeliveryState = SosDeliveryState.SOS_FAILED,
                    assistantResponse = msg
                )
            }
            return
        }

        val countdownSecs = if (bypassCountdown) 0 else settings.countdownSeconds

        if (countdownSecs > 0) {
            _uiState.update {
                it.copy(
                    sosDeliveryState = SosDeliveryState.SOS_COUNTDOWN,
                    sosCountdownSeconds = countdownSecs
                )
            }
            val prompt = if (isTest) "Test SOS will be sent in $countdownSecs seconds." else "SOS will be sent in $countdownSecs seconds."
            app.tts.speak(prompt)

            countdownJob = viewModelScope.launch {
                for (i in countdownSecs downTo 1) {
                    _uiState.update { it.copy(sosCountdownSeconds = i) }
                    kotlinx.coroutines.delay(1000)
                }
                _uiState.update { it.copy(sosCountdownSeconds = 0) }
                performSosDispatch(isTest)
            }
        } else {
            viewModelScope.launch {
                performSosDispatch(isTest)
            }
        }
    }

    fun confirmEmergencySos() {
        _uiState.update {
            it.copy(
                confirmationPrompt = null,
                pendingIntentForConfirmation = null,
                showSosConfirmationDialog = false
            )
        }
        triggerEmergencySos(isTest = false, bypassCountdown = false)
    }

    fun cancelActiveSos() {
        countdownJob?.cancel()
        countdownJob = null
        val wasActive = _uiState.value.sosDeliveryState == SosDeliveryState.SOS_COUNTDOWN ||
                _uiState.value.confirmationPrompt != null

        _uiState.update {
            it.copy(
                sosDeliveryState = SosDeliveryState.SOS_CANCELLED,
                sosCountdownSeconds = null,
                confirmationPrompt = null,
                pendingIntentForConfirmation = null,
                showSosConfirmationDialog = false,
                assistantResponse = "Emergency SOS cancelled."
            )
        }

        if (wasActive) {
            app.tts.speak("Emergency SOS cancelled.")
            viewModelScope.launch {
                repository.recordSosEvent(
                    SosEventEntity(
                        status = SosDeliveryState.SOS_CANCELLED.name,
                        recipients = "N/A",
                        messageBody = "Aborted by user",
                        deliverySummary = "Emergency SOS cancelled before transmission."
                    )
                )
            }
        }
    }

    private suspend fun performSosDispatch(isTest: Boolean) {
        val reqId = MishaEventBus.newRequestId()
        _uiState.update {
            it.copy(
                sosDeliveryState = SosDeliveryState.SOS_SENDING,
                sosCountdownSeconds = null
            )
        }
        logDiag("SOS: Dispatching payload (isTest=$isTest)")

        val dispatchResult = emergencySosManager.dispatchSos(isTest = isTest)

        _uiState.update {
            it.copy(
                sosDeliveryState = dispatchResult.state,
                lastSosResult = dispatchResult,
                assistantResponse = dispatchResult.message,
                mishaState = MishaState.SPEAKING
            )
        }

        MishaEventBus.emit(MishaEvent.SosEvent(reqId, dispatchResult.state, dispatchResult.message))
        repository.logAudit(
            requestId = reqId,
            category = "SOS",
            commandText = if (isTest) "Test SOS Dispatch" else "Emergency SOS Dispatch",
            status = dispatchResult.state.name,
            details = dispatchResult.details
        )

        app.tts.speak(dispatchResult.spokenResponse)
        logDiag("SOS: Result -> ${dispatchResult.state} (${dispatchResult.message})")
    }

    fun sendTestSos() {
        triggerEmergencySos(isTest = true, bypassCountdown = false)
    }

    fun testSingleEmergencyContact(contact: EmergencyContactEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(sosDeliveryState = SosDeliveryState.SOS_SENDING) }
            val res = emergencySosManager.dispatchSos(
                isTest = true,
                specificContacts = listOf(contact)
            )
            _uiState.update {
                it.copy(
                    sosDeliveryState = res.state,
                    lastSosResult = res,
                    assistantResponse = res.message
                )
            }
            app.tts.speak(res.spokenResponse)
        }
    }

    fun addEmergencyContact(name: String, phoneNumber: String, relationship: String = "Emergency Contact") {
        viewModelScope.launch {
            val count = emergencyContacts.value.size
            repository.addEmergencyContact(
                EmergencyContactEntity(
                    name = name.trim(),
                    phoneNumber = phoneNumber.trim(),
                    priority = count + 1,
                    relationship = relationship.trim()
                )
            )
            logDiag("SOS Contact Added: $name ($phoneNumber)")
        }
    }

    fun updateEmergencyContact(contact: EmergencyContactEntity) {
        viewModelScope.launch {
            repository.updateEmergencyContact(contact)
            logDiag("SOS Contact Updated: ${contact.name}")
        }
    }

    fun deleteEmergencyContact(id: Long) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(id)
            logDiag("SOS Contact Deleted: ID $id")
        }
    }

    fun reorderEmergencyContacts(contacts: List<EmergencyContactEntity>) {
        viewModelScope.launch {
            repository.reorderEmergencyContacts(contacts)
        }
    }

    fun updateSosSettings(settings: SosSettingsEntity) {
        viewModelScope.launch {
            repository.saveSosSettings(settings)
            logDiag("SOS Settings Updated")
        }
    }

    fun clearSosHistory() {
        viewModelScope.launch {
            repository.clearSosEvents()
        }
    }

    fun makeDirectEmergencyCall(number: String = "112") {
        viewModelScope.launch {
            telecomCallManager.makeCall(number)
        }
    }

    private suspend fun handleToolExecutionResult(
        requestId: String,
        intent: String,
        arguments: Map<String, String>,
        result: ToolExecutionResult,
        latencyMs: Long = 0
    ) {
        val verification = actionVerificationEngine.verifyAction(
            intent = intent,
            arguments = arguments,
            rawExecutionSuccess = result.status == ToolStatus.SUCCESS,
            rawErrorMessage = result.message
        )

        val spoken = result.spokenResponse.ifBlank { result.message }

        repository.addMessage(sender = "MISHA", text = result.message, intent = intent, requestId = requestId)
        repository.addAction(intent = intent, argsJson = arguments.toString(), status = verification.outcome.name, summary = result.message, requestId = requestId, latencyMs = latencyMs)

        repository.logAudit(
            requestId = requestId,
            category = mapIntentToCategory(intent),
            commandText = "$intent: ${arguments.values.joinToString(" ")}".trim(),
            status = verification.outcome.name,
            details = verification.verificationDetails,
            latencyMs = latencyMs
        )

        MishaEventBus.emit(
            MishaEvent.ActionOutcome(
                requestId = requestId,
                intent = intent,
                status = verification.outcome.name,
                summary = verification.summary,
                latencyMs = latencyMs
            )
        )

        logDiag("ACTION: $intent -> ${verification.outcome} (${result.message})")

        pythonVoiceBridge.sendActionResult(
            requestId = requestId,
            action = intent,
            status = verification.outcome.name,
            message = result.message,
            spokenFeedback = spoken
        )

        _uiState.update {
            it.copy(
                mishaState = MishaState.SPEAKING,
                assistantResponse = result.message,
                lastExecutionSummary = result.message
            )
        }

        app.tts.speak(spoken)
    }

    // ==========================================
    // V4 KNOWLEDGE VAULT & AGENT PLAN METHODS
    // ==========================================

    fun importKnowledgeDocument(title: String, content: String, category: String = "Notes") {
        viewModelScope.launch {
            val docId = knowledgeVaultManager.ingestDocument(title, content, category = category)
            logDiag("Knowledge Vault: Indexed document #$docId ($title)")
        }
    }

    fun deleteKnowledgeDocument(id: Long) {
        viewModelScope.launch {
            repository.deleteDocument(id)
            logDiag("Knowledge Vault: Deleted document #$id")
        }
    }

    fun clearKnowledgeVault() {
        viewModelScope.launch {
            repository.clearKnowledgeVault()
            logDiag("Knowledge Vault cleared.")
        }
    }

    fun searchKnowledgeVault(query: String, onResult: (List<KnowledgeChunkEntity>) -> Unit) {
        viewModelScope.launch {
            val res = knowledgeVaultManager.searchAndRetrieve(query)
            onResult(res.matchedChunks)
        }
    }

    fun saveProactiveSettings(settings: ProactiveSettingsEntity) {
        viewModelScope.launch {
            repository.saveProactiveSettings(settings)
            logDiag("Proactive Settings updated.")
        }
    }

    fun addVoiceProfile(name: String, isDefault: Boolean = false, requiresPin: Boolean = true) {
        viewModelScope.launch {
            repository.addVoiceProfile(
                VoiceProfileEntity(
                    name = name,
                    isDefault = isDefault,
                    requiresPinForSensitiveActions = requiresPin,
                    isEnabled = true
                )
            )
        }
    }

    fun deleteVoiceProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteVoiceProfile(id)
        }
    }

    fun addCustomCommand(name: String, phrase: String, actionsJson: String, response: String = "Custom command executed.") {
        viewModelScope.launch {
            repository.saveCustomCommand(
                CustomCommandEntity(
                    name = name,
                    triggerPhrase = phrase,
                    actionsJson = actionsJson,
                    customSpokenResponse = response
                )
            )
        }
    }

    fun deleteCustomCommand(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomCommand(id)
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearAuditLogs()
        }
    }

    fun runRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(mishaState = MishaState.EXECUTING) }
            val results = routineManager.executeRoutine(routine)
            val summary = "Executed ${routine.name} (${results.size} actions)"
            app.tts.speak("$summary completed.")
            _uiState.update {
                it.copy(
                    mishaState = MishaState.STANDBY,
                    assistantResponse = summary
                )
            }
        }
    }

    fun toggleRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            repository.updateRoutine(routine.copy(isEnabled = !routine.isEnabled))
        }
    }

    fun refreshSecurityAudit() {
        viewModelScope.launch {
            val assessment = securityScanner.performFullSecurityAudit()
            _uiState.update { it.copy(securityAssessment = assessment) }
            repository.logSecurity(
                "Daily Security Audit",
                "Assessed score: ${assessment.score}/100 - Status: ${assessment.status}",
                if (assessment.score >= 80) "SAFE" else "SUSPICIOUS"
            )
        }
    }

    fun checkSpamNumber(number: String) {
        val result = securityScanner.checkSpamNumber(number)
        _uiState.update { it.copy(spamCheckResult = result) }
    }

    fun updateSettings(newSettings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            repository.clearMemories()
        }
    }

    fun setTab(tab: String) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun dismissOnboarding() {
        _uiState.update { it.copy(showOnboarding = false) }
    }

    fun login(
        email: String,
        pass: String,
        rememberMe: Boolean,
        onResult: (com.example.security.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.login(email, pass, rememberMe)
            onResult(res)
        }
    }

    fun signUp(
        email: String,
        pass: String,
        name: String,
        secQuestion: String,
        secAnswer: String,
        recoveryPin: String,
        onResult: (com.example.security.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.signUp(email, pass, name, secQuestion, secAnswer, recoveryPin)
            onResult(res)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
        }
    }

    fun requestPasswordReset(
        email: String,
        onResult: (com.example.security.PasswordResetResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.requestPasswordReset(email)
            onResult(res)
        }
    }

    fun verifyResetOtp(
        email: String,
        otp: String,
        onResult: (com.example.security.PasswordResetResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.verifyResetOtp(email, otp)
            onResult(res)
        }
    }

    fun completePasswordReset(
        email: String,
        otp: String,
        newPass: String,
        onResult: (com.example.security.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.completePasswordReset(email, otp, newPass)
            onResult(res)
        }
    }

    fun changePassword(
        userId: Long,
        oldPass: String,
        newPass: String,
        onResult: (com.example.security.AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.changePassword(userId, oldPass, newPass)
            onResult(res)
        }
    }

    private fun mapIntentToCategory(intent: String): String {
        return when {
            intent.contains("SOS") -> "SOS"
            intent.contains("CALL") || intent.contains("SMS") || intent.contains("WHATSAPP") -> "TELEPHONY"
            intent.contains("FLASHLIGHT") || intent.contains("VOLUME") || intent.contains("LOCK") || intent.contains("APP") -> "AUTOMATION"
            intent.contains("KNOWLEDGE") -> "KNOWLEDGE"
            else -> "GENERAL"
        }
    }

    private fun logDiag(msg: String) {
        _uiState.update {
            val list = (listOf("${System.currentTimeMillis() % 100000}: $msg") + it.diagnosticsLogs).take(40)
            it.copy(diagnosticsLogs = list)
        }
    }

    fun reconnectPythonBridge() {
        pythonVoiceBridge.connect()
    }

    fun disconnectPythonBridge() {
        pythonVoiceBridge.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        MicCaptureEngine.instance.removeListener(wakeWordDetector)
        MicCaptureEngine.instance.removeListener(clapDetector)
        MicCaptureEngine.instance.removeListener(pythonVoiceBridge)
        pythonVoiceBridge.disconnect()
        speechRecognizerBridge.stopListening()
    }
}

