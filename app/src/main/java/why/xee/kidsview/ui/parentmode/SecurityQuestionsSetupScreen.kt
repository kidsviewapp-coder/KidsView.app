package why.xee.kidsview.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.delay
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.ui.viewmodel.ParentSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionsSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: ParentSettingsViewModel = hiltViewModel()
) {
    val preferencesManager = viewModel.preferencesManager
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
    
    var question1 by remember { mutableStateOf(preferencesManager.getSecurityQuestion(1) ?: "") }
    var answer1 by remember { mutableStateOf("") }
    var question2 by remember { mutableStateOf(preferencesManager.getSecurityQuestion(2) ?: "") }
    var answer2 by remember { mutableStateOf("") }
    var question3 by remember { mutableStateOf(preferencesManager.getSecurityQuestion(3) ?: "") }
    var answer3 by remember { mutableStateOf("") }
    
    var error by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var questionStep by remember { mutableStateOf(1) } // 1, 2, or 3
    
    // Navigate back after successful save
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            delay(1500) // Show success message for 1.5 seconds
            onNavigateBack() // Navigate back after saving
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Questions Setup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success message
            if (showSuccessMessage) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Security questions saved successfully!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Set up security question ${questionStep} of 3",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "All 3 answers must be correct to recover your PIN/Password if forgotten.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show only the current question
            when (questionStep) {
                1 -> {
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
                            var expanded1 by remember { mutableStateOf(false) }
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
                                        .fillMaxWidth()
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
                                                error = null
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = answer1,
                                onValueChange = { answer1 = it; error = null },
                                label = { Text("Answer") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
                2 -> {
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
                            var expanded2 by remember { mutableStateOf(false) }
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
                                        .fillMaxWidth()
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
                                                error = null
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = answer2,
                                onValueChange = { answer2 = it; error = null },
                                label = { Text("Answer") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
                3 -> {
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
                            var expanded3 by remember { mutableStateOf(false) }
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
                                        .fillMaxWidth()
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
                                                error = null
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = answer3,
                                onValueChange = { answer3 = it; error = null },
                                label = { Text("Answer") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
            
            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Next/Save Button
            Button(
                onClick = {
                    when {
                        questionStep == 1 -> {
                            // Validate Question 1
                            when {
                                question1.isBlank() -> error = "Please select a question"
                                answer1.isBlank() -> error = "Please provide an answer"
                                else -> {
                                    error = null
                                    questionStep = 2
                                }
                            }
                        }
                        questionStep == 2 -> {
                            // Validate Question 2
                            when {
                                question2.isBlank() -> error = "Please select a question"
                                answer2.isBlank() -> error = "Please provide an answer"
                                question2 == question1 -> error = "This question is already used. Please select a different one."
                                else -> {
                                    error = null
                                    questionStep = 3
                                }
                            }
                        }
                        questionStep == 3 -> {
                            // Validate Question 3 and save
                            when {
                                question3.isBlank() -> error = "Please select a question"
                                answer3.isBlank() -> error = "Please provide an answer"
                                question3 == question1 || question3 == question2 -> error = "This question is already used. Please select a different one."
                                else -> {
                                    preferencesManager.setSecurityQuestion(1, question1, answer1)
                                    preferencesManager.setSecurityQuestion(2, question2, answer2)
                                    preferencesManager.setSecurityQuestion(3, question3, answer3)
                                    error = null
                                    showSuccessMessage = true
                                }
                            }
                        }
                    }
                },
                enabled = when (questionStep) {
                    1 -> question1.isNotBlank() && answer1.isNotBlank()
                    2 -> question2.isNotBlank() && answer2.isNotBlank()
                    3 -> question3.isNotBlank() && answer3.isNotBlank()
                    else -> false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (questionStep == 3) "Save Security Questions" else "Next")
            }
        }
    }
}

