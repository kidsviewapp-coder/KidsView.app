package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import why.xee.kidsview.BuildConfig
import why.xee.kidsview.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * UI State for Settings Screen
 */
data class SettingsUiState(
    val isDarkMode: Boolean = true, // Default to dark mode
    val appVersion: String = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
)

/**
 * ViewModel for Settings Screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(
        SettingsUiState(isDarkMode = false) // Always light themes
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
}

