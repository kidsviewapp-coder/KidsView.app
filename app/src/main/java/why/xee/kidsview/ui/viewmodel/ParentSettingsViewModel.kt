package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import why.xee.kidsview.data.preferences.AuthMode
import why.xee.kidsview.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ParentSettingsViewModel @Inject constructor(
    val preferencesManager: PreferencesManager
) : ViewModel()

