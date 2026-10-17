package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.MishaAssistantService
import com.example.tools.MishaAccessibilityService
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaUiState
import com.example.voice.MicCaptureEngine

@Composable
fun DiagnosticsScreen(
    uiState: MishaUiState,
    onBack: () -> Unit,
    onReconnectPython: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HEALTH & DIAGNOSTICS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Python Voice Core & Subsystem Matrix",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = onReconnectPython,
                modifier = Modifier.testTag("reconnect_python_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reconnect Python Bridge",
                    tint = CyanCore
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subsystems status grid
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "MISHA SUBSYSTEM HEALTH",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = CyanCore
            )
            Spacer(modifier = Modifier.height(8.dp))

            HealthStatusRow("Microphone (Single Owner)", if (MicCaptureEngine.instance.isCapturing) "READY (16kHz Mono)" else "STANDBY", isGood = MicCaptureEngine.instance.isCapturing)
            HealthStatusRow("Python Voice Bridge", uiState.pythonBridgeState.displayName, isGood = uiState.isPythonConnected)
            HealthStatusRow("Wake Word Engine (openWakeWord)", if (uiState.isPythonConnected) "ONLINE (Python Core)" else "FALLBACK READY", isGood = uiState.isPythonConnected)
            HealthStatusRow("VAD Engine (Silero)", if (uiState.isPythonConnected) "ONLINE (Python Core)" else "STANDBY", isGood = uiState.isPythonConnected)
            HealthStatusRow("STT Engine (Vosk/Whisper)", if (uiState.isPythonConnected) "ONLINE (Python Core)" else "OFFLINE FALLBACK", isGood = uiState.isPythonConnected)
            HealthStatusRow("TTS Engine (Piper Neural)", if (uiState.isPythonConnected) "ONLINE (Python Core)" else "LOCAL ANDROID TTS", isGood = uiState.isPythonConnected)
            HealthStatusRow("Foreground Service", if (MishaAssistantService.isServiceRunning) "ACTIVE (Mic Foreground)" else "STOPPED", isGood = MishaAssistantService.isServiceRunning)
            HealthStatusRow("Accessibility Service (WhatsApp)", if (MishaAccessibilityService.isServiceRunning) "ACTIVE" else "INACTIVE", isGood = MishaAccessibilityService.isServiceRunning)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Acoustic Telemetry Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ACOUSTIC TELEMETRY",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = CyanCore
            )
            Spacer(modifier = Modifier.height(8.dp))
            TelemetryRow("Misha State", uiState.mishaState.name)
            TelemetryRow("Mic Input RMS", String.format("%.1f", uiState.currentRms))
            TelemetryRow("Wake Confidence", String.format("%.2f", uiState.wakeConfidence))
            TelemetryRow("Active Voice Profile", uiState.activeVoiceProfileName)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "LIVE EVENT & AUDIT LOG",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = TextTertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkBg,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (uiState.diagnosticsLogs.isEmpty()) {
                    item {
                        Text(
                            text = "Awaiting acoustic and intent events...",
                            color = TextTertiary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    items(uiState.diagnosticsLogs) { log ->
                        Text(
                            text = "> $log",
                            color = CyanGlow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthStatusRow(label: String, value: String, isGood: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isGood) NeonEmerald else NeonAmber)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = TextSecondary, fontSize = 12.sp)
        }
        Text(
            text = value,
            color = if (isGood) NeonEmerald else TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
