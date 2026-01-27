package why.xee.kidsview.ui.subcategory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import why.xee.kidsview.utils.DeviceUtils
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import why.xee.kidsview.utils.Categories
import why.xee.kidsview.utils.SubCategory

/**
 * Sub-Category Screen - Shows sub-categories for a main category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoryScreen(
    categoryId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSubCategory: (String, String) -> Unit
) {
    val category = Categories.getCategoryById(categoryId)
    val subCategories = Categories.getSubCategories(categoryId)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = category?.name ?: "Category",
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
            if (subCategories.isEmpty()) {
                // If no sub-categories, show message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sub-categories available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = DeviceUtils.getAdaptiveGridColumns(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(subCategories) { subCategory ->
                        SubCategoryCard(
                            subCategory = subCategory,
                            onClick = {
                                onNavigateToSubCategory(categoryId, subCategory.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sub-Category Card Component
 */
@Composable
fun SubCategoryCard(
    subCategory: SubCategory,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subCategory.icon,
                    style = MaterialTheme.typography.displayMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subCategory.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

