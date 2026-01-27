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
 * UI State for Home Screen
 */
data class HomeUiState(
    val popularVideos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String? = null
)

/**
 * ViewModel for Home Screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: YouTubeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val categories = Categories.allCategories
    
    init {
        loadPopularVideos()
    }
    
    /**
     * Load popular videos (using first category as default)
     * Always uses search API with type=video only (no playlists for child safety)
     */
    private fun loadPopularVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Always use search API - never use playlists
            val firstCategory = categories.firstOrNull()
            if (firstCategory != null) {
                loadVideosBySearch(firstCategory.name)
            } else {
                // No categories available, use default search
                loadVideosBySearch("kids cartoons")
            }
        }
    }
    
    /**
     * Load videos using search API as fallback
     */
    private fun loadVideosBySearch(query: String) {
        viewModelScope.launch {
            repository.searchVideos("$query kids")
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(
                        popularVideos = videos,
                        isLoading = false
                    )
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
                            "📊 Daily API quota exceeded.\n\n" +
                            "Quota resets daily. Please try again later."
                        }
                        errorMessage.contains("network", ignoreCase = true) ||
                        errorMessage.contains("timeout", ignoreCase = true) -> {
                            "Network error. Please check your internet connection."
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
     * Select a category
     */
    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = categoryId)
    }
    
    /**
     * Retry loading videos
     */
    fun retry() {
        loadPopularVideos()
    }
}

