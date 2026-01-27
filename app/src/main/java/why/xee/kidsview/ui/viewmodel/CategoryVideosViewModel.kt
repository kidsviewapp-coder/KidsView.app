package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.data.repository.CategoryRepository
import why.xee.kidsview.data.repository.ParentVideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryVideosUiState(
    val videos: List<FirestoreVideo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRemoving: Boolean = false
)

@HiltViewModel
class CategoryVideosViewModel @Inject constructor(
    private val parentVideoRepository: ParentVideoRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoryVideosUiState())
    val uiState: StateFlow<CategoryVideosUiState> = _uiState.asStateFlow()
    
    fun loadCategoryVideos(categoryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            parentVideoRepository.getVideosByCategory(categoryId)
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(
                        videos = videos,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load videos",
                        videos = emptyList()
                    )
                }
        }
    }
    
    fun removeVideoFromCategory(videoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRemoving = true)
            
            parentVideoRepository.updateVideoCategory(videoId, null)
                .onSuccess {
                    // Reload videos to reflect the change
                    val currentCategoryId = _uiState.value.videos.firstOrNull { it.videoId == videoId }?.categoryId
                    currentCategoryId?.let { categoryId ->
                        loadCategoryVideos(categoryId)
                        // Update category video count
                        categoryRepository.updateVideoCount(categoryId)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to remove video"
                    )
                }
            
            _uiState.value = _uiState.value.copy(isRemoving = false)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

