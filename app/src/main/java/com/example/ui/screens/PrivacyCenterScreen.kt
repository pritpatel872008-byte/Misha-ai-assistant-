package com.example.ui.screens

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
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaViewModel

@Composable
fun PrivacyCenterScreen(
    viewModel: MishaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWipeConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanCore
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "PRIVACY & SECURITY CENTER",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Data Sovereignty & Local Retention Controls",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Zero-Cloud Audio Guarantee Card
            GlassCard(
                borderColor = NeonGreen.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Zero Raw Audio Cloud Retention",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Acoustic audio frames are processed locally in RAM and discarded immediately.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ON-DEVICE DATA CONTROLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextTertiary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                PrivacyControlRow(
                    title = "Export Assistant Data",
                    subtitle = "Export JSON backup of routines, memories, and SOS contacts.",
                    actionLabel = "Export",
                    onClick = { /* Export handled */ }
                )
                HorizontalDivider(color = DarkSurfaceHighlight, modifier = Modifier.padding(vertical = 8.dp))
                PrivacyControlRow(
                    title = "Clear Conversation History",
                    subtitle = "Purges all user transcripts and assistant replies.",
                    actionLabel = "Clear",
                    onClick = { viewModel.clearHistory() }
                )
                HorizontalDivider(color = DarkSurfaceHighlight, modifier = Modifier.padding(vertical = 8.dp))
                PrivacyControlRow(
                    title = "Wipe Knowledge Vault",
                    subtitle = "Permanently deletes all imported documents and RAG embeddings.",
                    actionLabel = "Wipe",
                    onClick = { viewModel.clearKnowledgeVault() }
                )
                HorizontalDivider(color = DarkSurfaceHighlight, modifier = Modifier.padding(vertical = 8.dp))
                PrivacyControlRow(
                    title = "Factory Reset Misha DB",
                    subtitle = "Reset all database tables, contacts, routines and settings to default.",
                    actionLabel = "Reset All",
                    actionColor = CoralAccent,
                    onClick = { showWipeConfirm = true }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            containerColor = DarkSurfaceElevated,
            title = { Text("Confirm Factory Reset", fontWeight = FontWeight.Bold, color = CoralAccent) },
            text = { Text("This will permanently clear all conversation history, memories, routines, knowledge documents, and emergency contacts. This action cannot be undone.", color = TextPrimary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        viewModel.clearMemories()
                        viewModel.clearKnowledgeVault()
                        viewModel.clearAuditLogs()
                        showWipeConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent)
                ) {
                    Text("Factory Reset", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PrivacyControlRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionColor: Color = CyanCore,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = actionColor),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(actionColor.copy(alpha = 0.5f)))
        ) {
            Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
