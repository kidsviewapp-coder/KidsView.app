package why.xee.kidsview.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import why.xee.kidsview.data.preferences.AuthMode
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.utils.ReviewerUnlockManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialSetupDialog(
    preferencesManager: PreferencesManager,
    onDismiss: () -> Unit,
    onSetupComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var authMode by remember { mutableStateOf(AuthMode.PIN) }
    
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    
    // Track if reviewer code was detected to exit setup immediately
    var reviewerCodeDetected by remember { mutableStateOf(false) }
    
    // Exit setup immediately when reviewer code is detected
    LaunchedEffect(reviewerCodeDetected) {
        if (reviewerCodeDetected) {
            ReviewerUnlockManager.activate()
            onSetupComplete()
        }
    }
    
    val predefinedQuestions = listOf(
        "What was the name of your first pet?",
        "What city were you born in?",
        "What was your mother's maiden name?",
        "What was the name of your elementary school?",
        "What is your favorite childhood friend's name?",
        "What was your childhood nickname?",
        "What is the name of your favorite teacher?",
        "What street did you grow up on?",
        "What was your favorite food as a child?",
        "What was the make of your first car?"
    )
    
    var question1 by remember { mutableStateOf("") }
    var answer1 by remember { mutableStateOf("") }
    var question2 by remember { mutableStateOf("") }
    var answer2 by remember { mutableStateOf("") }
    var question3 by remember { mutableStateOf("") }
    var answer3 by remember { mutableStateOf("") }
    var questionsError by remember { mutableStateOf<String?>(null) }
    var questionStep by remember { mutableStateOf(1) } // 1, 2, or 3
    
    var expanded1 by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }
    var expanded3 by remember { mutableStateOf(false) }
    
    val passwordRequirements = """
        Password Requirements:
        • Minimum 8 characters
        • At least one numeric digit (0-9)
        • At least one special character (@#$%&*!)
    """.trimIndent()
    
    Dialog(
        onDismissRequest = { }, // Cannot dismiss during setup
        properties = DialogProperties(
            dismissOnBackPress = false, // Cannot dismiss during setup
            dismissOnClickOutside = false, // Cannot dismiss during setup
            usePlatformDefaultWidth = false
        )
    ) {
        // Semi-transparent backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dialog card - full width, wraps content height with max constraint
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = 600.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 20.dp,
                    pressedElevation = 24.dp
                )
            ) {
                // Glassmorphism gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header with title and close button (disabled during setup)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentStep == 0) "Initial Setup Required" else "Security Questions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            // Close button (disabled during setup, but shown for consistency)
                            IconButton(
                                onClick = { }, // Disabled during setup
                                modifier = Modifier.size(40.dp),
                                enabled = false
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                        
                        // Content that sizes to content (scrollable only if needed)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (currentStep == 0) {
                                Text(
                                    text = "Parent Mode requires authentication. Please set up your PIN or Password.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = authMode == AuthMode.PIN,
                                        onClick = { authMode = AuthMode.PIN },
                                        label = { Text("PIN") }
                                    )
                                    FilterChip(
                                        selected = authMode == AuthMode.PASSWORD,
                                        onClick = { authMode = AuthMode.PASSWORD },
                                        label = { Text("Password") }
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (authMode == AuthMode.PIN) {
                                    OutlinedTextField(
                                        value = pin,
                                        onValueChange = { newValue ->
                                            pin = newValue.filter { char -> char.isDigit() }.take(6)
                                            authError = null
                                            
                                            // Check for reviewer passkey immediately - exit setup flow if detected
                                            if (ReviewerUnlockManager.isReviewerPasskey(pin)) {
                                                reviewerCodeDetected = true
                                            }
                                        },
                                        label = { Text("PIN (6 digits)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = confirmPin,
                                        onValueChange = { newValue ->
                                            confirmPin = newValue.filter { char -> char.isDigit() }.take(6)
                                            authError = null
                                            
                                            // Check for reviewer passkey immediately - exit setup flow if detected
                                            if (ReviewerUnlockManager.isReviewerPasskey(confirmPin)) {
                                                reviewerCodeDetected = true
                                            }
                                        },
                                        label = { Text("Confirm PIN") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Text(
                                            text = passwordRequirements,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { newValue ->
                                            password = newValue
                                            authError = null
                                            
                                            // Check for reviewer password immediately - exit setup flow if detected
                                            if (ReviewerUnlockManager.isReviewerPassword(newValue)) {
                                                reviewerCodeDetected = true
                                            }
                                        },
                                        label = { Text("Password") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        visualTransformation = if (passwordVisible) {
                                            VisualTransformation.None
                                        } else {
                                            PasswordVisualTransformation()
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                                )
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { newValue ->
                                            confirmPassword = newValue
                                            authError = null
                                            
                                            // Check for reviewer password immediately - exit setup flow if detected
                                            if (ReviewerUnlockManager.isReviewerPassword(newValue)) {
                                                reviewerCodeDetected = true
                                            }
                                        },
                                        label = { Text("Confirm Password") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        visualTransformation = if (confirmPasswordVisible) {
                                            VisualTransformation.None
                                        } else {
                                            PasswordVisualTransformation()
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                                )
                                            }
                                        }
                                    )
                                }
                                
                                if (authError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        authError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                // Step-by-step security questions (one at a time)
                                Text(
                                    text = "Set up security question ${questionStep} of 3",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Text(
                                    text = "All 3 answers must be correct to recover your PIN/Password if forgotten.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                // Progress indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    repeat(3) { index ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .background(
                                                    color = if (index + 1 <= questionStep)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Show only the current question
                                when (questionStep) {
                                    1 -> {
                                        Text("Question 1:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ExposedDropdownMenuBox(
                                            expanded = expanded1,
                                            onExpandedChange = { expanded1 = !expanded1 }
                                        ) {
                                            OutlinedTextField(
                                                value = question1,
                                                onValueChange = { },
                                                readOnly = true,
                                                label = { Text("Select Question 1") },
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded1)
                                                },
                                                modifier = Modifier
                                                    .menuAnchor()
                                                    .fillMaxWidth(),
                                                singleLine = true
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded1,
                                                onDismissRequest = { expanded1 = false }
                                            ) {
                                                predefinedQuestions.forEach { question ->
                                                    DropdownMenuItem(
                                                        text = { Text(question) },
                                                        onClick = {
                                                            question1 = question
                                                            expanded1 = false
                                                            questionsError = null
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = answer1,
                                            onValueChange = { answer1 = it; questionsError = null },
                                            label = { Text("Answer") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                    2 -> {
                                        Text("Question 2:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ExposedDropdownMenuBox(
                                            expanded = expanded2,
                                            onExpandedChange = { expanded2 = !expanded2 }
                                        ) {
                                            OutlinedTextField(
                                                value = question2,
                                                onValueChange = { },
                                                readOnly = true,
                                                label = { Text("Select Question 2") },
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded2)
                                                },
                                                modifier = Modifier
                                                    .menuAnchor()
                                                    .fillMaxWidth(),
                                                singleLine = true
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded2,
                                                onDismissRequest = { expanded2 = false }
                                            ) {
                                                predefinedQuestions.forEach { question ->
                                                    DropdownMenuItem(
                                                        text = { Text(question) },
                                                        onClick = {
                                                            question2 = question
                                                            expanded2 = false
                                                            questionsError = null
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = answer2,
                                            onValueChange = { answer2 = it; questionsError = null },
                                            label = { Text("Answer") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                    3 -> {
                                        Text("Question 3:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ExposedDropdownMenuBox(
                                            expanded = expanded3,
                                            onExpandedChange = { expanded3 = !expanded3 }
                                        ) {
                                            OutlinedTextField(
                                                value = question3,
                                                onValueChange = { },
                                                readOnly = true,
                                                label = { Text("Select Question 3") },
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded3)
                                                },
                                                modifier = Modifier
                                                    .menuAnchor()
                                                    .fillMaxWidth(),
                                                singleLine = true
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded3,
                                                onDismissRequest = { expanded3 = false }
                                            ) {
                                                predefinedQuestions.forEach { question ->
                                                    DropdownMenuItem(
                                                        text = { Text(question) },
                                                        onClick = {
                                                            question3 = question
                                                            expanded3 = false
                                                            questionsError = null
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = answer3,
                                            onValueChange = { answer3 = it; questionsError = null },
                                            label = { Text("Answer") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                                
                                if (questionsError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        questionsError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        
                        // Action button at bottom
                        Button(
                            onClick = {
                                if (currentStep == 0) {
                                    // Check for reviewer passkey (PIN mode) or reviewer password (password mode)
                                    // This is a safety check - code should already be detected in onValueChange
                                    val codeDetected = if (authMode == AuthMode.PASSWORD) {
                                        ReviewerUnlockManager.isReviewerPassword(password) || ReviewerUnlockManager.isReviewerPassword(confirmPassword)
                                    } else {
                                        // PIN mode - check for reviewer passkey
                                        ReviewerUnlockManager.isReviewerPasskey(pin) || ReviewerUnlockManager.isReviewerPasskey(confirmPin)
                                    }
                                    
                                    if (codeDetected) {
                                        // Reviewer passkey or password detected - activate reviewer mode and immediately exit setup
                                        // Do not store the code, do not show security questions
                                        reviewerCodeDetected = true
                                        return@Button
                                    }
                                    
                                    if (authMode == AuthMode.PIN) {
                                        when {
                                            pin.length < 6 -> authError = "PIN must be 6 digits"
                                            pin != confirmPin -> authError = "PINs do not match"
                                            !preferencesManager.setParentPin(pin) -> authError = "Invalid PIN. Please choose a different one."
                                            else -> {
                                                preferencesManager.setAuthMode(AuthMode.PIN)
                                                currentStep = 1
                                            }
                                        }
                                    } else {
                                        when {
                                            password != confirmPassword -> authError = "Passwords do not match"
                                            else -> {
                                                val (isValid, validationError) = preferencesManager.validatePasswordRequirements(password)
                                                if (isValid) {
                                                    if (preferencesManager.setParentPassword(password)) {
                                                        preferencesManager.setAuthMode(AuthMode.PASSWORD)
                                                        currentStep = 1
                                                    } else {
                                                        authError = "Failed to save password. Please try again."
                                                    }
                                                } else {
                                                    authError = validationError ?: "Invalid password"
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Security questions step - check reviewer unlock (should not be reachable if code was entered)
                                    // This is a safety check in case user somehow reaches this step
                                    if (ReviewerUnlockManager.isUnlocked() || reviewerCodeDetected) {
                                        // Reviewer mode already active - exit immediately
                                        // Do not show security questions, do not store anything
                                        onSetupComplete()
                                        return@Button
                                    }
                                    
                                    // Handle step-by-step security questions
                                    when {
                                        questionStep == 1 -> {
                                            // Validate Question 1
                                            when {
                                                question1.isBlank() -> questionsError = "Please select a question"
                                                answer1.isBlank() -> questionsError = "Please provide an answer"
                                                else -> {
                                                    questionsError = null
                                                    questionStep = 2
                                                }
                                            }
                                        }
                                        questionStep == 2 -> {
                                            // Validate Question 2
                                            when {
                                                question2.isBlank() -> questionsError = "Please select a question"
                                                answer2.isBlank() -> questionsError = "Please provide an answer"
                                                question2 == question1 -> questionsError = "This question is already used. Please select a different one."
                                                else -> {
                                                    questionsError = null
                                                    questionStep = 3
                                                }
                                            }
                                        }
                                        questionStep == 3 -> {
                                            // Validate Question 3 and complete setup
                                            when {
                                                question3.isBlank() -> questionsError = "Please select a question"
                                                answer3.isBlank() -> questionsError = "Please provide an answer"
                                                question3 == question1 || question3 == question2 -> questionsError = "This question is already used. Please select a different one."
                                                else -> {
                                                    preferencesManager.setSecurityQuestion(1, question1, answer1)
                                                    preferencesManager.setSecurityQuestion(2, question2, answer2)
                                                    preferencesManager.setSecurityQuestion(3, question3, answer3)
                                                    onSetupComplete()
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when {
                                    currentStep == 0 -> "Next"
                                    questionStep == 1 || questionStep == 2 -> "Next"
                                    else -> "Complete Setup"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
