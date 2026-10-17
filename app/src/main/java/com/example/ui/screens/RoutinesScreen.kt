package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.RoutineEntity
import com.example.routines.RoutineManager
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaViewModel

@Composable
fun RoutinesScreen(
    viewModel: MishaViewModel,
    routines: List<RoutineEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "AUTOMATION ROUTINES",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = TextPrimary
            )
            Text(
                text = "Multi-step Voice & Schedule Workflows",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (routines.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No routines configured.", color = TextTertiary)
                }
            }
        } else {
            items(routines) { routine ->
                RoutineCardItem(
                    routine = routine,
                    onToggle = { viewModel.toggleRoutine(routine) },
                    onRun = { viewModel.runRoutine(routine) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RoutineCardItem(
    routine: RoutineEntity,
    onToggle: () -> Unit,
    onRun: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (routine.isEnabled) CyanCore.copy(alpha = 0.4f) else GlassBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Trigger: \"${routine.triggerPhrase}\"",
                    fontSize = 12.sp,
                    color = CyanGlow
                )
                if (routine.scheduledTime != null) {
                    Text(
                        text = "Schedule: ${routine.scheduledTime}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Switch(
                checked = routine.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkBg,
                    checkedTrackColor = CyanCore,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = DarkSurfaceHighlight
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        NeonButton(
            text = "Run Routine Now",
            icon = Icons.Default.PlayArrow,
            glowColor = CyanCore,
            modifier = Modifier.fillMaxWidth(),
            onClick = onRun
        )
    }
}
