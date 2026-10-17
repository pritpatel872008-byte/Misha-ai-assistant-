package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.ConversationMessageEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaUiState
import com.example.ui.viewmodel.MishaViewModel
import com.example.voice.MishaState

@Composable
fun HomeScreen(
    viewModel: MishaViewModel,
    uiState: MishaUiState,
    messages: List<ConversationMessageEntity>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUserSession by viewModel.currentUserSession.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        AccountProfileDialog(
            userSession = currentUserSession,
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false },
            onLogoutRequested = { viewModel.logout() }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF120D24), DarkBg, Color(0xFF080B18))))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Misha AI",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Hey Prit, your personal AI companion",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AccountProfileBadge(
                        userSession = currentUserSession,
                        onOpenProfile = { showProfileDialog = true }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onNavigate("sos") },
                        modifier = Modifier.testTag("home_header_sos_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency SOS",
                            tint = NeonCrimson
                        )
                    }
                    IconButton(
                        onClick = { onNavigate("diagnostics") },
                        modifier = Modifier.testTag("diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Diagnostics",
                            tint = CyanCore
                        )
                    }
                }
            }

            // Emergency Active SOS Countdown / Dispatched Alert Banner
            if (uiState.sosDeliveryState == com.example.ai.SosDeliveryState.SOS_COUNTDOWN) {
                Spacer(modifier = Modifier.height(12.dp))
                GlassCard(
                    borderColor = NeonCrimson,
                    backgroundColor = NeonCrimson.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NeonCrimson),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${uiState.sosCountdownSeconds ?: 0}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "EMERGENCY SOS COUNTDOWN",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Sending emergency SMS in ${uiState.sosCountdownSeconds}s...",
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.cancelActiveSos() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("ABORT", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Glass hero panel inspired by the supplied reference.
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = PurpleGlow.copy(alpha = 0.42f),
                backgroundColor = Color(0x332A174B),
                cornerRadius = 28.dp
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YOUR AI COMPANION", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp, color = CyanGlow)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Misha AI\nIn Your Pocket", fontSize = 25.sp, lineHeight = 29.sp,
                            fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Talk naturally. Get things done.", fontSize = 12.sp, color = TextSecondary)
                    }
                    Box(modifier = Modifier.padding(start = 8.dp).size(58.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PurpleCore.copy(alpha = 0.85f), CyanCore.copy(alpha = 0.55f)))),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Misha AI", tint = Color.White, modifier = Modifier.size(29.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3D Misha Core Visualizer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Misha3DCore(
                    state = uiState.mishaState,
                    rmsLevel = uiState.currentRms,
                    qualityMode = uiState.graphicsMode,
                    size = 230.dp
                )
            }

            // Status Badges (Assistant State & Python Engine Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MishaStatusBadge(state = uiState.mishaState)
                Spacer(modifier = Modifier.width(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.isPythonConnected) NeonEmerald.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        if (uiState.isPythonConnected) NeonEmerald.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isPythonConnected) NeonEmerald else NeonAmber)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (uiState.isPythonConnected) "Python Engine" else "Offline Fallback",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.isPythonConnected) NeonEmerald else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Waveform
            VoiceWaveform(
                rms = uiState.currentRms,
                isListening = uiState.mishaState == MishaState.COMMAND_LISTENING || uiState.mishaState == MishaState.WAKE_DETECTED
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Talk to Misha Big Action Button
            NeonButton(
                text = if (uiState.mishaState == MishaState.COMMAND_LISTENING) "Listening... (Tap to Stop)" else "Talk to Misha",
                icon = if (uiState.mishaState == MishaState.COMMAND_LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                glowColor = if (uiState.mishaState == MishaState.COMMAND_LISTENING) NeonEmerald else CyanCore,
                modifier = Modifier.fillMaxWidth(),
                testTag = "talk_to_misha_button",
                onClick = {
                    if (uiState.mishaState == MishaState.COMMAND_LISTENING) {
                        viewModel.stopListeningCommand()
                    } else {
                        viewModel.startListeningCommand()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirmation Box if active
            AnimatedVisibility(
                visible = uiState.confirmationPrompt != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                GlassCard(
                    borderColor = NeonAmber,
                    backgroundColor = DarkSurfaceHighlight.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = NeonAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirmation Required",
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.confirmationPrompt ?: "",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.confirmPendingAction(false) }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.confirmPendingAction(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                        ) {
                            Text("Proceed", color = DarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Live Speech Transcript & Response Card
            if (uiState.liveTranscript.isNotBlank() || uiState.assistantResponse.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    if (uiState.liveTranscript.isNotBlank()) {
                        Text(
                            text = "YOU: ${uiState.liveTranscript}",
                            color = CyanGlow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (uiState.assistantResponse.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MISHA: ${uiState.assistantResponse}",
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Action Grid Header
            Text(
                text = "DEVICE & AI CONTROLS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Grid of quick tools
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    title = "Vision Studio",
                    subtitle = "Local AI Image Lab",
                    icon = Icons.Default.Brush,
                    color = CyanCore,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("vision_studio") }
                )
                QuickActionChip(
                    title = "Emergency SOS",
                    subtitle = "Automated SMS",
                    icon = Icons.Default.Warning,
                    color = NeonCrimson,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("sos") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    title = "WhatsApp",
                    subtitle = "Verified send",
                    icon = Icons.Default.Chat,
                    color = NeonEmerald,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("assistant") }
                )
                QuickActionChip(
                    title = "Calls",
                    subtitle = "Telecom voice",
                    icon = Icons.Default.Call,
                    color = PurpleGlow,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("calls") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    title = "Lock Device",
                    subtitle = "Secure phone",
                    icon = Icons.Default.Lock,
                    color = NeonCrimson,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.processSpokenText("lock device") }
                )
                QuickActionChip(
                    title = "Security",
                    subtitle = "System score",
                    icon = Icons.Default.Security,
                    color = CyanGlow,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("security") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Activity Section
            Text(
                text = "RECENT CONVERSATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        if (messages.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "No recent activity. Say \"Misha flashlight on\" or tap 'Talk to Misha' to start.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(messages.take(4)) { msg ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (msg.sender == "USER") "You" else "Misha",
                            fontWeight = FontWeight.Bold,
                            color = if (msg.sender == "USER") CyanCore else PurpleGlow,
                            fontSize = 13.sp
                        )
                        if (msg.intent != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceHighlight
                            ) {
                                Text(
                                    text = msg.intent,
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg.message,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun QuickActionChip(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        borderColor = color.copy(alpha = 0.35f),
        backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
