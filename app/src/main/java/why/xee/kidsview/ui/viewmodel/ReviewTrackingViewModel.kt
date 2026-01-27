package why.xee.kidsview.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.utils.ReviewManager
import javax.inject.Inject

/**
 * ViewModel for tracking parent mode launches and requesting reviews.
 * This ensures reviews only appear in parent mode, not kids mode.
 */
@HiltViewModel
class ReviewTrackingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    /**
     * Track parent mode launch and request review if criteria are met.
     */
    fun trackParentModeLaunch(activity: Activity) {
        viewModelScope.launch {
            ReviewManager.trackLaunchAndRequestReviewIfNeeded(
                activity,
                preferencesManager,
                isParentMode = true
            )
        }
    }
}

