package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.PermissionAuditItem
import com.example.security.SecurityAssessment
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaViewModel

@Composable
fun SecurityScreen(
    viewModel: MishaViewModel,
    assessment: SecurityAssessment?,
    spamCheckResult: Pair<String, String>?,
    modifier: Modifier = Modifier
) {
    var checkPhoneInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SECURITY & TRUST",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Zero-Deception Device Safety Audit",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshSecurityAudit() },
                    modifier = Modifier.testTag("refresh_security_audit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Audit",
                        tint = CyanCore
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if ((assessment?.score ?: 0) >= 80) NeonEmerald.copy(alpha = 0.5f) else NeonAmber.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(
                                if ((assessment?.score ?: 0) >= 80) NeonEmerald.copy(alpha = 0.2f)
                                else NeonAmber.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${assessment?.score ?: 0}",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = if ((assessment?.score ?: 0) >= 80) NeonEmerald else NeonAmber
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = assessment?.status ?: "ASSESSING...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Verified on-device permission & privilege evaluation",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Spam Number Lookup Tool
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SPAM & TELEMARKETING AUDIT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = CyanCore
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = checkPhoneInput,
                    onValueChange = { checkPhoneInput = it },
                    label = { Text("Enter phone number to verify", color = TextTertiary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanCore,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                NeonButton(
                    text = "Check Number",
                    icon = Icons.Default.Search,
                    glowColor = CyanCore,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (checkPhoneInput.isNotBlank()) {
                            viewModel.checkSpamNumber(checkPhoneInput)
                        }
                    }
                )

                if (spamCheckResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (spamCheckResult.first == "SAFE") NeonEmerald.copy(alpha = 0.15f) else NeonAmber.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Verdict: ${spamCheckResult.first}",
                                fontWeight = FontWeight.Bold,
                                color = if (spamCheckResult.first == "SAFE") NeonEmerald else NeonAmber,
                                fontSize = 13.sp
                            )
                            Text(
                                text = spamCheckResult.second,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SYSTEM PERMISSIONS & REASONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(assessment?.permissionAudits ?: emptyList()) { audit ->
            PermissionAuditRow(audit = audit)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PermissionAuditRow(audit: PermissionAuditItem) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audit.permissionName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = audit.whyNeeded,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (audit.isGranted) NeonEmerald.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (audit.isGranted) "GRANTED" else "PENDING",
                    color = if (audit.isGranted) NeonEmerald else NeonAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
