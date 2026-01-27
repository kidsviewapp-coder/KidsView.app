package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Search Screen
 */
data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

/**
 * ViewModel for Search Screen
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: YouTubeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    /**
     * Update search query
     */
    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    /**
     * Perform search
     */
    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                hasSearched = false
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                hasSearched = true
            )
            
            repository.searchVideos(query)
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(
                        searchResults = videos,
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
                        errorMessage.contains("keyInvalid", ignoreCase = true) -> {
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
                            errorMessage.ifEmpty { "Search failed. Please try again." }
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
     * Clear search
     */
    fun clearSearch() {
        _uiState.value = SearchUiState()
    }
}

