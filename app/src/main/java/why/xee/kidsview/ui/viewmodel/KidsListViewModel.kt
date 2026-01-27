package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.data.repository.ParentVideoRepository
import why.xee.kidsview.data.repository.VideoRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Kids List Screen
 */
data class KidsListUiState(
    val videos: List<FirestoreVideo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAppLocked: Boolean = false,
    val remainingTimeMs: Long = 0L, // Remaining time in milliseconds
    val isTimeLimitEnabled: Boolean = false,
    val timeLimitMinutes: Int = 0,
    val rejectedRequestsCount: Int = 0, // Count of rejected requests with messages
    val pendingDeleteVideoIds: Set<String> = emptySet() // Video IDs with pending delete requests
)

/**
 * ViewModel for Kids List Screen
 */
@HiltViewModel
class KidsListViewModel @Inject constructor(
    private val repository: ParentVideoRepository,
    private val preferencesManager: PreferencesManager,
    private val requestRepository: VideoRequestRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(
        KidsListUiState(
            isAppLocked = preferencesManager.isAppLocked(),
            isTimeLimitEnabled = preferencesManager.isTimeLimitEnabled(),
            timeLimitMinutes = preferencesManager.getTimeLimitMinutes()
        )
    )
    val uiState: StateFlow<KidsListUiState> = _uiState.asStateFlow()
    
    init {
        // Update app lock status
        _uiState.value = _uiState.value.copy(isAppLocked = preferencesManager.isAppLocked())
        loadVideos()
        loadRejectedRequestsCount()
        loadPendingDeleteRequests()
        startTimerUpdate()
    }
    
    private fun loadRejectedRequestsCount() {
        viewModelScope.launch {
            requestRepository.getAllRequests()
                .onSuccess { requests ->
                    // Count only unread rejected requests
                    val rejectedCount = requests.count { request ->
                        request.status == why.xee.kidsview.data.model.RequestStatus.REJECTED && 
                        request.rejectionMessage.isNotEmpty() && 
                        !preferencesManager.isRejectionRead(request.requestId)
                    }
                    _uiState.value = _uiState.value.copy(rejectedRequestsCount = rejectedCount)
                }
        }
    }
    
    private fun loadPendingDeleteRequests() {
        viewModelScope.launch {
            requestRepository.getAllRequests()
                .onSuccess { requests ->
                    // Get video IDs with pending DELETE requests
                    val pendingDeleteIds = requests
                        .filter { 
                            it.type == why.xee.kidsview.data.model.RequestType.DELETE && 
                            it.status == why.xee.kidsview.data.model.RequestStatus.PENDING &&
                            it.videoId != null
                        }
                        .mapNotNull { it.videoId }
                        .toSet()
                    _uiState.value = _uiState.value.copy(pendingDeleteVideoIds = pendingDeleteIds)
                }
        }
    }
    
    private fun startTimerUpdate() {
        viewModelScope.launch {
            while (true) {
                updateRemainingTime()
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }
    
    private fun updateRemainingTime() {
        if (preferencesManager.isTimeLimitEnabled()) {
            val limitMs = preferencesManager.getTimeLimitMinutes() * 60 * 1000L
            val usedMs = preferencesManager.getTimeUsedToday()
            
            // Calculate remaining time
            // After the fix in applyEarnedTimeToDailyLimit, used time is reduced when earned time is applied,
            // so remaining should always be positive when time is added
            val remaining = (limitMs - usedMs).coerceAtLeast(0L)
            
            _uiState.value = _uiState.value.copy(
                remainingTimeMs = remaining,
                isTimeLimitEnabled = true,
                timeLimitMinutes = preferencesManager.getTimeLimitMinutes()
            )
        } else {
            _uiState.value = _uiState.value.copy(
                remainingTimeMs = 0L,
                isTimeLimitEnabled = false
            )
        }
    }
    
    fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.getAllVideos()
                .onSuccess { videos ->
                    // Filter out videos in Favorites category - they should only be in Favorites folder
                    val videosForKids = videos.filter { it.categoryId != "favorites" }
                    _uiState.value = _uiState.value.copy(
                        videos = videosForKids,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load videos",
                        videos = emptyList()
                    )
                }
        }
    }
    
    fun refresh() {
        loadVideos()
        loadRejectedRequestsCount()
        loadPendingDeleteRequests()
    }
    
    fun updateLockStatus() {
        _uiState.value = _uiState.value.copy(isAppLocked = preferencesManager.isAppLocked())
    }
    
    fun setAppLocked(locked: Boolean) {
        preferencesManager.setAppLocked(locked)
        _uiState.value = _uiState.value.copy(isAppLocked = locked)
    }
    
    fun refreshTimerSettings() {
        // Update timer settings from preferences
        _uiState.value = _uiState.value.copy(
            isTimeLimitEnabled = preferencesManager.isTimeLimitEnabled(),
            timeLimitMinutes = preferencesManager.getTimeLimitMinutes()
        )
    }
}

