package why.xee.kidsview.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import why.xee.kidsview.ui.components.VideoCardWithFavorites
import why.xee.kidsview.ui.viewmodel.SearchViewModel

/**
 * Search Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateQuery(it) },
                        onSearch = { viewModel.search(it) },
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
            when {
                !uiState.hasSearched -> {
                    // Initial state - show search suggestions
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = "Popular Cartoons",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        item {
                            Text(
                                text = "Indian Cartoons",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        item {
                            SearchSuggestionsRow(
                                suggestions = indianCartoons,
                                onSuggestionClick = { suggestion ->
                                    viewModel.updateQuery(suggestion)
                                    viewModel.search(suggestion)
                                }
                            )
                        }
                        item {
                            Text(
                                text = "Pakistani Cartoons",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        item {
                            SearchSuggestionsRow(
                                suggestions = pakistaniCartoons,
                                onSuggestionClick = { suggestion ->
                                    viewModel.updateQuery(suggestion)
                                    viewModel.search(suggestion)
                                }
                            )
                        }
                    }
                }
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
                    val errorMessage = uiState.error ?: "Unknown error"
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "😕",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.searchResults.isEmpty() -> {
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
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.searchResults) { video ->
                            VideoCardWithFavorites(
                                video = video,
                                onClick = {
                                    // Directly navigate to player (no ads in child-facing screens)
                                    onNavigateToPlayer(video.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Search bar component
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text("Search videos...")
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
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
    
    // Auto-search when query changes (with debounce)
    LaunchedEffect(query) {
        if (query.length >= 3) {
            kotlinx.coroutines.delay(500)
            onSearch(query)
        }
    }
}

/**
 * List of famous Indian cartoons
 */
private val indianCartoons = listOf(
    "Chhota Bheem",
    "Motu Patlu",
    "Krishna Balram",
    "Little Singham",
    "Gattu Battu",
    "Roll No 21",
    "Pakdam Pakdai",
    "Shiva",
    "Arjun Prince of Bali",
    "Keymon Ache",
    "Golmaal Jr",
    "Baal Veer",
    "Doraemon Hindi",
    "Shin Chan Hindi",
    "Ninja Hattori",
    "Kiteretsu",
    "Kochikame",
    "Dabangg",
    "Super Bheem",
    "Krishna and Friends"
)

/**
 * List of famous Pakistani cartoons
 */
private val pakistaniCartoons = listOf(
    "Burka Avenger",
    "3 Bahadur",
    "Allahyar and the Legend of Markhor",
    "Tick Tock",
    "The Donkey King",
    "Shehr-e-Tabassum",
    "Kitni Girhain Baaki Hain",
    "Mastana",
    "Chacha Jee",
    "Uncle Sargam",
    "Ainak Wala Jin",
    "Taleem o Tarbiat",
    "Aik Aur Aik Gyarah",
    "Tot Batot",
    "Babar",
    "Dekh Magar Pyar Se"
)

/**
 * Search suggestions row component
 */
@Composable
fun SearchSuggestionsRow(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionChip(
                text = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}

/**
 * Suggestion chip component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        enabled = true,
        label = { 
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.height(38.dp)
    )
}

