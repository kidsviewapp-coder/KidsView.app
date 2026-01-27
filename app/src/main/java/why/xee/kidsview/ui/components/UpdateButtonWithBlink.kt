package why.xee.kidsview.ui.components

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import why.xee.kidsview.utils.UpdateManager

/**
 * Update Button Component with Blinking Text
 * Shows when an update is available in Play Store
 * Update text blinks to draw attention
 * Uses In-App Update API to trigger actual update flow
 */
@Composable
fun UpdateButtonWithBlink(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    var updateAvailable by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(true) }
    
    // Infinite animation for blinking effect
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_alpha"
    )
    
    // Check for updates when component is first composed
    // Re-check periodically to catch updates that become available
    LaunchedEffect(activity) {
        // Initial check
        isChecking = true
        UpdateManager.checkUpdateAvailability { available ->
            updateAvailable = available
            isChecking = false
        }
        
        // Re-check every 30 seconds to catch new updates
        while (true) {
            kotlinx.coroutines.delay(30000) // 30 seconds
            if (!updateAvailable) {
                UpdateManager.checkUpdateAvailability { available ->
                    updateAvailable = available
                }
            }
        }
    }
    
    // Only show button if update is available
    if (!isChecking && updateAvailable) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            onClick = {
                // Use UpdateManager to start the actual in-app update flow
                UpdateManager.startUpdateManually(activity)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = "Update Available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap to update the app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                // Blinking "Update" text
                Text(
                    text = "Update",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alpha),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

