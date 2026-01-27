package why.xee.kidsview.ui.parentmode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import why.xee.kidsview.ui.viewmodel.CategoryManagementViewModel
import androidx.compose.ui.platform.LocalContext
import why.xee.kidsview.utils.AdManager
import why.xee.kidsview.ui.components.BannerAd
import why.xee.kidsview.ui.components.ElegantAlertDialog
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    onNavigateBack: () -> Unit,
    onViewCategoryVideos: (String, String) -> Unit = { _, _ -> },
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<why.xee.kidsview.data.model.FirestoreCategory?>(null) }
    var hasShownAd by remember { mutableStateOf(false) }
    
    // Preload ads when screen is opened
    LaunchedEffect(Unit) {
        AdManager.getInstance().loadInterstitialAd(context, true)
        AdManager.getInstance().loadRewardedAd(context, true)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.categories.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📁",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Categories Yet",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create categories to organize videos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.categories) { category ->
                            CategoryCard(
                                category = category,
                                onEdit = { editingCategory = category },
                                onDelete = { viewModel.deleteCategory(category.categoryId) },
                                onViewVideos = { onViewCategoryVideos(category.categoryId, category.name) },
                                isDeleting = uiState.isDeleting && uiState.deletingCategoryId == category.categoryId
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
        
        // Add/Edit Dialog
        if (showAddDialog || editingCategory != null) {
            CategoryDialog(
                category = editingCategory,
                onDismiss = {
                    showAddDialog = false
                    editingCategory = null
                },
                onSave = { name, description, icon, color ->
                    if (editingCategory != null) {
                        viewModel.updateCategory(editingCategory!!.copy(
                            name = name,
                            description = description,
                            icon = icon,
                            color = color
                        ))
                    } else {
                        viewModel.createCategory(name, description, icon, color)
                    }
                    showAddDialog = false
                    editingCategory = null
                }
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: why.xee.kidsview.data.model.FirestoreCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewVideos: () -> Unit,
    isDeleting: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewVideos),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewVideos),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${category.videoCount} video${if (category.videoCount != 1) "s" else ""} - Tap to view",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                // Hide delete button for Favorites category
                if (category.categoryId != "favorites") {
                    IconButton(
                        onClick = onDelete,
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDialog(
    category: why.xee.kidsview.data.model.FirestoreCategory?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "📁") }
    
    ElegantAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category != null) "Edit Category" else "New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Icon (emoji)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, description, icon, category?.color ?: "#6C5CE7") },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

