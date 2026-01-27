package why.xee.kidsview.ui.kidsmode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import why.xee.kidsview.utils.DeviceUtils
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.ui.components.AnimatedSuccessMessage
import why.xee.kidsview.ui.viewmodel.KidsListViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Kids List Screen - Display approved videos from Firestore
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidsListScreen(
    onNavigateBack: () -> Unit,
    onVideoSelected: (String) -> Unit, // videoId
    onNavigateToUnlock: () -> Unit = {}, // Navigate to unlock screen
    onNavigateToVideoRequest: () -> Unit = {}, // Navigate to video request screen
    onNavigateToParentMode: () -> Unit = {}, // Navigate to parent mode
    viewModel: KidsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isAppLocked = uiState.isAppLocked
    var showSuccess by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    
    // Show success when app is unlocked
    LaunchedEffect(uiState.isAppLocked) {
        if (!uiState.isAppLocked) {
            successMessage = "App unlocked successfully!"
            showSuccess = true
            // Auto-dismiss after 3 seconds
            kotlinx.coroutines.delay(3000)
            showSuccess = false
            successMessage = ""
        }
    }
    
    // Intercept back button when app is locked
    BackHandler(enabled = isAppLocked) {
        // Do nothing - prevent back navigation when locked
        // The back button will be hidden in the UI, but this prevents system back
    }
    
    // Update lock status and timer when it changes
    LaunchedEffect(Unit) {
        viewModel.updateLockStatus()
    }
    
    // Refresh timer settings periodically to catch changes from parent settings
    // Refresh videos every 1 minute (60000ms) instead of every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshTimerSettings()
            // Only refresh timer settings frequently, not videos
            kotlinx.coroutines.delay(5000) // Check timer settings every 5 seconds
        }
    }
    
    // Refresh videos every 1 minute
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refresh() // Refresh videos and rejected requests count
            kotlinx.coroutines.delay(60000) // Refresh every 1 minute (60000ms)
        }
    }
    
    // Refresh rejected requests count when navigating to request screen
    LaunchedEffect(Unit) {
        viewModel.refresh() // Initial load includes rejected count
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Success Message Overlay
        AnimatedSuccessMessage(
            message = successMessage,
            visible = showSuccess,
            onDismiss = { showSuccess = false },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
        
        Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { 
                    Column {
                        Text(
                            text = "Approved Videos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.videos.isNotEmpty()) {
                            Text(
                                text = "${uiState.videos.size} video${if (uiState.videos.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Countdown timer display
                        if (uiState.isTimeLimitEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val hours = (uiState.remainingTimeMs / (60 * 60 * 1000)).toInt()
                            val minutes = ((uiState.remainingTimeMs % (60 * 60 * 1000)) / (60 * 1000)).toInt()
                            val seconds = ((uiState.remainingTimeMs % (60 * 1000)) / 1000).toInt()
                            val timeText = if (hours > 0) {
                                String.format("%02d:%02d:%02d", hours, minutes, seconds)
                            } else {
                                String.format("%02d:%02d", minutes, seconds)
                            }
                            Text(
                                text = "⏰ $timeText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.remainingTimeMs < 5 * 60 * 1000) { // Less than 5 minutes
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (!isAppLocked) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Request Video button with notification badge
                    Box {
                        IconButton(
                            onClick = { onNavigateToVideoRequest() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add, // Use Add icon for requesting
                                contentDescription = "Request Video",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        // Notification badge for rejected requests
                        if (uiState.rejectedRequestsCount > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = if (uiState.rejectedRequestsCount > 99) "99+" else uiState.rejectedRequestsCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    
                    if (uiState.videos.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.refresh() },
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // Menu button
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .clickable(enabled = showMenu) { 
                    if (showMenu) {
                        showMenu = false
                    }
                }
        ) {
            when {
                uiState.isLoading && uiState.videos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                uiState.error != null && uiState.videos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "😕",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = uiState.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = { viewModel.refresh() }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                uiState.videos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "📺",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = "No Videos Yet",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Parents need to add videos first",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = DeviceUtils.getAdaptiveGridColumns(),
                        contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.videos) { video ->
                            val isPendingDelete = uiState.pendingDeleteVideoIds.contains(video.videoId)
                            ApprovedVideoCard(
                                video = video,
                                onClick = {
                                    // Directly open player in Kids Mode (no ads)
                                    onVideoSelected(video.videoId)
                                },
                                isPendingDelete = isPendingDelete
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Custom menu overlay - matching parent mode style, positioned on RIGHT side
    // Placed outside Scaffold to ensure proper screen-level alignment
    if (showMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f)
        ) {
            // Background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { showMenu = false }
            )
            // Menu card - positioned on the RIGHT side, aligned with menu button
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(280.dp)
                    .padding(top = 56.dp, end = 8.dp)
                    .zIndex(1000f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    TextButton(
                        onClick = { 
                            showMenu = false
                            onNavigateToParentMode()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings, 
                                null, 
                                modifier = Modifier.size(28.dp), 
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Parent Mode", 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    TextButton(
                        onClick = { 
                            showMenu = false
                            if (isAppLocked) {
                                // Unlock - navigate to PIN entry
                                onNavigateToUnlock()
                            } else {
                                // Lock - just lock the app
                                viewModel.setAppLocked(true)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isAppLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                null,
                                modifier = Modifier.size(28.dp),
                                tint = if (isAppLocked) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (isAppLocked) "Unlock App" else "Lock App",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAppLocked) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
    } // Close outer Box
}

/**
 * Video card for approved videos
 */
@Composable
private fun ApprovedVideoCard(
    video: FirestoreVideo,
    onClick: () -> Unit,
    isPendingDelete: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPendingDelete, onClick = onClick)
            .alpha(if (isPendingDelete) 0.5f else 1f), // Grey out if pending delete
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPendingDelete) {
                MaterialTheme.colorScheme.surfaceVariant // Grey background for pending delete
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                AsyncImage(
                    model = video.thumbnail,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Play icon overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                // YouTube Branding (Required by YouTube API Terms of Service)
                Text(
                    text = "YouTube",
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
                
                // Duration badge
                if (video.duration.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 4.dp
                    ) {
                        Text(
                            text = video.duration,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            
            // Video info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isPendingDelete) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (video.channel.isNotEmpty()) {
                        Text(
                            text = video.channel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (isPendingDelete) 0.5f else 1f
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Show pending delete indicator
                    if (isPendingDelete) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "⏳ Pending deletion",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
        }
    }
}

