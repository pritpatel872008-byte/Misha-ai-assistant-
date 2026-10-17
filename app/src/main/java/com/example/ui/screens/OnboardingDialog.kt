package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            borderColor = CyanCore,
            backgroundColor = DarkSurfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = CyanCore,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "WELCOME TO MISHA AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Futuristic personal AI assistant for Android with voice intelligence, verified device actions, and real automation.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceHighlight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("• Say \"Misha\" to wake anytime", color = CyanGlow, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Double-clap to trigger listening", color = CyanGlow, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Controls flashlight, calls, SMS, WhatsApp & weather", color = CyanGlow, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                NeonButton(
                    text = "Initialize Neural Link",
                    glowColor = NeonEmerald,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "onboarding_start_button",
                    onClick = onDismiss
                )
            }
        }
    }
}
