package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.AppSettingsEntity
import com.example.data.local.entities.ProactiveSettingsEntity
import com.example.ui.components.AccountProfileDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaViewModel

@Composable
fun SettingsScreen(
    viewModel: MishaViewModel,
    settings: AppSettingsEntity?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = settings ?: AppSettingsEntity()
    val proactive by viewModel.proactiveSettings.collectAsStateWithLifecycle()
    val currentProactive = proactive ?: ProactiveSettingsEntity()
    val currentUserSession by viewModel.currentUserSession.collectAsStateWithLifecycle()
    var showProfileModal by remember { mutableStateOf(false) }

    if (showProfileModal) {
        AccountProfileDialog(
            userSession = currentUserSession,
            viewModel = viewModel,
            onDismiss = { showProfileModal = false },
            onLogoutRequested = { viewModel.logout() }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "SETTINGS & SYSTEM",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = TextPrimary
            )
            Text(
                text = "Voice Pipeline, Proactive Engine & Intelligence Hub",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account & Security Card
            currentUserSession?.let { user ->
                GlassCard(
                    borderColor = CyanCore.copy(alpha = 0.5f),
                    backgroundColor = DarkSurfaceElevated,
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CyanCore.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.displayName.take(2).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    color = CyanCore,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = user.email,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { showProfileModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanDim.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("settings_manage_account_button")
                        ) {
                            Text("Manage", fontSize = 11.sp, color = CyanCore, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Navigation Shortcuts to Sub-Dashboards
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "INTELLIGENCE DASHBOARDS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = CyanCore
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onNavigate("voice_test_lab") },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanDim.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f).testTag("settings_voice_lab_button")
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanCore)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice Lab", fontSize = 11.sp, color = CyanCore, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onNavigate("privacy_center") },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleGlow.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f).testTag("settings_privacy_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = PurpleGlow)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Privacy", fontSize = 11.sp, color = PurpleGlow, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onNavigate("knowledge") },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberCore.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f).testTag("settings_knowledge_button")
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberCore)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vault (RAG)", fontSize = 11.sp, color = AmberCore, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onNavigate("diagnostics") },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight),
                        modifier = Modifier.weight(1f).testTag("settings_diagnostics_button")
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Telemetry", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Proactive Intelligence Section
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PROACTIVE INTELLIGENCE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = CyanCore
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Proactive Assistant", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Monitors battery, routines & readiness in background", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = currentProactive.proactiveAssistantEnabled,
                        onCheckedChange = { viewModel.saveProactiveSettings(currentProactive.copy(proactiveAssistantEnabled = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Low Battery & Charging Alerts", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Voice alert at <= 15% and full charge", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = currentProactive.lowBatteryAlerts,
                        onCheckedChange = { viewModel.saveProactiveSettings(currentProactive.copy(lowBatteryAlerts = it, chargingCompletedAlerts = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Quiet Hours (22:00 - 07:00)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Mutes non-critical proactive announcements", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = currentProactive.quietHoursEnabled,
                        onCheckedChange = { viewModel.saveProactiveSettings(currentProactive.copy(quietHoursEnabled = it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Detection Section
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "VOICE & ACOUSTICS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = CyanCore
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wake Word (\"Misha\")", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Listens continuously in background", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = current.wakeWordEnabled,
                        onCheckedChange = { viewModel.updateSettings(current.copy(wakeWordEnabled = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Double Clap Detection", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Acoustic transient trigger", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = current.doubleClapEnabled,
                        onCheckedChange = { viewModel.updateSettings(current.copy(doubleClapEnabled = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Wake Sensitivity: ${current.wakeSensitivity}", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LOW", "MEDIUM", "HIGH").forEach { sens ->
                        FilterChip(
                            selected = current.wakeSensitivity == sens,
                            onClick = { viewModel.updateSettings(current.copy(wakeSensitivity = sens)) },
                            label = { Text(sens, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Conversation Follow-Up Window
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CONVERSATIONAL FOLLOW-UP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = CyanCore
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Keep listening for contextual follow-up questions after Misha speaks:", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("OFF" to 0, "5s" to 5, "10s" to 10, "20s" to 20).forEach { (label, timeout) ->
                        val isSelected = if (timeout == 0) !current.followUpModeEnabled else (current.followUpModeEnabled && current.conversationTimeoutSeconds == timeout)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateSettings(
                                    current.copy(
                                        followUpModeEnabled = timeout > 0,
                                        conversationTimeoutSeconds = if (timeout > 0) timeout else current.conversationTimeoutSeconds
                                    )
                                )
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emergency SOS Shortcut
            NeonButton(
                text = "Emergency SOS & Trusted Contacts",
                icon = Icons.Default.Warning,
                glowColor = NeonCrimson,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigate("sos") }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
