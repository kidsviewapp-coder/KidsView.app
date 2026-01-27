package why.xee.kidsview.ui.parentmode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.ui.components.ElegantAlertDialog
import why.xee.kidsview.ui.viewmodel.PinRecoveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinRecoveryScreen(
    onNavigateBack: () -> Unit,
    onPinRecovered: () -> Unit,
    viewModel: PinRecoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var answers by remember { mutableStateOf(listOf("", "", "")) }
    var showResetDialog by remember { mutableStateOf(false) }
    var newAuthInput by remember { mutableStateOf("") }
    var confirmAuthInput by remember { mutableStateOf("") }

    // Show reset dialog when questions are verified, then navigate after reset
    LaunchedEffect(uiState.questionsVerified) {
        if (uiState.questionsVerified && !showResetDialog) {
            showResetDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recover PIN/Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.questionsVerified) {
                // Show current PIN/Password and reset option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "✓ All security questions verified!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Your current ${if (viewModel.getAuthMode() == why.xee.kidsview.data.preferences.AuthMode.PIN) "PIN" else "Password"}: ${uiState.recoveredAuthValue}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset ${if (viewModel.getAuthMode() == why.xee.kidsview.data.preferences.AuthMode.PIN) "PIN" else "Password"}")
                        }
                    }
                }
            } else {
                // Show security questions form
                Text(
                    "Answer all 3 security questions correctly to recover your PIN/Password",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Security Question 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Question 1:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = viewModel.getSecurityQuestion(1) ?: "Question 1 not set",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = answers[0],
                            onValueChange = { answers = answers.toMutableList().apply { set(0, it) } },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Security Question 2
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Question 2:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = viewModel.getSecurityQuestion(2) ?: "Question 2 not set",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = answers[1],
                            onValueChange = { answers = answers.toMutableList().apply { set(1, it) } },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // Security Question 3
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Question 3:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = viewModel.getSecurityQuestion(3) ?: "Question 3 not set",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = answers[2],
                            onValueChange = { answers = answers.toMutableList().apply { set(2, it) } },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                if (uiState.error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.error!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.verifySecurityQuestions(answers)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = answers.all { it.isNotBlank() }
                ) {
                    Text("Verify All Answers")
                }
            }

            if (!viewModel.hasSecurityQuestionsSet()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ Security questions not set. Please set them in Parent Settings first.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Reset PIN/Password Dialog
    if (showResetDialog && uiState.questionsVerified) {
        ResetAuthDialog(
            authMode = viewModel.getAuthMode(),
            onDismiss = { showResetDialog = false },
            onConfirm = { newValue, confirmValue ->
                if (viewModel.resetAuth(newValue, confirmValue)) {
                    showResetDialog = false
                    // Small delay to show success, then navigate
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(500)
                        onPinRecovered()
                    }
                }
            },
            currentError = uiState.resetError
        )
    }
}

@Composable
private fun ResetAuthDialog(
    authMode: why.xee.kidsview.data.preferences.AuthMode,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    currentError: String?
) {
    var newAuth by remember { mutableStateOf("") }
    var confirmAuth by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val isPinMode = authMode == why.xee.kidsview.data.preferences.AuthMode.PIN
    val minLength = if (isPinMode) 6 else 8

    ElegantAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset ${if (isPinMode) "PIN" else "Password"}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Enter your new ${if (isPinMode) "PIN" else "password"}:")
                
                OutlinedTextField(
                    value = newAuth,
                    onValueChange = {
                        if (isPinMode) {
                            newAuth = it.filter { char -> char.isDigit() }.take(6)
                        } else {
                            newAuth = it
                        }
                    },
                    label = { Text("New ${if (isPinMode) "PIN (6 digits)" else "Password (min 8 chars)"}") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword || isPinMode) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = if (!isPinMode) {
                        {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Text(
                                    text = if (showPassword) "👁️" else "👁️‍🗨️",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else null,
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = confirmAuth,
                    onValueChange = {
                        if (isPinMode) {
                            confirmAuth = it.filter { char -> char.isDigit() }.take(6)
                        } else {
                            confirmAuth = it
                        }
                    },
                    label = { Text("Confirm ${if (isPinMode) "PIN" else "Password"}") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword || isPinMode) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = if (!isPinMode) {
                        {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Text(
                                    text = if (showPassword) "👁️" else "👁️‍🗨️",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPinMode) KeyboardType.Number else KeyboardType.Text
                    )
                )
                
                if (currentError != null) {
                    Text(
                        text = currentError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newAuth, confirmAuth) },
                enabled = newAuth.length >= minLength && newAuth == confirmAuth
            ) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

