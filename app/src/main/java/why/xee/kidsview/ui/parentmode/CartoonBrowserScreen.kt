package why.xee.kidsview.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import why.xee.kidsview.data.model.CartoonData
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.ui.components.VideoCard
import why.xee.kidsview.ui.components.VideoCardWithFavorites
import why.xee.kidsview.ui.viewmodel.FavoritesViewModel
import why.xee.kidsview.ui.viewmodel.BrowseMode
import why.xee.kidsview.ui.viewmodel.CartoonBrowserViewModel
import why.xee.kidsview.ui.viewmodel.NavigationLevel
import why.xee.kidsview.ui.components.BannerAd
import why.xee.kidsview.utils.AdManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Cartoon Browser Screen - Browse cartoons by country and genre or simple list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartoonBrowserScreen(
    onNavigateBack: () -> Unit,
    onVideoSelected: (String) -> Unit, // videoId
    viewModel: CartoonBrowserViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoritesUiState by favoritesViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Track previous message states to avoid showing duplicate messages
    var previousSuccessMessage by remember { mutableStateOf<String?>(null) }
    
    // Show success messages from FavoritesViewModel
    LaunchedEffect(favoritesUiState.successMessage) {
        val message = favoritesUiState.successMessage
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
    
    // Preload ads when screen is opened
    LaunchedEffect(Unit) {
        AdManager.getInstance().loadInterstitialAd(context, true)
        AdManager.getInstance().loadRewardedAd(context, true)
    }
    
    Scaffold(
        snackbarHost = { 
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                // Custom success snackbar with icon
                val isSuccess = snackbarData.visuals.message.contains("Added") || 
                               snackbarData.visuals.message.contains("Removed")
                
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
                title = { Text("Cartoon Browser") },
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
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Mode Toggle Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Structured View Button
                FilterChip(
                    selected = uiState.currentMode == BrowseMode.STRUCTURED,
                    onClick = { 
                        if (uiState.currentMode != BrowseMode.STRUCTURED) {
                            viewModel.toggleMode()
                        }
                    },
                    label = { Text("Structured View") },
                    modifier = Modifier.weight(1f)
                )
                
                // Simple List View Button
                FilterChip(
                    selected = uiState.currentMode == BrowseMode.SIMPLE,
                    onClick = { 
                        if (uiState.currentMode != BrowseMode.SIMPLE) {
                            viewModel.toggleMode()
                        }
                    },
                    label = { Text("Simple List View") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Content based on mode
            when (uiState.currentMode) {
                BrowseMode.STRUCTURED -> {
                    StructuredModeView(
                        uiState = uiState,
                        onCountrySelected = { viewModel.onCountrySelected(it) },
                        onGenreSelected = { viewModel.onGenreSelected(it) },
                        onCartoonSelected = { viewModel.onCartoonSelected(it) },
                        onBack = { viewModel.goBack() },
                        onClearSearchResults = { viewModel.clearSearchResults() },
                        onVideoSelected = onVideoSelected,
                        favoritesViewModel = favoritesViewModel
                    )
                }
                BrowseMode.SIMPLE -> {
                    SimpleListModeView(
                        uiState = uiState,
                        onCartoonSelected = { viewModel.onCartoonSelected(it) },
                        onClearSearchResults = { viewModel.clearSearchResults() },
                        onVideoSelected = onVideoSelected,
                        favoritesViewModel = favoritesViewModel
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
    }
}

/**
 * Structured Mode View - Country → Genre → Cartoon List
 */
@Composable
private fun StructuredModeView(
    uiState: why.xee.kidsview.ui.viewmodel.CartoonBrowserUiState,
    onCountrySelected: (String) -> Unit,
    onGenreSelected: (String) -> Unit,
    onCartoonSelected: (String) -> Unit,
    onBack: () -> Unit,
    onClearSearchResults: () -> Unit,
    onVideoSelected: (String) -> Unit,
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // Show search results if available
            uiState.searchResults.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.searchResults.size} videos found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { 
                                onClearSearchResults()
                            }) {
                                Text("Back")
                            }
                        }
                    }
                    items(uiState.searchResults) { video ->
                        VideoCardWithFavorites(
                            video = video,
                            onClick = { onVideoSelected(video.id) },
                            favoritesViewModel = favoritesViewModel
                        )
                    }
                }
            }
            
            // Show cartoons list
            uiState.navigationStack.lastOrNull() == NavigationLevel.CARTOON -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.selectedCountry} > ${uiState.selectedGenre}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onBack) {
                                Text("Back")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(uiState.availableCartoons) { cartoonName ->
                        CartoonItemCard(
                            name = cartoonName,
                            onClick = { onCartoonSelected(cartoonName) }
                        )
                    }
                }
            }
            
            // Show genres list
            uiState.navigationStack.lastOrNull() == NavigationLevel.GENRE -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.selectedCountry ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onBack) {
                                Text("Back")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(uiState.availableGenres) { genre ->
                        GenreItemCard(
                            name = genre,
                            onClick = { onGenreSelected(genre) }
                        )
                    }
                }
            }
            
            // Show countries list (default)
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Select Country",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(uiState.availableCountries) { country ->
                        CountryItemCard(
                            name = country,
                            onClick = { onCountrySelected(country) }
                        )
                    }
                }
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Searching videos...")
                    }
                }
            }
        }
        
        // Error message
        if (uiState.error != null && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple List Mode View - All cartoons in one list
 */
@Composable
fun SimpleListModeView(
    uiState: why.xee.kidsview.ui.viewmodel.CartoonBrowserUiState,
    onCartoonSelected: (String) -> Unit,
    onClearSearchResults: () -> Unit,
    onVideoSelected: (String) -> Unit,
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // Show search results if available
            uiState.searchResults.isNotEmpty() -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${uiState.searchResults.size} videos found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(uiState.searchResults) { video ->
                        VideoCardWithFavorites(
                            video = video,
                            onClick = { onVideoSelected(video.id) },
                            favoritesViewModel = favoritesViewModel
                        )
                    }
                }
            }
            
            // Show all cartoons list
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "All Cartoons (${uiState.fullCartoonList.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(uiState.fullCartoonList) { cartoon ->
                        SimpleCartoonItemCard(
                            cartoon = cartoon,
                            onClick = { onCartoonSelected(cartoon.name) }
                        )
                    }
                }
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Searching videos...")
                    }
                }
            }
        }
        
        // Error message
        if (uiState.error != null && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Country Item Card
 */
@Composable
private fun CountryItemCard(
    name: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Genre Item Card
 */
@Composable
private fun GenreItemCard(
    name: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Cartoon Item Card
 */
@Composable
private fun CartoonItemCard(
    name: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "🔍",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/**
 * Simple Cartoon Item Card (with country and genre info)
 */
@Composable
private fun SimpleCartoonItemCard(
    cartoon: CartoonData.Cartoon,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = cartoon.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = cartoon.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = cartoon.genre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
