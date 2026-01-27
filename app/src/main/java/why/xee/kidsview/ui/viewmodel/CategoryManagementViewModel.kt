package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.FirestoreCategory
import why.xee.kidsview.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryManagementUiState(
    val categories: List<FirestoreCategory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val deletingCategoryId: String? = null
)

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadCategories()
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            categoryRepository.getAllCategories()
                .onSuccess { categories ->
                    // Filter out "favorites" category - it's a special folder, not a regular category
                    val filteredCategories = categories.filter { it.categoryId != "favorites" }
                    _uiState.value = _uiState.value.copy(
                        categories = filteredCategories,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load categories",
                        categories = emptyList()
                    )
                }
        }
    }
    
    fun createCategory(name: String, description: String = "", icon: String = "📁", color: String = "#6C5CE7") {
        viewModelScope.launch {
            val categoryId = FirestoreCategory.generateCategoryId(name)
            val category = FirestoreCategory(
                categoryId = categoryId,
                name = name,
                description = description,
                icon = icon,
                color = color
            )
            
            categoryRepository.createCategory(category)
                .onSuccess {
                    loadCategories() // Reload to show new category
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to create category"
                    )
                }
        }
    }
    
    fun updateCategory(category: FirestoreCategory) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
                .onSuccess {
                    loadCategories() // Reload
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to update category"
                    )
                }
        }
    }
    
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            // Prevent deletion of Favorites category
            if (categoryId == "favorites") {
                _uiState.value = _uiState.value.copy(
                    error = "Favorites category cannot be deleted"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(
                isDeleting = true,
                deletingCategoryId = categoryId
            )
            
            categoryRepository.deleteCategory(categoryId)
                .onSuccess {
                    loadCategories() // Reload
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to delete category"
                    )
                }
            
            _uiState.value = _uiState.value.copy(
                isDeleting = false,
                deletingCategoryId = null
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

