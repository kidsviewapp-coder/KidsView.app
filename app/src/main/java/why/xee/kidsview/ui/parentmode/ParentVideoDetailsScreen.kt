package why.xee.kidsview.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import why.xee.kidsview.navigation.Screen
import why.xee.kidsview.ui.viewmodel.ParentVideoDetailsViewModel
import androidx.compose.ui.platform.LocalContext
import why.xee.kidsview.utils.AdManager
import why.xee.kidsview.ui.components.BannerAd
import why.xee.kidsview.ui.components.ElegantDialog
import why.xee.kidsview.ui.viewmodel.CategoryManagementViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

/**
 * Parent Video Details Screen - View, watch, and save video with quality and category selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentVideoDetailsScreen(
    videoId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit = {},
    viewModel: ParentVideoDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(videoId) {
        viewModel.loadVideoDetails(videoId)
        AdManager.getInstance().loadInterstitialAd(context, true)
        AdManager.getInstance().loadRewardedAd(context, true)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text("Video Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (uiState.video != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.saveSuccess) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Video saved successfully!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Button(
                            onClick = {
                                // Show interstitial ad before saving video
                                AdManager.getInstance().showInterstitialAd(context, true, false) {
                                    viewModel.saveVideo()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSaved && !uiState.isSaving && uiState.video != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isSaved) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                contentColor = if (uiState.isSaved) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            )
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...")
                            } else if (uiState.isSaved) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Already Saved")
                            } else {
                                Text("Save to Approved List")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "❌",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { viewModel.clearError() }
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            uiState.video != null -> {
                val video = uiState.video!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Video Player Section with Watch Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(0.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = video.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Play button overlay
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                                FilledTonalButton(
                                onClick = {
                                    if (onNavigateToPlayer != {}) {
                                        // Parent Mode: show interstitial ad before playing video (cooldown enforced)
                                        AdManager.getInstance().showInterstitialAd(
                                            context,
                                            true,
                                            false
                                        ) {
                                            onNavigateToPlayer(video.id)
                                        }
                                    }
                                },
                                modifier = Modifier.padding(16.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Watch Video",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Video Info and Settings
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Channel and Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (video.channelTitle.isNotEmpty()) {
                                Text(
                                    text = "📺 ${video.channelTitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (video.duration.isNotEmpty()) {
                                Text(
                                    text = "⏱️ ${video.duration}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        // Quality Selection
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Video Quality",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Choose quality for kids viewing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                var expanded by remember { mutableStateOf(false) }
                                
                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = !expanded }
                                ) {
                                    OutlinedTextField(
                                        value = uiState.selectedQuality.replaceFirstChar { it.uppercase() },
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                    
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        listOf("auto", "hd720", "medium", "small").forEach { quality ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Text(
                                                        quality.replaceFirstChar { it.uppercase() },
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setQuality(quality)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Category Selection
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Category",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Assign video to a category",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                val selectedCategory = uiState.categories.find { 
                                    it.categoryId == uiState.selectedCategoryId 
                                }
                                
                                OutlinedButton(
                                    onClick = { showCategoryDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isLoadingCategories
                                ) {
                                    if (uiState.isLoadingCategories) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(selectedCategory?.name ?: "Select Category")
                                    }
                                }
                            }
                            
                            // Add/Update Category Button (if category selected and different from original or not saved)
                            val categoryChanged = uiState.selectedCategoryId != uiState.originalCategoryId
                            if (uiState.selectedCategoryId != null && (!uiState.isSaved || categoryChanged)) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val selectedCategory = uiState.categories.find { 
                                    it.categoryId == uiState.selectedCategoryId 
                                }
                                FilledTonalButton(
                                    onClick = {
                                // Show interstitial ad before saving video
                                AdManager.getInstance().showInterstitialAd(context, true, false) {
                                    viewModel.saveVideo()
                                }
                            },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSaving && uiState.video != null,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (uiState.isSaved && categoryChanged) {
                                            "Update to ${selectedCategory?.name ?: "Category"}"
                                        } else {
                                            "Add to ${selectedCategory?.name ?: "Category"}"
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Description
                        if (video.description.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Description",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = video.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Video ID
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Video ID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = video.id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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
        
        // Category Selection Dialog
        if (showCategoryDialog) {
            CategorySelectionDialog(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = { categoryId ->
                    viewModel.setCategory(categoryId)
                    showCategoryDialog = false
                },
                onAddCategory = {
                    showCategoryDialog = false
                    showAddCategoryDialog = true
                },
                onDismiss = { showCategoryDialog = false }
            )
        }
        
        // Add Category Dialog
        if (showAddCategoryDialog) {
            AddCategoryDialog(
                onCategoryCreated = { categoryId ->
                    viewModel.setCategory(categoryId)
                    showAddCategoryDialog = false
                    // Reload categories to show the new one
                    viewModel.loadCategories()
                },
                onDismiss = { showAddCategoryDialog = false }
            )
        }
    }
}

/**
 * Category Selection Dialog
 */
@Composable
private fun CategorySelectionDialog(
    categories: List<why.xee.kidsview.data.model.FirestoreCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    onAddCategory: () -> Unit,
    onDismiss: () -> Unit,
    categoryManagementViewModel: CategoryManagementViewModel = hiltViewModel()
) {
    ElegantDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "No Category" option
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(null) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCategoryId == null) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📁", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "No Category",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedCategoryId == null) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selectedCategoryId == null) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // Existing categories
                items(categories) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category.categoryId) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCategoryId == category.categoryId) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(category.icon, style = MaterialTheme.typography.titleLarge)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selectedCategoryId == category.categoryId) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    "${category.videoCount} video${if (category.videoCount != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedCategoryId == category.categoryId) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // Add Category option (always shown)
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddCategory() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Add New Category",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            // Close button
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}

/**
 * Add Category Dialog
 */
@Composable
private fun AddCategoryDialog(
    onCategoryCreated: (String) -> Unit,
    onDismiss: () -> Unit,
    categoryManagementViewModel: CategoryManagementViewModel = hiltViewModel()
) {
    var categoryName by remember { mutableStateOf("") }
    var categoryDescription by remember { mutableStateOf("") }
    var categoryIcon by remember { mutableStateOf("📁") }
    val categoryUiState by categoryManagementViewModel.uiState.collectAsState()
    var isCreating by remember { mutableStateOf(false) }
    
    // Watch for successful category creation
    LaunchedEffect(categoryUiState.categories.size) {
        if (isCreating && categoryName.isNotBlank()) {
            // Category was created, find it and call callback
            val newCategory = categoryUiState.categories.find { 
                it.name.equals(categoryName, ignoreCase = true) 
            }
            if (newCategory != null) {
                onCategoryCreated(newCategory.categoryId)
                isCreating = false
                categoryName = ""
                categoryDescription = ""
                categoryIcon = "📁"
            }
        }
    }
    
    ElegantDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create New Category",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = categoryDescription,
                onValueChange = { categoryDescription = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
            
            OutlinedTextField(
                value = categoryIcon,
                onValueChange = { categoryIcon = it },
                label = { Text("Icon (Emoji)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            val errorMessage = categoryUiState.error
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (categoryName.isNotBlank()) {
                            isCreating = true
                            categoryManagementViewModel.createCategory(
                                name = categoryName,
                                description = categoryDescription,
                                icon = categoryIcon
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = categoryName.isNotBlank() && !categoryUiState.isLoading && !isCreating
                ) {
                    if (categoryUiState.isLoading || isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create")
                    }
                }
            }
        }
    }
}
