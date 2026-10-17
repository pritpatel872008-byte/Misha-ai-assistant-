package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.UserSessionEntity
import com.example.security.AuthResult
import com.example.ui.screens.mishaTextFieldColors
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountProfileBadge(
    userSession: UserSessionEntity?,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (userSession == null) return

    val initials = userSession.displayName.take(2).uppercase().ifBlank { "ME" }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceHighlight.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, CyanCore.copy(alpha = 0.4f)),
        modifier = modifier
            .testTag("account_profile_badge")
            .clickable(onClick = onOpenProfile)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(CyanCore.copy(alpha = 0.2f))
                    .border(1.dp, CyanCore, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = CyanCore
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = userSession.displayName.split(" ").firstOrNull() ?: userSession.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified User",
                tint = NeonGreen,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AccountProfileDialog(
    userSession: UserSessionEntity?,
    viewModel: MishaViewModel,
    onDismiss: () -> Unit,
    onLogoutRequested: () -> Unit
) {
    if (userSession == null) {
        onDismiss()
        return
    }

    var isChangePasswordOpen by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var changePassError by remember { mutableStateOf<String?>(null) }
    var changePassSuccess by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    val formattedLoginDate = remember(userSession.loginTimestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(userSession.loginTimestamp))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.5.dp, CyanCore.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User Security Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar and Display Info
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CyanCore.copy(alpha = 0.15f))
                        .border(2.dp, CyanCore, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userSession.displayName.take(2).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanCore
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = userSession.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = userSession.email,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Info Badges
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Account Status:", fontSize = 11.sp, color = TextSecondary)
                            Text("VERIFIED & ENCRYPTED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Session Active Since:", fontSize = 11.sp, color = TextSecondary)
                            Text(formattedLoginDate, fontSize = 11.sp, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cipher Suite:", fontSize = 11.sp, color = TextSecondary)
                            Text("PBKDF2-HMAC-SHA256", fontSize = 11.sp, color = CyanCore)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isChangePasswordOpen) {
                    // Actions Menu
                    OutlinedButton(
                        onClick = {
                            isChangePasswordOpen = true
                            changePassError = null
                            changePassSuccess = null
                            currentPassword = ""
                            newPassword = ""
                            confirmNewPassword = ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DarkSurfaceHighlight.copy(alpha = 0.3f),
                            contentColor = NeonPurple
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_change_password_button")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Master Password", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Logout Button
                    Button(
                        onClick = { showLogoutConfirmation = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCrimson.copy(alpha = 0.15f),
                            contentColor = NeonCrimson
                        ),
                        border = BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of Misha AI", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // --- CHANGE PASSWORD FORM ---
                    Text(
                        text = "Change Master Password",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    changePassError?.let {
                        Text(it, color = NeonCrimson, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    changePassSuccess?.let {
                        Text(it, color = NeonGreen, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_current_password_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password (min 8 chars)") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_new_password_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_confirm_password_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isChangePasswordOpen = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                if (newPassword != confirmNewPassword) {
                                    changePassError = "New passwords do not match."
                                    return@Button
                                }
                                if (newPassword.length < 8) {
                                    changePassError = "Password must be at least 8 characters."
                                    return@Button
                                }
                                isSubmitting = true
                                changePassError = null
                                viewModel.changePassword(userSession.userId, currentPassword, newPassword) { result ->
                                    isSubmitting = false
                                    when (result) {
                                        is AuthResult.Success -> {
                                            changePassSuccess = "Password updated successfully!"
                                            currentPassword = ""
                                            newPassword = ""
                                            confirmNewPassword = ""
                                        }
                                        is AuthResult.Error -> {
                                            changePassError = result.message
                                        }
                                    }
                                }
                            },
                            enabled = !isSubmitting && currentPassword.isNotBlank() && newPassword.isNotBlank() && newPassword == confirmNewPassword,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("change_password_submit_button")
                        ) {
                            Text(if (isSubmitting) "Updating..." else "Save Password", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Confirm Sign Out") },
            text = { Text("Are you sure you want to end your authenticated session? You will need your password to sign back in.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        onDismiss()
                        onLogoutRequested()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson)
                ) {
                    Text("Sign Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
