package why.xee.kidsview.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import why.xee.kidsview.ui.components.ElegantAlertDialog

/**
 * Update Dialog Component
 * Shows when an app update is available
 */
@Composable
fun UpdateDialog(
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElegantAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Update Available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "A new version of KidsView is available!",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Update now to get the latest features and improvements.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        },
        modifier = modifier
    )
}

/**
 * Update Button Component
 * Opens Play Store listing for the app
 */
@Composable
fun UpdateButton(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Button(
        onClick = {
            openPlayStoreListing(context)
        },
        modifier = modifier
    ) {
        Icon(
            Icons.Default.SystemUpdate,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open Play Store")
    }
}

private fun openPlayStoreListing(context: android.content.Context) {
    try {
        val packageName = "why.xee.kidsview"
        // Try to open Play Store app first
        val playStoreIntent = Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName"))
        try {
            context.startActivity(playStoreIntent)
        } catch (e: android.content.ActivityNotFoundException) {
            // If Play Store app is not available, open in browser
            val browserIntent = Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

