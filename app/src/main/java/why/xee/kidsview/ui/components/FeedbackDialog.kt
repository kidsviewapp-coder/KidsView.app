package why.xee.kidsview.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import why.xee.kidsview.ui.components.ElegantAlertDialog

/**
 * Feedback Dialog Component with Star Rating
 * Shows star rating, and when submitted, opens Play Store for rating/review
 */
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedRating by remember { mutableStateOf(0) }
    
    ElegantAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Feedback,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Rate Your Experience",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "How would you rate KidsView?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Star Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= selectedRating) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "$i stars",
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { selectedRating = i }
                                .padding(4.dp),
                            tint = if (i <= selectedRating) 
                                Color(0xFFFFD700) // Gold color for selected stars
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                
                if (selectedRating > 0) {
                    Text(
                        text = when (selectedRating) {
                            1 -> "Poor"
                            2 -> "Fair"
                            3 -> "Good"
                            4 -> "Very Good"
                            5 -> "Excellent"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Open Play Store when user submits rating
                    openPlayStoreForFeedback(context)
                    onDismiss()
                },
                enabled = selectedRating > 0
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit & Rate on Play Store")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

private fun openPlayStoreForFeedback(context: android.content.Context) {
    try {
        val packageName = "why.xee.kidsview"
        // Try to open Play Store app first
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        try {
            context.startActivity(playStoreIntent)
        } catch (e: android.content.ActivityNotFoundException) {
            // If Play Store app is not available, open in browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

