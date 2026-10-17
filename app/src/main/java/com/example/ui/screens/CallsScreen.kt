package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaViewModel

@Composable
fun CallsScreen(
    viewModel: MishaViewModel,
    modifier: Modifier = Modifier
) {
    var dialNumber by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "COMMUNICATION & CALLS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = TextPrimary
            )
            Text(
                text = "Telecom Voice Manager & Dialer",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cellular limitation notice banner
            GlassCard(
                borderColor = PurpleGlow.copy(alpha = 0.4f),
                backgroundColor = PurpleDeep.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PurpleGlow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OS Privacy Standard",
                        fontWeight = FontWeight.Bold,
                        color = PurpleGlow,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Android OS enforces strict sandboxing for cellular call uplink audio. Misha supports call initiation, answering, rejecting, and contact resolution without simulating unauthorized cellular stream manipulation.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dialer Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DIRECT VOICE DIALER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = CyanCore
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dialNumber,
                    onValueChange = { dialNumber = it },
                    label = { Text("Contact name or phone number", color = TextTertiary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanCore,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialer_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonButton(
                    text = "Place Call",
                    icon = Icons.Default.Call,
                    glowColor = NeonEmerald,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "place_call_button",
                    onClick = {
                        if (dialNumber.isNotBlank()) {
                            viewModel.processSpokenText("call $dialNumber")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "VOICE COMMAND SAMPLES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TextTertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SampleCommandCard(
                phrase = "\"Misha call Prit\"",
                desc = "Resolves contact named Prit and connects call"
            ) { viewModel.processSpokenText("call Prit") }

            Spacer(modifier = Modifier.height(8.dp))

            SampleCommandCard(
                phrase = "\"Prit ko WhatsApp pe hello bhej\"",
                desc = "Automates verified WhatsApp delivery"
            ) { viewModel.processSpokenText("Prit ko WhatsApp pe hello bhej") }

            Spacer(modifier = Modifier.height(8.dp))

            SampleCommandCard(
                phrase = "\"Send SMS to Rahul saying I'll be late\"",
                desc = "Sends direct SMS or opens verified composer"
            ) { viewModel.processSpokenText("Send SMS to Rahul saying I'll be late") }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SampleCommandCard(
    phrase: String,
    desc: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = phrase,
                    color = CyanGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = desc,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Test Command",
                tint = CyanCore
            )
        }
    }
}
