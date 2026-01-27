package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.data.repository.YouTubeRepository
import why.xee.kidsview.utils.Categories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Category Screen
 */
data class CategoryUiState(
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    val categoryName: String = "",
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Category Screen
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: YouTubeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()
    
    /**
     * Load videos for a category or sub-category
     */
    fun loadCategoryVideos(categoryId: String, subCategoryId: String? = null) {
        val category = Categories.getCategoryById(categoryId)
        if (category == null) {
            _uiState.value = _uiState.value.copy(
                error = "Category not found"
            )
            return
        }

        viewModelScope.launch {
            val subCategoryName = subCategoryId?.let {
                Categories.getSubCategoryById(categoryId, it)?.let { sub -> " - ${sub.name}" }
            } ?: ""
            
            _uiState.value = _uiState.value.copy(
                categoryId = categoryId,
                subCategoryId = subCategoryId,
                categoryName = category.name + subCategoryName,
                isLoading = true,
                error = null
            )

            // Always use search API - never use playlists (for child safety)
            // Get search query from subcategory or use category name
            val searchQuery = subCategoryId?.let {
                Categories.getSubCategoryById(categoryId, it)?.searchQuery
            } ?: category.name
            
            loadVideosBySearch(searchQuery)
        }
    }
    
    /**
     * Load videos using search API as fallback
     */
    private fun loadVideosBySearch(query: String) {
        viewModelScope.launch {
            // Use the query as-is if it already contains "kids" or "cartoon", otherwise append " kids"
            val searchQuery = if (query.contains("kids", ignoreCase = true) || 
                                  query.contains("cartoon", ignoreCase = true) ||
                                  query.contains("for", ignoreCase = true)) {
                query
            } else {
                "$query kids"
            }
            
            repository.searchVideos(searchQuery)
                .onSuccess { videos ->
                    if (videos.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            videos = videos,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No videos found. Try a different search."
                        )
                    }
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: ""
                    val userFriendlyError = when {
                        errorMessage.contains("API key is missing", ignoreCase = true) -> {
                            "API key is missing. Please add your YouTube API key to local.properties file."
                        }
                        errorMessage.contains("API key", ignoreCase = true) || 
                        errorMessage.contains("keyInvalid", ignoreCase = true) ||
                        errorMessage.contains("invalid", ignoreCase = true) && errorMessage.contains("key", ignoreCase = true) -> {
                            "Invalid API key. Please check your YouTube API key in local.properties."
                        }
                        errorMessage.contains("quota", ignoreCase = true) ||
                        errorMessage.contains("403", ignoreCase = true) ||
                        errorMessage.contains("quotaExceeded", ignoreCase = true) -> {
                            "📊 API Quota Exceeded\n\n" +
                            "Daily limit reached (~100 searches/day).\n" +
                            "Resets at midnight Pacific Time.\n\n" +
                            "Try again tomorrow or request quota increase."
                        }
                        errorMessage.contains("network", ignoreCase = true) ||
                        errorMessage.contains("timeout", ignoreCase = true) ||
                        errorMessage.contains("Unable to resolve host", ignoreCase = true) -> {
                            "Network error. Please check your internet connection."
                        }
                        errorMessage.contains("YouTube API Error", ignoreCase = true) -> {
                            errorMessage // Use the specific API error message
                        }
                        else -> {
                            "Unable to load videos: $errorMessage"
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = userFriendlyError
                    )
                }
        }
    }
    
    /**
     * Retry loading videos
     */
    fun retry() {
        val categoryId = _uiState.value.categoryId
        val subCategoryId = _uiState.value.subCategoryId
        if (categoryId != null) {
            loadCategoryVideos(categoryId, subCategoryId)
        }
    }
}

