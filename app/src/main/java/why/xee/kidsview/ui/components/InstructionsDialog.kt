package why.xee.kidsview.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Instructions Dialog showing how to operate the app
 * Centered, half height, full width with close button
 */
@Composable
fun InstructionsDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
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
            // Dialog card - full width, half height, centered
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header with title and close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "How to Use KidsView",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Scrollable content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            InstructionsContent()
                        }
                        
                        // Close button at bottom
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Got it!")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Instructions content that can be reused in Privacy Policy screen
 */
@Composable
fun InstructionsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Getting Started
        InstructionSection(
            title = "Getting Started",
            items = listOf(
                "Welcome to KidsView! This app provides a safe, controlled environment for children to watch YouTube videos.",
                "On the welcome screen, you can choose between Parent Mode and Kids Mode.",
                "Parent Mode requires authentication (PIN or Password) to access settings and manage content.",
                "Kids Mode provides a simplified interface for children to watch approved videos."
            )
        )
        
        // Parent Mode Instructions
        InstructionSection(
            title = "Parent Mode Features",
            items = listOf(
                "Search & Add Videos: Use the search function to find YouTube videos. Preview and add them to your child's approved list. Interstitial ads may appear when saving videos.",
                "Manage Categories: Organize videos into custom categories for easy browsing.",
                "Favorites: Mark videos as favorites while searching to save them for later. Access favorites from the Favorites category. Interstitial ads may appear when promoting to kids mode.",
                "Video Requests: Review and approve/deny video requests from your child.",
                "Time Limits: Set daily time limits for screen time (1 hour default, up to 2 hours maximum). Quick selection dropdown and custom input available.",
                "Settings: Configure PIN/Password (numeric keyboard for faster entry), security questions, and other parental controls.",
                "Feedback: Rate the app using the star rating system in About screen. Your rating automatically opens Play Store for review.",
                "Updates: App automatically checks for updates on start and resume. Updates install automatically when downloaded."
            )
        )
        
        // Kids Mode Instructions
        InstructionSection(
            title = "Kids Mode Features",
            items = listOf(
                "Browse Videos: Kids can browse approved videos organized by categories.",
                "Watch Videos: Tap any video to start watching. Videos play in a safe, ad-free player.",
                "Video Controls: Tap the screen while watching to show play/pause, next, and previous buttons. Controls auto-hide after 2 seconds.",
                "Menu Access: Tap the three-dot menu (top right) to access Parent Mode or Lock/Unlock App.",
                "Request Videos: Kids can request to add new videos or remove existing ones. Parents review these requests.",
                "Time Tracking: The app tracks viewing time and enforces daily limits set by parents.",
                "Auto-Skip: If a video is not compatible, the app automatically skips to the next video."
            )
        )
        
        // Time Management
        InstructionSection(
            title = "Time Management & Reset",
            items = listOf(
                "Default Watch Time: The default daily watch time is 1 hour (60 minutes). Maximum effective time is 2 hours (120 minutes).",
                "Time Selection: Use quick selection dropdown (1 hour, 1.5 hours, 2 hours) or custom minutes input (60-120 minutes).",
                "Wallet System: Watch rewarded ads to earn 15 minutes per ad. Wallet can accumulate up to 3 hours (180 minutes) independently.",
                "Manual Application: Wallet time must be manually applied to increase daily watch-time. Applied time is deducted from wallet.",
                "Daily Reset: App time, wallet, and applied time reset automatically at 12:00am (midnight) every day.",
                "Reset After Usage: When reaching 2 hours of used time, watch 3 consecutive ads to reset daily watch-time to 2 hours.",
                "Time Tracking: Timer only counts when videos are playing. Display shows: 'Used today: HH:MM / HH:MM | Wallet: X min'.",
                "Time limits are enforced automatically. When the limit is reached, kids cannot watch more videos until reset (midnight or 3 consecutive ads)."
            )
        )
        
        // Important Notes
        InstructionSection(
            title = "Important Notes",
            items = listOf(
                "All videos must be approved by parents before children can watch them.",
                "Videos requested for deletion are greyed out in kids mode until parent approves the deletion.",
                "Incompatible videos are automatically skipped and removed from your collection.",
                "The app requires internet connection to search and play YouTube videos.",
                "For privacy and safety, all data is stored securely and per-user.",
                "PIN entry uses numeric keyboard for faster and more accurate input.",
                "App updates install automatically when downloaded - no manual restart needed."
            )
        )
        
        // Tips
        InstructionSection(
            title = "Tips for Parents",
            items = listOf(
                "Regularly review video requests to keep content appropriate.",
                "Use categories to organize content by theme, age group, or educational value.",
                "Set reasonable time limits based on your child's age and needs.",
                "Check the Privacy Policy section for detailed information about data handling.",
                "You can access these instructions anytime from Parent Mode > Settings > Privacy Policy."
            )
        )
    }
}

@Composable
private fun InstructionSection(
    title: String,
    items: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

