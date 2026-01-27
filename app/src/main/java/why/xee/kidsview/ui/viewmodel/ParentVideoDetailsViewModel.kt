package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.FirestoreCategory
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.data.repository.CategoryRepository
import why.xee.kidsview.data.repository.ParentVideoRepository
import why.xee.kidsview.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Parent Video Details Screen
 */
data class ParentVideoDetailsUiState(
    val video: VideoItem? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val selectedQuality: String = "auto",
    val selectedCategoryId: String? = null,
    val originalCategoryId: String? = null, // Track original category to detect changes
    val categories: List<FirestoreCategory> = emptyList(),
    val isLoadingCategories: Boolean = false
)

/**
 * ViewModel for Parent Video Details Screen
 */
@HiltViewModel
class ParentVideoDetailsViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    private val parentVideoRepository: ParentVideoRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ParentVideoDetailsUiState())
    val uiState: StateFlow<ParentVideoDetailsUiState> = _uiState.asStateFlow()
    
    init {
        loadCategories()
    }
    
    fun loadVideoDetails(videoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // First check if video is already saved
            val isAlreadySaved = parentVideoRepository.isVideoSaved(videoId)
            
            repository.getVideoDetails(videoId)
                .onSuccess { video ->
                    // If saved, get the saved video to get quality and category
                    var savedVideo: FirestoreVideo? = null
                    if (isAlreadySaved) {
                        val videos = parentVideoRepository.getAllVideos().getOrNull()
                        savedVideo = videos?.find { it.videoId == videoId }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        video = video,
                        isLoading = false,
                        isSaved = isAlreadySaved,
                        selectedQuality = savedVideo?.quality ?: "auto",
                        selectedCategoryId = savedVideo?.categoryId,
                        originalCategoryId = savedVideo?.categoryId, // Store original category for comparison
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load video details",
                        isSaved = isAlreadySaved
                    )
                }
        }
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCategories = true)
            
            categoryRepository.getAllCategories()
                .onSuccess { categories ->
                    // Filter out "favorites" category - it's a special folder, not a regular category
                    val filteredCategories = categories.filter { it.categoryId != "favorites" }
                    _uiState.value = _uiState.value.copy(
                        categories = filteredCategories,
                        isLoadingCategories = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingCategories = false)
                }
        }
    }
    
    fun setQuality(quality: String) {
        _uiState.value = _uiState.value.copy(selectedQuality = quality)
    }
    
    fun setCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }
    
    fun saveVideo() {
        val video = _uiState.value.video ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            
            val firestoreVideo = FirestoreVideo.fromVideoItem(
                videoItem = video,
                duration = video.duration,
                quality = _uiState.value.selectedQuality,
                categoryId = _uiState.value.selectedCategoryId
            )
            
            parentVideoRepository.saveVideo(firestoreVideo)
                .onSuccess {
                    // Update video count for categories if changed
                    val oldCategoryId = _uiState.value.originalCategoryId
                    val newCategoryId = _uiState.value.selectedCategoryId
                    
                    // Only update counts if category actually changed
                    if (oldCategoryId != newCategoryId) {
                        // Update new category count (will recalculate from videos)
                        newCategoryId?.let { categoryId ->
                            categoryRepository.updateVideoCount(categoryId)
                        }
                        // Update old category count if it existed
                        oldCategoryId?.let { categoryId ->
                            categoryRepository.updateVideoCount(categoryId)
                        }
                    } else if (newCategoryId != null && !_uiState.value.isSaved) {
                        // New video being saved to a category
                        categoryRepository.updateVideoCount(newCategoryId)
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isSaved = true,
                        saveSuccess = true,
                        originalCategoryId = newCategoryId, // Update original after save
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = exception.message ?: "Failed to save video",
                        saveSuccess = false
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, saveSuccess = false)
    }
}

