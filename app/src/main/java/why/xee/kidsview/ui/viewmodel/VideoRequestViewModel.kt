package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.data.model.RequestStatus
import why.xee.kidsview.data.model.RequestType
import why.xee.kidsview.data.model.VideoRequest
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.data.repository.ParentVideoRepository
import why.xee.kidsview.data.repository.VideoRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoRequestUiState(
    val videos: List<FirestoreVideo> = emptyList(),
    val requestSubmitted: Boolean = false,
    val error: String? = null,
    val requestHistory: List<VideoRequest> = emptyList(),
    val rejectedRequestsCount: Int = 0,
    val pendingDeleteVideoIds: Set<String> = emptySet() // Video IDs with pending delete requests
)

@HiltViewModel
class VideoRequestViewModel @Inject constructor(
    private val requestRepository: VideoRequestRepository,
    private val parentVideoRepository: ParentVideoRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoRequestUiState())
    val uiState: StateFlow<VideoRequestUiState> = _uiState.asStateFlow()

    init {
        loadVideos()
        loadRequestHistory()
        loadPendingDeleteRequests()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            parentVideoRepository.getAllVideos()
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(videos = videos)
                }
        }
    }
    
    private fun loadRequestHistory() {
        viewModelScope.launch {
            requestRepository.getAllRequests()
                .onSuccess { requests ->
                    // Count only unread rejected requests (those with rejection messages that haven't been marked as read)
                    val rejectedCount = requests.count { request ->
                        request.status == RequestStatus.REJECTED && 
                        request.rejectionMessage.isNotEmpty() && 
                        !preferencesManager.isRejectionRead(request.requestId)
                    }
                    _uiState.value = _uiState.value.copy(
                        requestHistory = requests,
                        rejectedRequestsCount = rejectedCount
                    )
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
                            it.type == RequestType.DELETE && 
                            it.status == RequestStatus.PENDING &&
                            it.videoId != null
                        }
                        .mapNotNull { it.videoId }
                        .toSet()
                    _uiState.value = _uiState.value.copy(pendingDeleteVideoIds = pendingDeleteIds)
                }
        }
    }
    
    fun markRejectionAsRead(requestId: String) {
        preferencesManager.markRejectionAsRead(requestId)
        // Refresh count after marking as read
        loadRequestHistory()
    }
    
    fun markAllRejectionsAsRead() {
        viewModelScope.launch {
            requestRepository.getAllRequests()
                .onSuccess { requests ->
                    // Mark all rejected requests with messages as read
                    requests.forEach { request ->
                        if (request.status == RequestStatus.REJECTED && request.rejectionMessage.isNotEmpty()) {
                            preferencesManager.markRejectionAsRead(request.requestId)
                        }
                    }
                    // Refresh count
                    loadRequestHistory()
                }
        }
    }
    
    fun refreshRequestHistory() {
        loadRequestHistory()
        loadPendingDeleteRequests()
    }

    fun createRequest(type: RequestType, videoName: String, videoId: String?, message: String) {
        viewModelScope.launch {
            requestRepository.createRequest(type, videoName, videoId, message)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(requestSubmitted = true, error = null)
                    // Reset after 2 seconds
                    kotlinx.coroutines.delay(2000)
                    _uiState.value = _uiState.value.copy(requestSubmitted = false)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to submit request",
                        requestSubmitted = false
                    )
                }
        }
    }
}

