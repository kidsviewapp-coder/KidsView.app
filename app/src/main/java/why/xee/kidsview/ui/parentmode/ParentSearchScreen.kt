package why.xee.kidsview.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.utils.AdManager
import why.xee.kidsview.ui.components.BannerAd
import why.xee.kidsview.ui.components.VideoCard
import why.xee.kidsview.ui.components.VideoCardWithFavorites
import why.xee.kidsview.ui.components.UpdateButtonWithBlink
import why.xee.kidsview.ui.components.InstructionsDialog
import why.xee.kidsview.ui.viewmodel.ParentSearchViewModel
import why.xee.kidsview.ui.viewmodel.VideoRequestsViewModel
import why.xee.kidsview.ui.viewmodel.FavoritesViewModel
import androidx.compose.ui.platform.LocalContext

/**
 * Parent Search Screen - Search for YouTube videos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSearchScreen(
    onNavigateBack: () -> Unit,
    onVideoSelected: (String) -> Unit, // videoId
    onNavigateToCategoryManagement: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToVideoRequests: () -> Unit = {},
    onNavigateToCartoonBrowse: () -> Unit = {},
    onNavigateToMyVideos: () -> Unit = {},
    onNavigateToCategoryVideos: (String, String) -> Unit = { _, _ -> },
    viewModel: ParentSearchViewModel = hiltViewModel(),
    videoRequestsViewModel: VideoRequestsViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val requestsUiState by videoRequestsViewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Prevent scroll state changes when menu opens
    LaunchedEffect(showMenu) {
        // Keep scroll state stable when menu opens/closes
    }
    
    // Preload ads when screen is opened
    LaunchedEffect(Unit) {
        videoRequestsViewModel.loadRequests()
        AdManager.getInstance().loadInterstitialAd(context, true)
        AdManager.getInstance().loadRewardedAd(context, true)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SearchBarTextField(
                        query = uiState.searchQuery,
                        onQueryChange = { query: String -> viewModel.updateSearchQuery(query) },
                        onSearch = { viewModel.searchVideos() },
                        onClear = { viewModel.clearSearch() }
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                modifier = Modifier.size(28.dp)
                            )
                            // Notification dot for pending requests
                            if (requestsUiState.pendingRequestCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(12.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.error,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
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
        ) {
            // Scrollable content with bottom padding for fixed banner ad
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 60.dp) // Extra padding for banner ad
            ) {
            // Cartoon Browser Quick Access Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { onNavigateToCartoonBrowse() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        Text(
                            text = "🎬",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Column {
                            Text(
                                text = "Cartoon Browser",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Browse cartoons by country & genre",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Favorites Category Quick Access Card
            val favoritesCategory = uiState.savedVideosByCategory.keys.find { it?.categoryId == "favorites" }
            val favoritesCount = favoritesCategory?.let { uiState.savedVideosByCategory[it]?.size ?: 0 } ?: 0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { 
                        // Navigate to Favorites category
                        onNavigateToCategoryVideos("favorites", "Favorites")
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        Text(
                            text = "❤️",
                            style = MaterialTheme.typography.displaySmall
                        )
                        Column {
                            Text(
                                text = "Favorites",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = if (favoritesCount > 0) "$favoritesCount favorited videos" else "Your favorite videos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Update Button (shows when update is available)
            val activity = context as? android.app.Activity
            if (activity != null) {
                UpdateButtonWithBlink(
                    activity = activity,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Main content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "😕",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = uiState.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Button(
                                onClick = { viewModel.clearError() }
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
                uiState.searchResults.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "📺",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Try a different search query",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.searchResults.isNotEmpty() && uiState.searchQuery.isNotEmpty() -> {
                    // Show search results when there's a search query
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.searchResults) { video ->
                            VideoCardWithFavorites(
                                video = video,
                                onClick = {
                                    // Parent Mode: show interstitial ad before navigating (cooldown enforced)
                                    AdManager.getInstance().showInterstitialAd(
                                        context,
                                        true,
                                        false
                                    ) {
                                        onVideoSelected(video.id)
                                    }
                                },
                                favoritesViewModel = favoritesViewModel
                            )
                        }
                    }
                }
                uiState.searchResults.isEmpty() && uiState.searchQuery.isEmpty() -> {
                    // Show empty state when no search or cartoon selection
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
                                text = "🔍",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = "Search for Videos",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Search for YouTube videos or browse cartoons by country and genre above",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { onNavigateToMyVideos() }
                            ) {
                                Text("View My Saved Videos →")
                            }
                        }
                    }
                }
                uiState.searchResults.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "📺",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Try a different search query",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    // Fallback: show empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🔍",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = "Search for Videos",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Search for YouTube videos or browse cartoons by country and genre",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
            } // Close Box that started at line 378
            } // Close Column that started at line 253
            
            // Fixed Banner Ad at bottom of screen (always visible in Parent Mode)
            BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                isParentMode = true
            )
            
            // Custom menu overlay - completely isolated, no layout impact
            if (showMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { showMenu = false }
                        .zIndex(999f)
                )
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
                    Column {
                        TextButton(
                            onClick = { showMenu = false; onNavigateToMyVideos() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("📹", style = MaterialTheme.typography.titleLarge)
                                Text("My Videos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        TextButton(
                            onClick = { showMenu = false; onNavigateToCategoryManagement() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("📁", style = MaterialTheme.typography.titleLarge)
                                Text("Manage Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        TextButton(
                            onClick = { showMenu = false; onNavigateToVideoRequests() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box {
                                    Text("✉️", style = MaterialTheme.typography.titleLarge)
                                    if (requestsUiState.pendingRequestCount > 0) {
                                        Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                                            Text(
                                                if (requestsUiState.pendingRequestCount > 99) "99+" 
                                                else requestsUiState.pendingRequestCount.toString(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                                Text("Video Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        TextButton(
                            onClick = { showMenu = false; onNavigateToSettings() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(Icons.Default.Settings, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Parent Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        TextButton(
                            onClick = { showMenu = false; showInstructionsDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("📖", style = MaterialTheme.typography.titleLarge)
                                Text("How to Use KidsView", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } // Close the outer Box at line 246
        
        // Instructions Dialog
        if (showInstructionsDialog) {
            InstructionsDialog(
                onDismiss = { showInstructionsDialog = false }
            )
        }
    } // Close Scaffold body
} // Close ParentSearchScreen function

/**
 * Search bar component for parent search
 */
@Composable
private fun SearchBarTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text("Search YouTube videos...")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
    
    // Auto-search when query changes (with debounce)
    LaunchedEffect(query) {
        if (query.length >= 3) {
            kotlinx.coroutines.delay(500)
            if (query == query.trim()) {
                onSearch()
            }
        }
    }
}

