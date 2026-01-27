package why.xee.kidsview.ui.state

/**
 * Generic UI state wrapper for handling loading, success, and error states
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(
        val message: String,
        val retry: (() -> Unit)? = null,
        val errorCode: String? = null
    ) : UiState<Nothing>()
    
    /**
     * Check if state is loading
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Check if state is success
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if state is error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Get data if success, null otherwise
     */
    fun getDataOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Get error message if error, null otherwise
     */
    fun getErrorMessageOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }
}

