package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.security.AuthResult
import com.example.security.AuthSecurityUtil
import com.example.security.PasswordResetResult
import com.example.security.PasswordStrengthLevel
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.MishaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthTab {
    LOGIN,
    SIGN_UP
}

@Composable
fun AuthScreen(
    viewModel: MishaViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedTab by remember { mutableStateOf(AuthTab.LOGIN) }

    // Login Form State
    var loginEmail by remember { mutableStateOf("patelchetanbhai468@gmail.com") }
    var loginPassword by remember { mutableStateOf("Misha@2026") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Sign Up Form State
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }
    var signupConfirmPasswordVisible by remember { mutableStateOf(false) }
    var signupSecurityQuestion by remember { mutableStateOf("What is your primary emergency keyword?") }
    var signupSecurityAnswer by remember { mutableStateOf("misha") }
    var signupRecoveryPin by remember { mutableStateOf("1234") }

    // Reset Password Modal State
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetStep by remember { mutableStateOf(1) } // 1: Email Request, 2: OTP + New Password
    var resetOtp by remember { mutableStateOf("") }
    var resetDispatchedOtp by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }
    var resetConfirmPassword by remember { mutableStateOf("") }
    var resetPasswordVisible by remember { mutableStateOf(false) }
    var resetDialogError by remember { mutableStateOf<String?>(null) }
    var resetDialogSuccess by remember { mutableStateOf<String?>(null) }
    var isResetLoading by remember { mutableStateOf(false) }

    // General Auth UI States
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val passwordStrength = remember(if (selectedTab == AuthTab.SIGN_UP) signupPassword else resetNewPassword) {
        AuthSecurityUtil.evaluatePasswordStrength(if (selectedTab == AuthTab.SIGN_UP) signupPassword else resetNewPassword)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBg,
                        Color(0xFF0D1322),
                        DarkBg
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Brand Header & Shield Emblem
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(1.5.dp, CyanCore, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Misha Security Shield",
                    tint = CyanCore,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MISHA AI",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = TextPrimary
            )

            Text(
                text = "NEURAL GATEWAY • SECURE AUTHENTICATION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = CyanCore
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Tabs (Sign In / Sign Up)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabPill(
                        text = "SIGN IN",
                        icon = Icons.Default.LockOpen,
                        isSelected = selectedTab == AuthTab.LOGIN,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_tab_login")
                    ) {
                        selectedTab = AuthTab.LOGIN
                        errorMessage = null
                        successMessage = null
                    }

                    TabPill(
                        text = "SIGN UP",
                        icon = Icons.Default.PersonAdd,
                        isSelected = selectedTab == AuthTab.SIGN_UP,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("auth_tab_signup")
                    ) {
                        selectedTab = AuthTab.SIGN_UP
                        errorMessage = null
                        successMessage = null
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error / Success Notification Banners
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonCrimson.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NeonCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = NeonCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = err,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { errorMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = successMessage != null) {
                successMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NeonGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = "Success",
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Auth Card Form
            GlassCard(
                borderColor = GlassBorder,
                backgroundColor = DarkSurfaceElevated.copy(alpha = 0.85f),
                cornerRadius = 20.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedTab == AuthTab.LOGIN) {
                    // --- LOGIN FORM ---
                    Text(
                        text = "Access Your AI Core",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Enter your verified credentials to synchronize neural memory and encrypted tools.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    // Email Field
                    OutlinedTextField(
                        value = loginEmail,
                        onValueChange = { loginEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("user@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = CyanCore)
                        },
                        trailingIcon = {
                            if (loginEmail.isNotBlank()) {
                                IconButton(onClick = { loginEmail = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextTertiary)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("Master Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CyanCore)
                        },
                        trailingIcon = {
                            IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                Icon(
                                    imageVector = if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (loginPasswordVisible) "Hide password" else "Show password",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!isLoading) {
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.login(loginEmail, loginPassword, rememberMe) { result ->
                                        isLoading = false
                                        when (result) {
                                            is AuthResult.Success -> {
                                                successMessage = result.message
                                            }
                                            is AuthResult.Error -> {
                                                errorMessage = result.message
                                            }
                                        }
                                    }
                                }
                            }
                        ),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Remember Me & Forgot Password Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CyanCore,
                                    checkmarkColor = Color.Black
                                ),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Remember Session",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        TextButton(
                            onClick = {
                                resetEmail = loginEmail.trim()
                                resetStep = 1
                                resetOtp = ""
                                resetDispatchedOtp = ""
                                resetNewPassword = ""
                                resetConfirmPassword = ""
                                resetDialogError = null
                                resetDialogSuccess = null
                                showResetDialog = true
                            },
                            modifier = Modifier.testTag("auth_forgot_password_button")
                        ) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign In Submit Button
                    NeonButton(
                        text = if (isLoading) "AUTHENTICATING..." else "SIGN IN TO MISHA",
                        icon = Icons.Default.Login,
                        glowColor = CyanCore,
                        enabled = !isLoading && loginEmail.isNotBlank() && loginPassword.isNotBlank(),
                        testTag = "auth_submit_button",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        focusManager.clearFocus()
                        isLoading = true
                        errorMessage = null
                        viewModel.login(loginEmail, loginPassword, rememberMe) { result ->
                            isLoading = false
                            when (result) {
                                is AuthResult.Success -> {
                                    successMessage = result.message
                                }
                                is AuthResult.Error -> {
                                    errorMessage = result.message
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Demo Fill
                    OutlinedButton(
                        onClick = {
                            loginEmail = "patelchetanbhai468@gmail.com"
                            loginPassword = "Misha@2026"
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanCore.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DarkSurfaceHighlight.copy(alpha = 0.4f),
                            contentColor = CyanCore
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_demo_fill_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanCore,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fill Default Demo Account",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                } else {
                    // --- SIGN UP FORM ---
                    Text(
                        text = "Create Encrypted Account",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Your voice models, emergency contacts, and knowledge vaults will be linked securely.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    // Full Name
                    OutlinedTextField(
                        value = signupName,
                        onValueChange = { signupName = it },
                        label = { Text("Display Name / Full Name") },
                        placeholder = { Text("e.g. Chetan Patel") },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = CyanCore)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Address
                    OutlinedTextField(
                        value = signupEmail,
                        onValueChange = { signupEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("you@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = CyanCore)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_email_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = signupPassword,
                        onValueChange = { signupPassword = it },
                        label = { Text("Create Master Password") },
                        placeholder = { Text("Min 8 chars, mixed types") },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = CyanCore)
                        },
                        trailingIcon = {
                            IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                                Icon(
                                    imageVector = if (signupPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (signupPasswordVisible) "Hide" else "Show",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_password_input")
                    )

                    // Password Entropy Meter
                    if (signupPassword.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        PasswordStrengthMeter(result = passwordStrength)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value = signupConfirmPassword,
                        onValueChange = { signupConfirmPassword = it },
                        label = { Text("Confirm Password") },
                        placeholder = { Text("Re-enter password") },
                        leadingIcon = {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = CyanCore)
                        },
                        trailingIcon = {
                            IconButton(onClick = { signupConfirmPasswordVisible = !signupConfirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (signupConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (signupConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_confirm_password_input")
                    )

                    if (signupConfirmPassword.isNotEmpty() && signupPassword != signupConfirmPassword) {
                        Text(
                            text = "Passwords do not match.",
                            color = NeonCrimson,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Backup Recovery PIN
                    OutlinedTextField(
                        value = signupRecoveryPin,
                        onValueChange = { if (it.length <= 6) signupRecoveryPin = it },
                        label = { Text("Backup 4-Digit Recovery PIN") },
                        placeholder = { Text("1234") },
                        leadingIcon = {
                            Icon(Icons.Default.Pin, contentDescription = null, tint = NeonPurple)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        colors = mishaTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_recovery_pin_input")
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sign Up Submit Button
                    NeonButton(
                        text = if (isLoading) "CREATING ACCOUNT..." else "REGISTER & INITIALIZE",
                        icon = Icons.Default.VerifiedUser,
                        glowColor = NeonPurple,
                        enabled = !isLoading &&
                                signupEmail.isNotBlank() &&
                                signupPassword.isNotBlank() &&
                                signupPassword == signupConfirmPassword,
                        testTag = "signup_submit_button",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        focusManager.clearFocus()
                        if (signupPassword != signupConfirmPassword) {
                            errorMessage = "Passwords do not match. Please verify."
                            return@NeonButton
                        }
                        if (passwordStrength.score < 2) {
                            errorMessage = "Password does not meet minimum security requirements."
                            return@NeonButton
                        }

                        isLoading = true
                        errorMessage = null
                        viewModel.signUp(
                            email = signupEmail,
                            pass = signupPassword,
                            name = signupName.ifBlank { "User" },
                            secQuestion = signupSecurityQuestion,
                            secAnswer = signupSecurityAnswer,
                            recoveryPin = signupRecoveryPin.ifBlank { "1234" }
                        ) { result ->
                            isLoading = false
                            when (result) {
                                is AuthResult.Success -> {
                                    successMessage = result.message
                                }
                                is AuthResult.Error -> {
                                    errorMessage = result.message
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Specifications Footer
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "PBKDF2-HMAC-SHA256 • On-Device Cryptographic Vault • Zero-Cloud Plaintext",
                    fontSize = 10.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // ==========================================
    // FORGOT & RESET PASSWORD MODAL DIALOG
    // ==========================================
    if (showResetDialog) {
        Dialog(onDismissRequest = { if (!isResetLoading) showResetDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.5.dp, NeonPurple),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Password Recovery",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { showResetDialog = false },
                            enabled = !isResetLoading
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Alert Messages
                    resetDialogError?.let { err ->
                        Text(
                            text = err,
                            color = NeonCrimson,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }

                    resetDialogSuccess?.let { msg ->
                        Text(
                            text = msg,
                            color = NeonGreen,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }

                    if (resetStep == 1) {
                        // STEP 1: Request OTP by Email
                        Text(
                            text = "Enter your registered email address. A 6-digit cryptographic recovery OTP will be generated.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Account Email") },
                            placeholder = { Text("user@example.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = CyanCore)
                            },
                            singleLine = true,
                            colors = mishaTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_email_input")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        NeonButton(
                            text = if (isResetLoading) "DISPATCHING CODE..." else "SEND RECOVERY CODE",
                            icon = Icons.Default.Send,
                            glowColor = NeonPurple,
                            enabled = !isResetLoading && resetEmail.isNotBlank(),
                            testTag = "reset_send_otp_button",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            isResetLoading = true
                            resetDialogError = null
                            viewModel.requestPasswordReset(resetEmail) { result ->
                                isResetLoading = false
                                when (result) {
                                    is PasswordResetResult.OtpSent -> {
                                        resetDispatchedOtp = result.previewOtp
                                        resetStep = 2
                                        resetDialogSuccess = "OTP dispatched! For on-device verification, code is: ${result.previewOtp}"
                                    }
                                    is PasswordResetResult.Error -> {
                                        resetDialogError = result.message
                                    }
                                    else -> {}
                                }
                            }
                        }
                    } else {
                        // STEP 2: Enter OTP & Set New Password
                        Text(
                            text = "Verification code generated for $resetEmail. Enter the code and your new password.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Dispatched Code Chip Helper
                        if (resetDispatchedOtp.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyanCore.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, CyanCore.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { resetOtp = resetDispatchedOtp }
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Generated OTP: $resetDispatchedOtp",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyanCore
                                    )
                                    Text(
                                        text = "TAP TO AUTO-FILL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                }
                            }
                        }

                        // OTP Code Input
                        OutlinedTextField(
                            value = resetOtp,
                            onValueChange = { if (it.length <= 6) resetOtp = it },
                            label = { Text("6-Digit OTP Code") },
                            placeholder = { Text("123456") },
                            leadingIcon = {
                                Icon(Icons.Default.Pin, contentDescription = null, tint = CyanCore)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = mishaTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_otp_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // New Password
                        OutlinedTextField(
                            value = resetNewPassword,
                            onValueChange = { resetNewPassword = it },
                            label = { Text("New Master Password") },
                            placeholder = { Text("Min 8 characters") },
                            leadingIcon = {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = CyanCore)
                            },
                            trailingIcon = {
                                IconButton(onClick = { resetPasswordVisible = !resetPasswordVisible }) {
                                    Icon(
                                        imageVector = if (resetPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = mishaTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_new_password_input")
                        )

                        if (resetNewPassword.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            PasswordStrengthMeter(result = passwordStrength)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Confirm New Password
                        OutlinedTextField(
                            value = resetConfirmPassword,
                            onValueChange = { resetConfirmPassword = it },
                            label = { Text("Confirm New Password") },
                            placeholder = { Text("Re-type new password") },
                            leadingIcon = {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = CyanCore)
                            },
                            visualTransformation = if (resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = mishaTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_confirm_password_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        NeonButton(
                            text = if (isResetLoading) "UPDATING PASSWORD..." else "RESET PASSWORD & SIGN IN",
                            icon = Icons.Default.Check,
                            glowColor = NeonGreen,
                            enabled = !isResetLoading &&
                                    resetOtp.isNotBlank() &&
                                    resetNewPassword.isNotBlank() &&
                                    resetNewPassword == resetConfirmPassword,
                            testTag = "reset_submit_button",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (resetNewPassword != resetConfirmPassword) {
                                resetDialogError = "Passwords do not match."
                                return@NeonButton
                            }
                            if (passwordStrength.score < 2) {
                                resetDialogError = "Password is too weak. Ensure at least 8 characters."
                                return@NeonButton
                            }

                            isResetLoading = true
                            resetDialogError = null
                            viewModel.completePasswordReset(
                                email = resetEmail,
                                otp = resetOtp,
                                newPass = resetNewPassword
                            ) { result ->
                                isResetLoading = false
                                when (result) {
                                    is AuthResult.Success -> {
                                        showResetDialog = false
                                        successMessage = result.message
                                    }
                                    is AuthResult.Error -> {
                                        resetDialogError = result.message
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) DarkSurfaceHighlight else Color.Transparent)
            .border(
                BorderStroke(
                    if (isSelected) 1.dp else 0.dp,
                    if (isSelected) CyanCore.copy(alpha = 0.5f) else Color.Transparent
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) CyanCore else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
}

@Composable
fun PasswordStrengthMeter(result: com.example.security.PasswordStrengthResult) {
    val barColor = when (result.level) {
        PasswordStrengthLevel.WEAK -> NeonCrimson
        PasswordStrengthLevel.FAIR -> NeonAmber
        PasswordStrengthLevel.GOOD -> CyanCore
        PasswordStrengthLevel.STRONG -> NeonGreen
    }

    val progress = when (result.score) {
        0 -> 0.1f
        1 -> 0.25f
        2 -> 0.5f
        3 -> 0.75f
        else -> 1.0f
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strength: ${result.level.name}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = barColor,
            trackColor = DarkSurfaceHighlight,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
        Text(
            text = result.feedbackMessage,
            fontSize = 10.sp,
            color = TextTertiary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun mishaTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanCore,
        unfocusedBorderColor = GlassBorder,
        focusedLabelColor = CyanCore,
        unfocusedLabelColor = TextSecondary,
        cursorColor = CyanCore,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = DarkSurfaceElevated.copy(alpha = 0.5f),
        unfocusedContainerColor = DarkSurfaceElevated.copy(alpha = 0.3f)
    )
}
