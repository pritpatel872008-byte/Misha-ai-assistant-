package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AssistantActionEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityScreen(
    viewModel: MishaViewModel,
    auditLogs: List<AuditLogEntity>,
    actions: List<AssistantActionEntity>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filterCategories = listOf("ALL", "SOS", "TELEPHONY", "AUTOMATION", "PROACTIVE", "GENERAL")

    val filteredLogs = remember(auditLogs, selectedFilter) {
        if (selectedFilter == "ALL") {
            auditLogs
        } else {
            auditLogs.filter { it.category.equals(selectedFilter, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
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
                        text = "ACTIVITY & AUDIT",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Correlated Execution Logs & Tool Outcomes",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                if (auditLogs.isNotEmpty() || actions.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.clearAuditLogs()
                            viewModel.clearHistory()
                        },
                        modifier = Modifier.testTag("clear_activity_button")
                    ) {
                        Text("Clear All", color = TextTertiary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterCategories) { cat ->
                    val isSelected = selectedFilter == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = cat },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanDim.copy(alpha = 0.4f),
                            selectedLabelColor = CyanCore,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = DarkSurfaceHighlight,
                            selectedBorderColor = CyanCore
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics overview bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text("TOTAL AUDITS", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${auditLogs.size}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }

                val successCount = auditLogs.count { it.outcomeStatus.contains("SUCCESS", ignoreCase = true) }
                val successRate = if (auditLogs.isNotEmpty()) (successCount * 100) / auditLogs.size else 100
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text("SUCCESS RATE", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$successRate%", color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "LOGGED EXECUTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = TextTertiary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredLogs.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Logged Activity", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Voice commands and assistant actions will be recorded here.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(filteredLogs) { log ->
                AuditLogRow(log = log)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AuditLogRow(log: AuditLogEntity) {
    val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    val isSuccess = log.outcomeStatus.contains("SUCCESS", ignoreCase = true)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkSurfaceElevated.copy(alpha = 0.9f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSuccess) NeonGreen else CoralAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.category,
                        fontWeight = FontWeight.Bold,
                        color = CyanCore,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "${log.requestId.takeLast(9)} • $dateStr",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.commandText,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            if (log.detailsSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.detailsSummary,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (log.latencyMs > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Latency: ${log.latencyMs}ms | Status: ${log.outcomeStatus}",
                    fontSize = 9.sp,
                    color = TextTertiary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
