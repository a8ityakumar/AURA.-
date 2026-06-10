package com.example.ui

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

enum class AuthScreenMode {
    LOGIN,
    SIGNUP,
    FORGOT_PASSWORD
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(AuthScreenMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val inPreview = remember { isRunningInPreview() }
    val showGoogleSignIn = false

    // Resolve the Web Client ID dynamically or fallback
    val webClientId = remember(context) {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else "835714928645-lmgfs9qb6v69n03kchvqj0lr070uuvla.apps.googleusercontent.com"
    }

    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember(context, gso) {
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            val email = account?.email ?: ""
            val displayName = account?.displayName
            val photoUrl = account?.photoUrl?.toString()

            if (!idToken.isNullOrBlank()) {
                viewModel.loginWithGoogle(
                    idToken = idToken,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl
                )
            } else {
                Log.e("AuthScreen", "Google Sign-In failed: parsed ID token is null")
                viewModel.setCustomError("Google Sign-In failed. Please use email login for now.")
            }
        } catch (e: Exception) {
            Log.e("AuthScreen", "Google Sign-In API flow exception info (logged for safety): ", e)
            viewModel.setCustomError("Google Sign-In failed. Please use email login for now.")
        }
    }

    // Observe login success to transition screens
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onAuthSuccess()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .maxSizeWidthModifier()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Logo & Wordmark Area
                Text(
                    text = "AURA.",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        fontSize = 32.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.testTag("auth_logo")
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "INTELLIGENCE SIMPLIFIED",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color.White.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Contextual instruction text
                val subtitle = when (mode) {
                    AuthScreenMode.LOGIN -> "Sign in to access your secure assistant workspace."
                    AuthScreenMode.SIGNUP -> "Create a secure account to sync historic sessions."
                    AuthScreenMode.FORGOT_PASSWORD -> "Enter your email to reset your assistant password."
                }
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.61f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Error Message Accent Panel
                if (uiState is AuthUiState.Error) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF140D0D)),
                        border = BorderStroke(1.dp, Color(0xFF3B1A1A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ERROR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFFFF5252)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = (uiState as AuthUiState.Error).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clickable { viewModel.clearError() }
                                    .padding(start = 12.dp)
                            )
                        }
                    }
                }

                // Success Message Inline Panel
                successMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D140D)),
                        border = BorderStroke(1.dp, Color(0xFF1A3B1A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SUCCESS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFF52FF52)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                val fieldShape = RoundedCornerShape(16.dp)

                // Input form fields
                if (mode == AuthScreenMode.SIGNUP) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_display_name"),
                        label = { Text("Display Name") },
                        placeholder = { Text("Your name (optional)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = auraTextFieldColors(),
                        shape = fieldShape,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email"),
                    label = { Text("Email Address") },
                    placeholder = { Text("email@example.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    colors = auraTextFieldColors(),
                    shape = fieldShape,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = if (mode == AuthScreenMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (mode == AuthScreenMode.FORGOT_PASSWORD) focusManager.clearFocus() }
                    )
                )

                if (mode != AuthScreenMode.FORGOT_PASSWORD) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password"),
                        label = { Text("Security Password") },
                        placeholder = { Text("6 or more characters") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = if (passwordVisible) "Hide" else "Show",
                                color = Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier
                                    .clickable { passwordVisible = !passwordVisible }
                                    .padding(end = 12.dp)
                            )
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = auraTextFieldColors(),
                        shape = fieldShape,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }

                // Password Recovery trigger link
                if (mode == AuthScreenMode.LOGIN) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable {
                                    viewModel.clearError()
                                    successMessage = null
                                    mode = AuthScreenMode.FORGOT_PASSWORD
                                }
                                .testTag("btn_forgot_password")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary Submit Button
                if (uiState is AuthUiState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.clearError()
                            successMessage = null
                            when (mode) {
                                AuthScreenMode.LOGIN -> viewModel.login(email, password)
                                AuthScreenMode.SIGNUP -> viewModel.signUp(email, password, displayName)
                                AuthScreenMode.FORGOT_PASSWORD -> {
                                    viewModel.resetPassword(email) {
                                        successMessage = "Password reset dispatch complete if account exists."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("primary_auth_submit"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        val actText = when (mode) {
                            AuthScreenMode.LOGIN -> "Sign in"
                            AuthScreenMode.SIGNUP -> "Create account"
                            AuthScreenMode.FORGOT_PASSWORD -> "Reset password"
                        }
                        Text(
                            text = actText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Mode Toggle Navigation links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    when (mode) {
                        AuthScreenMode.LOGIN -> {
                            Text(
                                text = "New to AURA? ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Sign up",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.clearError()
                                        successMessage = null
                                        mode = AuthScreenMode.SIGNUP
                                    }
                                    .testTag("link_register")
                            )
                        }
                        AuthScreenMode.SIGNUP -> {
                            Text(
                                text = "Have an account? ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Sign in",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.clearError()
                                        successMessage = null
                                        mode = AuthScreenMode.LOGIN
                                    }
                                    .testTag("link_login")
                            )
                        }
                        AuthScreenMode.FORGOT_PASSWORD -> {
                            Text(
                                text = "Back to ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Sign in",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.clearError()
                                        successMessage = null
                                        mode = AuthScreenMode.LOGIN
                                    }
                                    .testTag("link_back_to_login")
                            )
                        }
                    }
                }

                if (showGoogleSignIn) {
                    Spacer(modifier = Modifier.height(36.dp))

                    // Custom Clean Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Continue with Google Secondary Button
                    Button(
                        onClick = {
                            if (!inPreview) {
                                focusManager.clearFocus()
                                viewModel.clearError()
                                successMessage = null
                                scope.launch {
                                    try {
                                        googleSignInClient.signOut()
                                    } catch (_: Exception) {}
                                    val signInIntent = googleSignInClient.signInIntent
                                    googleSignInLauncher.launch(signInIntent)
                                }
                            }
                        },
                        enabled = !inPreview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_auth_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inPreview) Color(0xFF101010).copy(alpha = 0.5f) else Color(0xFF101010),
                            contentColor = if (inPreview) Color.White.copy(alpha = 0.4f) else Color.White,
                            disabledContainerColor = Color(0xFF101010).copy(alpha = 0.3f),
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(
                            1.dp, 
                            if (inPreview) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                ),
                                color = if (inPreview) Color.White.copy(alpha = 0.3f) else Color.White,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.sp
                                )
                            )
                        }
                    }

                    if (inPreview) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Google Sign-In is available only in the installed app build.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                // Spacer cushion to elevate the login form slightly higher on screen
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun auraTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color.White.copy(alpha = 0.7f),
    unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
    focusedBorderColor = Color.White.copy(alpha = 0.4f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
    focusedContainerColor = Color(0xFF101010),
    unfocusedContainerColor = Color(0xFF101010),
    cursorColor = Color.White,
    errorBorderColor = Color(0xFFFF5252),
    errorTextColor = Color.White,
    errorLabelColor = Color(0xFFFF5252),
    errorContainerColor = Color(0xFF101010)
)

@Composable
fun Modifier.maxSizeWidthModifier(): Modifier {
    return this.widthIn(max = 460.dp)
}

private fun isRunningInPreview(): Boolean {
    val fingerprint = android.os.Build.FINGERPRINT ?: ""
    val model = android.os.Build.MODEL ?: ""
    val brand = android.os.Build.BRAND ?: ""
    val device = android.os.Build.DEVICE ?: ""
    val product = android.os.Build.PRODUCT ?: ""
    val hardware = android.os.Build.HARDWARE ?: ""
    
    return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            model.contains("google_sdk") ||
            model.contains("Emulator") ||
            model.contains("Android SDK built for x86") ||
            brand.startsWith("generic") && device.startsWith("generic") ||
            "google_sdk" == product ||
            product.contains("sdk_gphone") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu")
}
