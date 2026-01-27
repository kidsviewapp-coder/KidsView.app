package why.xee.kidsview.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import why.xee.kidsview.data.local.FavoriteVideo
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.ui.components.VideoCard
import why.xee.kidsview.ui.components.ElegantAlertDialog
import why.xee.kidsview.ui.components.BannerAd
import why.xee.kidsview.ui.viewmodel.FavoritesViewModel
import why.xee.kidsview.utils.AdManager

/**
 * Favorites Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Preload ads when screen is opened
    LaunchedEffect(Unit) {
        AdManager.getInstance().loadInterstitialAd(context, true)
        AdManager.getInstance().loadRewardedAd(context, true)
        // Just refresh the favorites collection list to ensure button visibility is correct
        viewModel.refreshVideosInFavoritesCategory()
    }
    
    // Refresh when favorites list changes
    LaunchedEffect(uiState.favorites.size) {
        viewModel.refreshVideosInFavoritesCategory()
    }
    
    // Track previous message states to avoid showing duplicate messages
    var previousSuccessMessage by remember { mutableStateOf<String?>(null) }
    var previousError by remember { mutableStateOf<String?>(null) }
    
    // Show success messages with enhanced styling
    LaunchedEffect(uiState.successMessage) {
        val message = uiState.successMessage
        if (message != null && message != previousSuccessMessage) {
            previousSuccessMessage = message
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                )
            }
        } else if (message == null) {
            previousSuccessMessage = null
        }
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error != null && error != previousError) {
            previousError = error
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Error: $error",
                    duration = SnackbarDuration.Long
                )
            }
        } else if (error == null) {
            previousError = null
        }
    }
    
    Scaffold(
        snackbarHost = { 
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                // Custom success snackbar with icon
                val isSuccess = snackbarData.visuals.message.contains("Added") || 
                               snackbarData.visuals.message.contains("Removed") ||
                               snackbarData.visuals.message.contains("added to kids list")
                
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = if (isSuccess) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.inverseSurface
                    },
                    contentColor = if (isSuccess) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    },
                    actionColor = if (isSuccess) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.inversePrimary
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Favorites ❤️",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Simple direct check - show list if we have any favorites
            if (uiState.favorites.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.favorites,
                        key = { favorite -> favorite.videoId }
                    ) { favorite ->
                        val video = favorite.toVideoItem()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoCard(
                                video = video,
                                onClick = {
                                    // Favorites screen opens videos in parent mode (no time limit)
                                    onNavigateToPlayer(video.id)
                                }
                            )
                            // Action buttons row: Unfavorite and Add to Kids List
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Unfavorite button (heart icon) - always show for all favorites
                                IconButton(
                                    onClick = {
                                        viewModel.toggleFavorite(video)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Remove from favorites",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                // Promote to kids list button - show if video is in favorites collection
                                val isInFavoritesCollection = uiState.videosInFavoritesCategory.contains(video.id)
                                if (isInFavoritesCollection) {
                                    Button(
                                        onClick = {
                                            // Show interstitial ad before promoting to kids list
                                            AdManager.getInstance().showInterstitialAd(context, true, false) {
                                                // After ad is shown (or dismissed), promote video
                                                viewModel.promoteToKidsList(video.id)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add to Kids List")
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (uiState.isLoading && uiState.favorites.isEmpty()) {
                // Show loading only if we have no favorites
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Show empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "❤️",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "No favorites yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the heart icon to add videos to favorites",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Fixed Banner Ad at bottom of screen (always visible in Parent Mode)
            BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                isParentMode = true
            )
        }
        
        // Dialog to ask if user wants to keep video in favorites or delete it
        if (uiState.promotedVideoId != null && uiState.promotedVideoTitle != null) {
            ElegantAlertDialog(
                onDismissRequest = { viewModel.dismissPromoteDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = "Added to Kids List",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Video added to kids list.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Keep in favorites?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.handlePromoteChoice(uiState.promotedVideoId!!, keepInFavorites = false)
                        }
                    ) {
                        Text("Remove")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.handlePromoteChoice(uiState.promotedVideoId!!, keepInFavorites = true)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Keep")
                    }
                }
            )
        }
    }
}

/**
 * Extension function to convert FavoriteVideo to VideoItem
 */
private fun FavoriteVideo.toVideoItem(): VideoItem {
    return VideoItem(
        id = videoId,
        title = title,
        description = description,
        channelTitle = channelTitle,
        thumbnailUrl = thumbnailUrl,
        publishedAt = publishedAt
    )
}

