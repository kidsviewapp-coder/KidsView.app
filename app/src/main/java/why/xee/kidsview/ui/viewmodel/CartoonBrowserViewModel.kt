package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.CartoonData
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Browse mode enum
 */
enum class BrowseMode {
    STRUCTURED,
    SIMPLE
}

/**
 * Navigation level for structured mode
 */
enum class NavigationLevel {
    COUNTRY,
    GENRE,
    CARTOON
}

/**
 * UI State for Cartoon Browser Screen
 */
data class CartoonBrowserUiState(
    val currentMode: BrowseMode = BrowseMode.STRUCTURED,
    val selectedCountry: String? = null,
    val selectedGenre: String? = null,
    val availableCountries: List<String> = emptyList(),
    val availableGenres: List<String> = emptyList(),
    val availableCartoons: List<String> = emptyList(),
    val fullCartoonList: List<CartoonData.Cartoon> = emptyList(),
    val searchResults: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigationStack: List<NavigationLevel> = listOf(NavigationLevel.COUNTRY)
)

/**
 * ViewModel for Cartoon Browser Screen
 */
@HiltViewModel
class CartoonBrowserViewModel @Inject constructor(
    private val repository: YouTubeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CartoonBrowserUiState())
    val uiState: StateFlow<CartoonBrowserUiState> = _uiState.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        val countries = CartoonData.getCountries()
        val allCartoons = CartoonData.getAllCartoons()
        
        _uiState.value = _uiState.value.copy(
            availableCountries = countries,
            fullCartoonList = allCartoons
        )
    }
    
    /**
     * Toggle between Structured and Simple mode
     */
    fun toggleMode() {
        val newMode = if (_uiState.value.currentMode == BrowseMode.STRUCTURED) {
            BrowseMode.SIMPLE
        } else {
            BrowseMode.STRUCTURED
        }
        
        _uiState.value = _uiState.value.copy(
            currentMode = newMode,
            selectedCountry = null,
            selectedGenre = null,
            availableGenres = emptyList(),
            availableCartoons = emptyList(),
            navigationStack = if (newMode == BrowseMode.STRUCTURED) {
                listOf(NavigationLevel.COUNTRY)
            } else {
                _uiState.value.navigationStack
            }
        )
    }
    
    /**
     * Handle country selection in Structured mode
     */
    fun onCountrySelected(country: String) {
        val genres = CartoonData.getGenresForCountry(country)
        
        _uiState.value = _uiState.value.copy(
            selectedCountry = country,
            selectedGenre = null,
            availableGenres = genres,
            availableCartoons = emptyList(),
            navigationStack = listOf(NavigationLevel.COUNTRY, NavigationLevel.GENRE)
        )
    }
    
    /**
     * Handle genre selection in Structured mode
     */
    fun onGenreSelected(genre: String) {
        val country = _uiState.value.selectedCountry
        if (country == null) return
        
        val cartoons = CartoonData.getCartoonsForCountryAndGenre(country, genre)
        
        _uiState.value = _uiState.value.copy(
            selectedGenre = genre,
            availableCartoons = cartoons,
            navigationStack = listOf(
                NavigationLevel.COUNTRY,
                NavigationLevel.GENRE,
                NavigationLevel.CARTOON
            )
        )
    }
    
    /**
     * Handle cartoon selection - triggers YouTube search
     */
    fun onCartoonSelected(cartoonName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                searchResults = emptyList()
            )
            
            // Search for the cartoon on YouTube
            repository.searchVideos("$cartoonName cartoon")
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(
                        searchResults = videos,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to search for videos"
                    )
                }
        }
    }
    
    /**
     * Navigate back in Structured mode
     */
    fun goBack() {
        val currentStack = _uiState.value.navigationStack
        
        when {
            currentStack.lastOrNull() == NavigationLevel.CARTOON -> {
                // Go back from CARTOON to GENRE
                _uiState.value = _uiState.value.copy(
                    navigationStack = listOf(NavigationLevel.COUNTRY, NavigationLevel.GENRE),
                    availableCartoons = emptyList(),
                    searchResults = emptyList()
                )
            }
            currentStack.lastOrNull() == NavigationLevel.GENRE -> {
                // Go back from GENRE to COUNTRY
                _uiState.value = _uiState.value.copy(
                    selectedCountry = null,
                    selectedGenre = null,
                    availableGenres = emptyList(),
                    availableCartoons = emptyList(),
                    searchResults = emptyList(),
                    navigationStack = listOf(NavigationLevel.COUNTRY)
                )
            }
            else -> {
                // Already at COUNTRY level, do nothing
            }
        }
    }
    
    /**
     * Clear search results
     */
    fun clearSearchResults() {
        _uiState.value = _uiState.value.copy(
            searchResults = emptyList()
        )
    }
}
