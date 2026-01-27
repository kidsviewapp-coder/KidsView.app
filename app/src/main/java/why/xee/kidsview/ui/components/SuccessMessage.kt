package why.xee.kidsview.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Animated success message that appears with a transition and auto-dismisses after 2 seconds
 */
@Composable
fun AnimatedSuccessMessage(
    message: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    // Auto-dismiss after 3 seconds
    LaunchedEffect(visible, message) {
        if (visible && message.isNotEmpty()) {
            delay(3000) // Show for 3 seconds
            onDismiss()
        }
    }
    
    // Animate visibility
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(300)
        ),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

