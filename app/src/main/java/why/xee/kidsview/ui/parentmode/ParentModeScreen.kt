package why.xee.kidsview.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import why.xee.kidsview.data.preferences.AuthMode
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.ui.viewmodel.ParentModeViewModel
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import why.xee.kidsview.utils.ReviewerUnlockManager

/**
 * Parent Mode Authentication Entry Screen (PIN or Password)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentModeScreen(
    onNavigateBack: () -> Unit,
    onPinVerified: (android.content.Context) -> Unit,
    onForgotPin: () -> Unit = {},
    viewModel: ParentModeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val preferencesManager = viewModel.preferencesManager
    
    // Check reviewer unlock first - if active, immediately grant access without showing UI
    LaunchedEffect(Unit) {
        if (ReviewerUnlockManager.isUnlocked()) {
            // Reviewer mode active - immediately grant access and return
            onPinVerified(context)
        }
    }
    
    // If reviewer is unlocked, don't render any UI - access already granted
    if (ReviewerUnlockManager.isUnlocked()) {
        return
    }
    
    val isPinMode = uiState.authMode == AuthMode.PIN
    val displayLength = if (isPinMode) 6 else uiState.enteredInput.length.coerceAtMost(20)
    
    // Check if setup is required - redirect to welcome if not set up
    LaunchedEffect(Unit) {
        if (!preferencesManager.isAuthSet() || !preferencesManager.hasSecurityQuestionsSet()) {
            onNavigateBack()
        }
    }
    
    // Navigate when authentication is verified
    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onPinVerified(context)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text("Parent Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = if (isPinMode) "Enter Parent PIN" else "Enter Parent Password",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = if (isPinMode) 
                        "Please enter your 6-digit PIN" 
                    else 
                        "Please enter your password (minimum 8 characters)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Forgot PIN/Password button
                TextButton(
                    onClick = onForgotPin
                ) {
                    Text("Forgot ${if (isPinMode) "PIN" else "Password"}?")
                }
                
                // PIN/Password Display
                if (isPinMode) {
                    Row(
                        modifier = Modifier.padding(bottom = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        repeat(6) { index ->
                            val isFilled = index < uiState.enteredInput.length
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }
                
                // Error message
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )
                }
                
                // Input Pad - Different for PIN vs Password
                if (isPinMode) {
                    // Number Pad for PIN
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1: 1, 2, 3
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NumberButton("1") { viewModel.addDigit("1") }
                            NumberButton("2") { viewModel.addDigit("2") }
                            NumberButton("3") { viewModel.addDigit("3") }
                        }
                        
                        // Row 2: 4, 5, 6
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NumberButton("4") { viewModel.addDigit("4") }
                            NumberButton("5") { viewModel.addDigit("5") }
                            NumberButton("6") { viewModel.addDigit("6") }
                        }
                        
                        // Row 3: 7, 8, 9
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NumberButton("7") { viewModel.addDigit("7") }
                            NumberButton("8") { viewModel.addDigit("8") }
                            NumberButton("9") { viewModel.addDigit("9") }
                        }
                        
                        // Row 4: Clear, 0, Delete
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NumberButton("C") { viewModel.clearInput() }
                            NumberButton("0") { viewModel.addDigit("0") }
                            NumberButton("⌫") { viewModel.removeDigit() }
                        }
                    }
                } else {
                    // Text input for Password (single field with show/hide support)
                    var passwordText by remember { mutableStateOf(uiState.enteredInput) }
                    var passwordVisible by remember { mutableStateOf(false) }

                    LaunchedEffect(uiState.enteredInput) {
                        passwordText = uiState.enteredInput
                    }

                    OutlinedTextField(
                        value = passwordText,
                        onValueChange = { newValue ->
                            passwordText = newValue
                            viewModel.setPassword(newValue)
                        },
                        label = { Text("Password") },
                        modifier = Modifier
                            .fillMaxWidth(0.9f),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "👁️" else "👁️‍🗨️",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    )

                    // For password, add a verify button
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.verifyAuth() },
                        enabled = uiState.enteredInput.length >= 8,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text("Verify Password")
                    }
                }
            }
            
            
        }
    }
}

@Composable
private fun NumberButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

