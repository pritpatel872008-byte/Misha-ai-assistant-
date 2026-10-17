package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaViewModel

data class NavTabItem(
    val id: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun MishaApp(viewModel: MishaViewModel) {
    val currentUserSession by viewModel.currentUserSession.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.conversationMessages.collectAsStateWithLifecycle()
    val actions by viewModel.assistantActions.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val emergencyContacts by viewModel.emergencyContacts.collectAsStateWithLifecycle()
    val sosSettings by viewModel.sosSettings.collectAsStateWithLifecycle()
    val sosEvents by viewModel.sosEvents.collectAsStateWithLifecycle()
    val knowledgeDocs by viewModel.knowledgeDocuments.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    var activeScreen by remember { mutableStateOf("home") }

    // If user is not authenticated, show AuthScreen
    if (currentUserSession == null) {
        AuthScreen(viewModel = viewModel)
        return
    }

    val navItems = listOf(
        NavTabItem("home", "Home", Icons.Default.Home),
        NavTabItem("vision_studio", "Vision", Icons.Default.Brush),
        NavTabItem("assistant", "Chat", Icons.Default.GraphicEq),
        NavTabItem("knowledge", "Vault", Icons.Default.AutoStories),
        NavTabItem("sos", "SOS", Icons.Default.Warning),
        NavTabItem("routines", "Routines", Icons.Default.AutoAwesome),
        NavTabItem("security", "Security", Icons.Default.Security),
        NavTabItem("settings", "Settings", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = DarkBg,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            if (activeScreen !in listOf("diagnostics", "voice_test_lab", "privacy_center", "vision_studio")) {
                NavigationBar(
                    containerColor = DarkSurfaceElevated,
                    tonalElevation = 8.dp,
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    navItems.forEach { item ->
                        val selected = activeScreen == item.id
                        NavigationBarItem(
                            selected = selected,
                            onClick = { activeScreen = item.id },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) CyanCore else TextTertiary
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 9.sp,
                                    color = if (selected) CyanCore else TextTertiary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CyanDim.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${item.id}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            when (activeScreen) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    messages = messages,
                    onNavigate = { dest -> activeScreen = dest }
                )
                "assistant" -> AssistantScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    messages = messages
                )
                "knowledge" -> KnowledgeScreen(
                    viewModel = viewModel,
                    documents = knowledgeDocs
                )
                "activity" -> ActivityScreen(
                    viewModel = viewModel,
                    auditLogs = auditLogs,
                    actions = actions
                )
                "sos" -> SosScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    contacts = emergencyContacts,
                    settings = sosSettings,
                    events = sosEvents
                )
                "calls" -> CallsScreen(
                    viewModel = viewModel
                )
                "routines" -> RoutinesScreen(
                    viewModel = viewModel,
                    routines = routines
                )
                "security" -> SecurityScreen(
                    viewModel = viewModel,
                    assessment = uiState.securityAssessment,
                    spamCheckResult = uiState.spamCheckResult
                )
                "history" -> HistoryScreen(
                    viewModel = viewModel,
                    messages = messages,
                    actions = actions
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    settings = settings,
                    onNavigate = { dest -> activeScreen = dest }
                )
                "diagnostics" -> DiagnosticsScreen(
                    uiState = uiState,
                    onBack = { activeScreen = "settings" },
                    onReconnectPython = { viewModel.reconnectPythonBridge() }
                )
                "voice_test_lab" -> VoiceTestLabScreen(
                    onBack = { activeScreen = "settings" }
                )
                "privacy_center" -> PrivacyCenterScreen(
                    viewModel = viewModel,
                    onBack = { activeScreen = "settings" }
                )
                "vision_studio" -> VisionStudioScreen(
                    onBack = { activeScreen = "home" }
                )
            }

            if (uiState.showOnboarding) {
                OnboardingDialog(
                    onDismiss = { viewModel.dismissOnboarding() }
                )
            }
        }
    }
}
