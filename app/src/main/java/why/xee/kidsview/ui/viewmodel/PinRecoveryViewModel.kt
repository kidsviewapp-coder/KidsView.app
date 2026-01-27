package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import why.xee.kidsview.data.preferences.AuthMode
import why.xee.kidsview.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PinRecoveryUiState(
    val error: String? = null,
    val questionsVerified: Boolean = false,
    val recoveredAuthValue: String = "",
    val resetError: String? = null
)

@HiltViewModel
class PinRecoveryViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinRecoveryUiState())
    val uiState: StateFlow<PinRecoveryUiState> = _uiState.asStateFlow()

    fun getSecurityQuestion(questionNumber: Int): String? {
        return preferencesManager.getSecurityQuestion(questionNumber)
    }

    fun getAuthMode(): AuthMode {
        return preferencesManager.getAuthMode()
    }

    fun verifySecurityQuestions(answers: List<String>) {
        // Verify all 3 answers must be correct
        val isValid = preferencesManager.verifySecurityQuestions(answers)
        if (!isValid) {
            _uiState.value = _uiState.value.copy(
                error = "One or more answers are incorrect. All 3 answers must be correct.",
                questionsVerified = false,
                recoveredAuthValue = ""
            )
        } else {
            // All answers correct - show current PIN/Password
            val currentAuth = when (preferencesManager.getAuthMode()) {
                AuthMode.PIN -> preferencesManager.getParentPin()
                AuthMode.PASSWORD -> preferencesManager.getParentPassword()
            }
            _uiState.value = _uiState.value.copy(
                error = null,
                questionsVerified = true,
                recoveredAuthValue = currentAuth,
                resetError = null
            )
        }
    }

    fun resetAuth(newValue: String, confirmValue: String): Boolean {
        if (newValue != confirmValue) {
            _uiState.value = _uiState.value.copy(
                resetError = "Values do not match. Please try again."
            )
            return false
        }

        val authMode = preferencesManager.getAuthMode()
        val minLength = if (authMode == AuthMode.PIN) 6 else 8
        
        if (newValue.length < minLength) {
            _uiState.value = _uiState.value.copy(
                resetError = "${if (authMode == AuthMode.PIN) "PIN" else "Password"} must be at least $minLength characters."
            )
            return false
        }

        val success = when (authMode) {
            AuthMode.PIN -> preferencesManager.setParentPin(newValue)
            AuthMode.PASSWORD -> preferencesManager.setParentPassword(newValue)
        }

        if (success) {
            _uiState.value = _uiState.value.copy(
                resetError = null,
                recoveredAuthValue = newValue
            )
            return true
        } else {
            _uiState.value = _uiState.value.copy(
                resetError = "Failed to reset ${if (authMode == AuthMode.PIN) "PIN" else "password"}. Please try again."
            )
            return false
        }
    }

    fun hasSecurityQuestionsSet(): Boolean {
        return preferencesManager.hasSecurityQuestionsSet()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, resetError = null)
    }
}

