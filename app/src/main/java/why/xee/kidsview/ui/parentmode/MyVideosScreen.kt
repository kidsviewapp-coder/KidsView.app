package why.xee.kidsview.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.utils.AdManager
import why.xee.kidsview.ui.components.BannerAd
import why.xee.kidsview.ui.viewmodel.ParentSearchViewModel
import why.xee.kidsview.ui.viewmodel.FavoritesViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * My Videos Screen - Shows all saved videos grouped by categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVideosScreen(
    onNavigateBack: () -> Unit,
    onVideoSelected: (String) -> Unit, // videoId
    viewModel: ParentSearchViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadSavedVideos()
        AdManager.getInstance().loadInterstitialAd(context, true)
        AdManager.getInstance().loadRewardedAd(context, true)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Videos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
            when {
                uiState.isLoadingSavedVideos -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                uiState.savedVideosByCategory.isEmpty() -> {
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
                                text = "📁",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = "No Videos Yet",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Search for videos and add them to your collection",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        uiState.savedVideosByCategory.forEach { (category, videos) ->
                            // Category header
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = category?.icon ?: "📁",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = category?.name ?: "Uncategorized",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "(${videos.size})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Videos in this category
                            items(videos, key = { it.videoId }) { video ->
                                SavedVideoCard(
                                    video = video,
                                    onClick = {
                                        // Parent Mode: show ad, guarded by AdManager (parent-only)
                                        AdManager.getInstance().showInterstitialAd(context, true, false) {
                                            onVideoSelected(video.videoId)
                                        }
                                    },
                                    onEdit = {
                                        onVideoSelected(video.videoId)
                                    },
                                    onDelete = {
                                        viewModel.deleteVideo(video.videoId)
                                        viewModel.loadSavedVideos() // Refresh after delete
                                    },
                                    favoritesViewModel = favoritesViewModel
                                )
                            }
                        }
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
    }
}

/**
 * Card for displaying saved Firestore videos
 */
@Composable
private fun SavedVideoCard(
    video: FirestoreVideo,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    var isFavorite by remember(video.videoId) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Check favorite status when video changes
    LaunchedEffect(video.videoId) {
        isFavorite = favoritesViewModel.isFavorite(video.videoId)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Box {
                AsyncImage(
                    model = video.thumbnail,
                    contentDescription = video.title,
                    modifier = Modifier
                        .size(120.dp, 68.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // YouTube Branding (Required by YouTube API Terms of Service)
                Text(
                    text = "YouTube",
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }
            
            // Video info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (video.channel.isNotEmpty()) {
                    Text(
                        text = video.channel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (video.duration.isNotEmpty()) {
                    Text(
                        text = "Duration: ${video.duration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { 
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites")
                            }
                        },
                        onClick = {
                            showMenu = false
                            // Optimistically update UI
                            val newState = !isFavorite
                            isFavorite = newState
                            
                            coroutineScope.launch {
                                favoritesViewModel.toggleFavorite(video)
                                delay(350)
                                val actualState = favoritesViewModel.isFavorite(video.videoId)
                                if (actualState != newState) {
                                    isFavorite = actualState
                                }
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

