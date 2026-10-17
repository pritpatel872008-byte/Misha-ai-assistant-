package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ConversationMessageEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaUiState
import com.example.ui.viewmodel.MishaViewModel
import com.example.voice.MishaState

@Composable
fun AssistantScreen(
    viewModel: MishaViewModel,
    uiState: MishaUiState,
    messages: List<ConversationMessageEntity>,
    modifier: Modifier = Modifier
) {
    var typedText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MISHA INTELLIGENCE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Continuous Voice & Intent Pipeline",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            MishaStatusBadge(state = uiState.mishaState)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mini 3D Core / Visualizer Header
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = CyanDim.copy(alpha = 0.3f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Misha3DCore(
                    state = uiState.mishaState,
                    rmsLevel = uiState.currentRms,
                    size = 72.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.mishaState == MishaState.COMMAND_LISTENING) "Listening to speech..."
                        else if (uiState.mishaState == MishaState.PROCESSING) "Gemini reasoning & intent routing..."
                        else if (uiState.mishaState == MishaState.SPEAKING) "Speaking response..."
                        else "Ears active for \"Misha\"",
                        color = CyanGlow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    VoiceWaveform(
                        rms = uiState.currentRms,
                        isListening = uiState.mishaState == MishaState.COMMAND_LISTENING
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Prompt Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { viewModel.processSpokenText("flashlight on") },
                label = { Text("💡 Flashlight On", fontSize = 11.sp) },
                colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceHighlight)
            )
            AssistChip(
                onClick = { viewModel.processSpokenText("Ahmedabad weather") },
                label = { Text("☀️ Weather", fontSize = 11.sp) },
                colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceHighlight)
            )
            AssistChip(
                onClick = { viewModel.processSpokenText("battery status") },
                label = { Text("🔋 Battery", fontSize = 11.sp) },
                colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceHighlight)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Conversation Message Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true
        ) {
            items(messages) { msg ->
                MessageBubble(msg = msg)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = typedText,
                onValueChange = { typedText = it },
                placeholder = { Text("Type command or say 'Misha'...", color = TextTertiary, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_text_input"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanCore,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (typedText.isNotBlank()) {
                        viewModel.processSpokenText(typedText)
                        typedText = ""
                        focusManager.clearFocus()
                    }
                }),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (typedText.isNotBlank()) {
                        viewModel.processSpokenText(typedText)
                        typedText = ""
                        focusManager.clearFocus()
                    } else {
                        if (uiState.mishaState == MishaState.COMMAND_LISTENING) {
                            viewModel.stopListeningCommand()
                        } else {
                            viewModel.startListeningCommand()
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (uiState.mishaState == MishaState.COMMAND_LISTENING) NeonEmerald else CyanCore)
                    .testTag("send_or_mic_button")
            ) {
                Icon(
                    imageVector = if (typedText.isNotBlank()) Icons.Default.Send
                    else if (uiState.mishaState == MishaState.COMMAND_LISTENING) Icons.Default.Stop
                    else Icons.Default.Mic,
                    contentDescription = "Send or Mic",
                    tint = DarkBg
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ConversationMessageEntity) {
    val isUser = msg.sender == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) CyanDim.copy(alpha = 0.4f) else DarkSurfaceElevated,
                border = BorderStroke(1.dp, if (isUser) CyanCore.copy(alpha = 0.4f) else PurpleGlow.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isUser) "You" else "Misha AI",
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) CyanGlow else PurpleGlow,
                            fontSize = 12.sp
                        )
                        if (msg.intent != null) {
                            Text(
                                text = msg.intent,
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg.message,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
