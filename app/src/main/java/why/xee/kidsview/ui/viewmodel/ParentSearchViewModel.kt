package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.CartoonDatabase
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
 * UI State for Parent Search Screen
 */
data class ParentSearchUiState(
    val searchQuery: String = "",
    val searchResults: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val savedVideosByCategory: Map<FirestoreCategory?, List<FirestoreVideo>> = emptyMap(),
    val isLoadingSavedVideos: Boolean = false,
    // Cartoon browsing
    val selectedCountry: String? = null,
    val selectedGenre: String? = null,
    val cartoonVideos: List<VideoItem> = emptyList(),
    val isLoadingCartoons: Boolean = false
)

/**
 * ViewModel for Parent Search Screen
 */
@HiltViewModel
class ParentSearchViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    private val parentVideoRepository: ParentVideoRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ParentSearchUiState())
    val uiState: StateFlow<ParentSearchUiState> = _uiState.asStateFlow()
    
    init {
        // Favorites now use separate collection, no need to ensure category exists
        loadSavedVideos()
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    fun searchVideos() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter a search query"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            repository.searchVideos(query)
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(
                        searchResults = videos,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    val errorMessage = exception.message ?: "Failed to search videos"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage,
                        searchResults = emptyList()
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResults = emptyList(),
            error = null
        )
    }
    
    fun loadSavedVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSavedVideos = true)
            
            val categoriesResult = categoryRepository.getAllCategories()
            val videosResult = parentVideoRepository.getAllVideos()
            
            if (categoriesResult.isSuccess && videosResult.isSuccess) {
                val categories = categoriesResult.getOrNull() ?: emptyList()
                val videos = videosResult.getOrNull() ?: emptyList()
                
                // Filter out "favorites" category - it's a special folder, not a regular category
                val filteredCategories = categories.filter { it.categoryId != "favorites" }
                
                // Group videos by category
                val videosByCategory = mutableMapOf<FirestoreCategory?, MutableList<FirestoreVideo>>()
                
                videos.forEach { video ->
                    // Skip videos in favorites category - they're handled separately
                    if (video.categoryId == "favorites") {
                        return@forEach
                    }
                    val category = video.categoryId?.let { categoryId ->
                        filteredCategories.find { it.categoryId == categoryId }
                    }
                    videosByCategory.getOrPut(category) { mutableListOf() }.add(video)
                }
                
                _uiState.value = _uiState.value.copy(
                    savedVideosByCategory = videosByCategory,
                    isLoadingSavedVideos = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingSavedVideos = false,
                    savedVideosByCategory = emptyMap()
                )
            }
        }
    }
    
    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            parentVideoRepository.deleteVideo(videoId)
                .onSuccess {
                    loadSavedVideos() // Refresh the list
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to delete video"
                    )
                }
        }
    }
    
    // Cartoon browsing functions
    fun selectCountry(country: String) {
        _uiState.value = _uiState.value.copy(
            selectedCountry = country,
            selectedGenre = null,
            cartoonVideos = emptyList(),
            searchResults = emptyList() // Clear search results when browsing cartoons
        )
    }
    
    fun selectGenre(genre: String) {
        val country = _uiState.value.selectedCountry ?: return
        _uiState.value = _uiState.value.copy(
            selectedGenre = genre,
            cartoonVideos = emptyList()
        )
        loadCartoonVideos(country, genre)
    }
    
    fun clearCartoonSelection() {
        _uiState.value = _uiState.value.copy(
            selectedCountry = null,
            selectedGenre = null,
            cartoonVideos = emptyList()
        )
    }
    
    private fun loadCartoonVideos(country: String, genre: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingCartoons = true,
                error = null,
                cartoonVideos = emptyList()
            )
            
            repository.searchCartoonsByCountryAndGenre(country, genre)
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(
                        cartoonVideos = videos,
                        isLoadingCartoons = false,
                        error = null,
                        searchResults = videos // Show cartoon videos in search results
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingCartoons = false,
                        error = exception.message ?: "Failed to load cartoons",
                        cartoonVideos = emptyList()
                    )
                }
        }
    }
    
    fun getCountries(): List<String> = CartoonDatabase.getCountries()
    
    fun getGenresForSelectedCountry(): List<String> {
        val country = _uiState.value.selectedCountry ?: return emptyList()
        return CartoonDatabase.getGenres(country)
    }
}

