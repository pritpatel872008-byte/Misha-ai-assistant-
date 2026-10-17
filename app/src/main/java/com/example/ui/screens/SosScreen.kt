package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.SosDeliveryState
import com.example.data.local.entities.EmergencyContactEntity
import com.example.data.local.entities.SosEventEntity
import com.example.data.local.entities.SosSettingsEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaUiState
import com.example.ui.viewmodel.MishaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SosScreen(
    viewModel: MishaViewModel,
    uiState: MishaUiState,
    contacts: List<EmergencyContactEntity>,
    settings: SosSettingsEntity?,
    events: List<SosEventEntity>,
    modifier: Modifier = Modifier
) {
    val currentSettings = settings ?: SosSettingsEntity()
    var showAddContactDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContactEntity?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDirectCallDialog by remember { mutableStateOf(false) }

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
                        text = "EMERGENCY SOS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = NeonCrimson
                    )
                    Text(
                        text = "Real Telephony & Automated Protection",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row {
                    IconButton(
                        onClick = { showDirectCallDialog = true },
                        modifier = Modifier.testTag("emergency_dial_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalPhone,
                            contentDescription = "Direct Emergency Call",
                            tint = NeonCrimson
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Emergency Trigger Box / Active Status
            SosActiveHeroCard(
                uiState = uiState,
                onTriggerSos = { viewModel.triggerEmergencySos(isTest = false) },
                onCancelSos = { viewModel.cancelActiveSos() },
                onSendTest = { viewModel.sendTestSos() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Emergency Contacts Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TRUSTED EMERGENCY CONTACTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = TextTertiary
                    )
                    Text(
                        text = "Dispatches SMS payload in priority order",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                FilledTonalButton(
                    onClick = { showAddContactDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CyanDim.copy(alpha = 0.3f),
                        contentColor = CyanCore
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_contact_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (contacts.isEmpty()) {
            item {
                GlassCard(
                    borderColor = NeonAmber.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "No Emergency Contacts Configured",
                                fontWeight = FontWeight.Bold,
                                color = NeonAmber,
                                fontSize = 13.sp
                            )
                            Text(
                                "Add at least one contact to enable automated SOS SMS dispatch.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else {
            itemsIndexed(contacts) { index, contact ->
                EmergencyContactRowItem(
                    contact = contact,
                    priorityIndex = index + 1,
                    onEdit = { editingContact = contact },
                    onDelete = { viewModel.deleteEmergencyContact(contact.id) },
                    onTest = { viewModel.testSingleEmergencyContact(contact) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            // SOS Settings Card Header
            Text(
                text = "SOS SYSTEM SETTINGS & PREFERENCES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            SosConfigPanel(
                settings = currentSettings,
                onUpdateSettings = { viewModel.updateSosSettings(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SOS History Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISPATCH AUDIT & EVENT HISTORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = TextTertiary
                )
                if (events.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearSosHistory() },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Clear Logs", color = TextTertiary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (events.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No SOS dispatch events logged. Test mode events will appear here.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            itemsIndexed(events.take(6)) { _, event ->
                SosEventItemCard(event = event)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddContactDialog) {
        AddEditContactDialog(
            contact = null,
            onDismiss = { showAddContactDialog = false },
            onSave = { name, phone, rel ->
                viewModel.addEmergencyContact(name, phone, rel)
                showAddContactDialog = false
            }
        )
    }

    if (editingContact != null) {
        AddEditContactDialog(
            contact = editingContact,
            onDismiss = { editingContact = null },
            onSave = { name, phone, rel ->
                editingContact?.let {
                    viewModel.updateEmergencyContact(
                        it.copy(name = name, phoneNumber = phone, relationship = rel)
                    )
                }
                editingContact = null
            }
        )
    }

    if (showDirectCallDialog) {
        AlertDialog(
            onDismissRequest = { showDirectCallDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text("Direct Emergency Call", color = NeonCrimson, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Choose an emergency service to dial immediately from your telephony subsystem:",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.makeDirectEmergencyCall("112")
                        showDirectCallDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson)
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call 112 (India / Unified)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectCallDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SosActiveHeroCard(
    uiState: MishaUiState,
    onTriggerSos: () -> Unit,
    onCancelSos: () -> Unit,
    onSendTest: () -> Unit
) {
    val state = uiState.sosDeliveryState
    val isCountdown = state == SosDeliveryState.SOS_COUNTDOWN
    val isSending = state == SosDeliveryState.SOS_SENDING

    val cardBorderColor = when (state) {
        SosDeliveryState.SOS_COUNTDOWN -> NeonCrimson
        SosDeliveryState.SOS_SENDING -> NeonAmber
        SosDeliveryState.SOS_SENT -> NeonEmerald
        SosDeliveryState.SOS_PARTIALLY_SENT -> NeonAmber
        SosDeliveryState.SOS_FAILED, SosDeliveryState.SOS_PERMISSION_REQUIRED -> NeonCrimson
        else -> NeonCrimson.copy(alpha = 0.35f)
    }

    GlassCard(
        borderColor = cardBorderColor,
        backgroundColor = if (isCountdown) NeonCrimson.copy(alpha = 0.12f) else DarkSurfaceElevated.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // State Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (state) {
                    SosDeliveryState.SOS_COUNTDOWN -> NeonCrimson
                    SosDeliveryState.SOS_SENDING -> NeonAmber.copy(alpha = 0.2f)
                    SosDeliveryState.SOS_SENT -> NeonEmerald.copy(alpha = 0.2f)
                    SosDeliveryState.SOS_FAILED -> NeonCrimson.copy(alpha = 0.2f)
                    else -> DarkSurfaceHighlight
                },
                border = BorderStroke(
                    1.dp,
                    when (state) {
                        SosDeliveryState.SOS_COUNTDOWN -> NeonCrimson
                        SosDeliveryState.SOS_SENDING -> NeonAmber
                        SosDeliveryState.SOS_SENT -> NeonEmerald
                        SosDeliveryState.SOS_FAILED -> NeonCrimson
                        else -> TextTertiary.copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    SosDeliveryState.SOS_COUNTDOWN -> Color.White
                                    SosDeliveryState.SOS_SENDING -> NeonAmber
                                    SosDeliveryState.SOS_SENT -> NeonEmerald
                                    SosDeliveryState.SOS_FAILED -> NeonCrimson
                                    else -> NeonEmerald
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.label.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = when (state) {
                            SosDeliveryState.SOS_COUNTDOWN -> Color.White
                            SosDeliveryState.SOS_SENDING -> NeonAmber
                            SosDeliveryState.SOS_SENT -> NeonEmerald
                            SosDeliveryState.SOS_FAILED -> NeonCrimson
                            else -> TextSecondary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Trigger Circle / Countdown display
            if (isCountdown) {
                val secs = uiState.sosCountdownSeconds ?: 0
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(NeonCrimson.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$secs",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCrimson
                        )
                        Text(
                            text = "SECONDS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DISPATCHING EMERGENCY SOS...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = NeonCrimson
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onCancelSos,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_sos_countdown_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ABORT / CANCEL SOS", color = Color.Black, fontWeight = FontWeight.Black)
                }
            } else {
                // Large Standby SOS Trigger Button
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonCrimson, NeonCrimson.copy(alpha = 0.4f))
                            )
                        )
                        .clickable { onTriggerSos() }
                        .testTag("primary_sos_trigger_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Trigger SOS",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "SEND SOS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tap to trigger Emergency SOS to trusted contacts",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSendTest,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_test_sos_button"),
                        border = BorderStroke(1.dp, CyanCore.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanCore)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Test SOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (state != SosDeliveryState.SOS_IDLE) {
                        OutlinedButton(
                            onClick = onCancelSos,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, TextTertiary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Reset Status", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (uiState.lastSosResult != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkSurfaceHighlight)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.lastSosResult.message,
                    fontSize = 12.sp,
                    color = if (uiState.lastSosResult.state == SosDeliveryState.SOS_SENT) NeonEmerald else NeonAmber
                )
            }
        }
    }
}

@Composable
private fun EmergencyContactRowItem(
    contact: EmergencyContactEntity,
    priorityIndex: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Priority Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when (priorityIndex) {
                                1 -> NeonCrimson.copy(alpha = 0.2f)
                                2 -> NeonAmber.copy(alpha = 0.2f)
                                else -> CyanDim.copy(alpha = 0.3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$priorityIndex",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = when (priorityIndex) {
                            1 -> NeonCrimson
                            2 -> NeonAmber
                            else -> CyanCore
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        if (priorityIndex == 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NeonCrimson.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "PRIMARY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonCrimson,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${contact.phoneNumber} • ${contact.relationship}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Row {
                IconButton(onClick = onTest) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Test Contact",
                        tint = CyanCore,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Contact",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Contact",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SosConfigPanel(
    settings: SosSettingsEntity,
    onUpdateSettings: (SosSettingsEntity) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkSurfaceElevated.copy(alpha = 0.9f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Emergency SOS Active", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text("Enables voice and button SOS triggering", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.isSosEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(isSosEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonEmerald)
                )
            }

            HorizontalDivider(color = DarkSurfaceHighlight)

            // One-Tap SOS Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("One-Tap Immediate SOS", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text("Skips voice confirmation question and sends immediately", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.isOneTapEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(isOneTapEnabled = it)) }
                )
            }

            HorizontalDivider(color = DarkSurfaceHighlight)

            // Voice Confirmation Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Voice Confirmation", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text("Asks \"Emergency SOS bhejne wala hoon. Confirm?\" before send", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.requireVoiceConfirmation,
                    onCheckedChange = { onUpdateSettings(settings.copy(requireVoiceConfirmation = it)) }
                )
            }

            HorizontalDivider(color = DarkSurfaceHighlight)

            // Countdown Delay
            Column {
                Text("Countdown Protection Delay", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Text("Allows cancelling accidental triggers before SMS dispatch", color = TextSecondary, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0 to "OFF", 5 to "5s", 10 to "10s", 15 to "15s").forEach { (sec, label) ->
                        val selected = settings.countdownSeconds == sec
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) NeonCrimson.copy(alpha = 0.25f) else DarkSurfaceHighlight,
                            border = BorderStroke(1.dp, if (selected) NeonCrimson else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateSettings(settings.copy(countdownSeconds = sec)) }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (selected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = DarkSurfaceHighlight)

            // Include Location Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Attach GPS Location Link", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text("Appends real Google Maps coordinates to SMS", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.includeLocation,
                    onCheckedChange = { onUpdateSettings(settings.copy(includeLocation = it)) }
                )
            }

            HorizontalDivider(color = DarkSurfaceHighlight)

            // Include Battery Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Attach Battery Percentage", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text("Appends real device battery level (e.g. 37%)", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.includeBattery,
                    onCheckedChange = { onUpdateSettings(settings.copy(includeBattery = it)) }
                )
            }

            HorizontalDivider(color = DarkSurfaceHighlight)

            // Custom Message
            Column {
                Text("Emergency SMS Message Template", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = settings.customMessage,
                    onValueChange = { onUpdateSettings(settings.copy(customMessage = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanCore,
                        unfocusedBorderColor = TextTertiary.copy(alpha = 0.5f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun SosEventItemCard(event: SosEventEntity) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
    val isTest = event.isTest

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = DarkSurfaceHighlight.copy(alpha = 0.7f),
        borderColor = if (isTest) CyanCore.copy(alpha = 0.3f) else NeonCrimson.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isTest) CyanDim.copy(alpha = 0.3f) else NeonCrimson.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isTest) "TEST SOS" else "EMERGENCY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTest) CyanCore else NeonCrimson,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = event.status,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
            Text(
                text = dateStr,
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = event.deliverySummary,
            fontSize = 11.sp,
            color = TextSecondary
        )
        if (event.recipients.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Recipients: ${event.recipients}",
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun AddEditContactDialog(
    contact: EmergencyContactEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, relationship: String) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phoneNumber ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "Family") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Text(
                text = if (contact == null) "Add Emergency Contact" else "Edit Emergency Contact",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name (e.g. Mom, Prit)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (e.g. +919876543210)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship / Label") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(name, phone, relationship)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanCore)
            ) {
                Text("Save Contact", color = DarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
