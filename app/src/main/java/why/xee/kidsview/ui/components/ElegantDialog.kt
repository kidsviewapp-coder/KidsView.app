package why.xee.kidsview.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Elegant dialog wrapper with transparency and glassmorphism effects
 */
@Composable
fun ElegantDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = false
    ),
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        // Semi-transparent backdrop with elegant gradient
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
            // Elegant dialog container with glassmorphism
            Card(
                modifier = modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 16.dp,
                    pressedElevation = 20.dp
                )
            ) {
                // Glassmorphism effect overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                        )
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Elegant AlertDialog wrapper with transparency effects
 */
@Composable
fun ElegantAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = false
    )
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        // Semi-transparent backdrop with elegant gradient
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
            // Elegant dialog card with glassmorphism
            // Use fixed height when text is scrollable to provide bounded constraints for weight()
            Card(
                modifier = modifier
                    .widthIn(max = 400.dp)
                    .then(
                        if (text != null) {
                            Modifier.height(550.dp) // Fixed height for scrollable content
                        } else {
                            Modifier.wrapContentHeight()
                        }
                    )
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
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
                        .fillMaxSize()
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
                    // Manually compose AlertDialog structure since Material3 AlertDialog
                    // doesn't accept nullable composables directly
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Icon
                        if (icon != null) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                icon()
                            }
                        }
                        
                        // Title
                        if (title != null) {
                            title()
                        }
                        
                        // Text - scrollable if needed
                        // Use weight modifier like InstructionsDialog - requires parent Column with fillMaxSize()
                        if (text != null) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(scrollState)
                            ) {
                                text()
                            }
                        }
                        
                        // Buttons row - responsive layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (dismissButton != null) {
                                dismissButton()
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            if (confirmButton != null) {
                                confirmButton()
                            }
                        }
                    }
                }
            }
        }
    }
}

