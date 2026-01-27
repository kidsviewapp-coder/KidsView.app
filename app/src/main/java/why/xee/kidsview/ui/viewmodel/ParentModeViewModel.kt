package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.preferences.AuthMode
import why.xee.kidsview.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * UI State for Parent Mode authentication entry
 */
data class ParentModeUiState(
    val enteredInput: String = "",
    val error: String? = null,
    val isVerified: Boolean = false,
    val authMode: AuthMode = AuthMode.PIN
)

/**
 * ViewModel for Parent Mode PIN/Password verification
 */
@HiltViewModel
class ParentModeViewModel @Inject constructor(
    val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(
        ParentModeUiState(authMode = preferencesManager.getAuthMode())
    )
    val uiState: StateFlow<ParentModeUiState> = _uiState.asStateFlow()
    
    init {
        _uiState.value = _uiState.value.copy(authMode = preferencesManager.getAuthMode())
    }
    
    fun addDigit(digit: String) {
        val currentInput = _uiState.value.enteredInput
        val maxLength = if (_uiState.value.authMode == AuthMode.PIN) 6 else Int.MAX_VALUE
        
        if (currentInput.length < maxLength) {
            _uiState.value = _uiState.value.copy(
                enteredInput = currentInput + digit,
                error = null
            )
            
            // Auto-verify when required length is reached
            val requiredLength = if (_uiState.value.authMode == AuthMode.PIN) 6 else 8
            if (_uiState.value.enteredInput.length >= requiredLength) {
                verifyAuth()
            }
        }
    }
    
    fun setPassword(password: String) {
        if (_uiState.value.authMode == AuthMode.PASSWORD) {
            _uiState.value = _uiState.value.copy(
                enteredInput = password,
                error = null
            )
        }
    }
    
    fun removeDigit() {
        val currentInput = _uiState.value.enteredInput
        if (currentInput.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                enteredInput = currentInput.dropLast(1),
                error = null
            )
        }
    }
    
    fun clearInput() {
        _uiState.value = _uiState.value.copy(
            enteredInput = "",
            error = null,
            isVerified = false
        )
    }
    
    fun verifyAuth(): Boolean {
        val enteredInput = _uiState.value.enteredInput
        val isValid = preferencesManager.verifyAuth(enteredInput)
        val authMode = _uiState.value.authMode
        
        _uiState.value = _uiState.value.copy(
            isVerified = isValid,
            error = if (isValid) null else "Incorrect ${if (authMode == AuthMode.PIN) "PIN" else "Password"}. Please try again."
        )
        
        if (isValid) {
            // If unlocking from locked state, unlock the app
            if (preferencesManager.isAppLocked()) {
                preferencesManager.setAppLocked(false)
            }
        } else {
            // Clear input after showing error
            viewModelScope.launch {
                delay(1000)
                clearInput()
            }
        }
        
        return isValid
    }
    
    fun getMaxLength(): Int {
        return if (_uiState.value.authMode == AuthMode.PIN) 6 else Int.MAX_VALUE
    }
    
    fun getMinLength(): Int {
        return if (_uiState.value.authMode == AuthMode.PIN) 6 else 8
    }
}

