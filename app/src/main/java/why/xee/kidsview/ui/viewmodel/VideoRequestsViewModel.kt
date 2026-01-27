package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.RequestStatus
import why.xee.kidsview.data.model.RequestType
import why.xee.kidsview.data.model.VideoRequest
import why.xee.kidsview.data.repository.ParentVideoRepository
import why.xee.kidsview.data.repository.VideoRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoRequestsUiState(
    val requests: List<VideoRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingRequestCount: Int = 0,
    val successMessage: String? = null,
    val videoThumbnails: Map<String, String> = emptyMap() // videoId -> thumbnailUrl
)

@HiltViewModel
class VideoRequestsViewModel @Inject constructor(
    private val repository: VideoRequestRepository,
    private val parentVideoRepository: ParentVideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoRequestsUiState())
    val uiState: StateFlow<VideoRequestsUiState> = _uiState.asStateFlow()

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getAllRequests()
                .onSuccess { requests ->
                    val pendingCount = requests.count { it.status == RequestStatus.PENDING }
                    
                    // Load thumbnails for DELETE requests that have videoId
                    // Get all videos once, then build thumbnail map
                    val thumbnailMap = mutableMapOf<String, String>()
                    parentVideoRepository.getAllVideos()
                        .onSuccess { videos ->
                            requests.filter { 
                                it.type == RequestType.DELETE && it.videoId != null 
                            }.forEach { request ->
                                request.videoId?.let { videoId ->
                                    videos.find { it.videoId == videoId }?.let { video ->
                                        thumbnailMap[videoId] = video.thumbnail
                                    }
                                }
                            }
                        }
                    
                    _uiState.value = _uiState.value.copy(
                        requests = requests,
                        isLoading = false,
                        pendingRequestCount = pendingCount,
                        videoThumbnails = thumbnailMap
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load requests"
                    )
                }
        }
    }

    fun updateRequestStatus(requestId: String, status: RequestStatus, rejectionMessage: String = "") {
        viewModelScope.launch {
            // First, get the request to check if it's a DELETE request
            val allRequests = repository.getAllRequests().getOrNull() ?: emptyList()
            val request = allRequests.find { it.requestId == requestId }
            
            if (request != null) {
                // If it's a DELETE request being approved, actually delete the video from user's collection
                if (request.type == RequestType.DELETE && status == RequestStatus.APPROVED) {
                    if (request.videoId != null) {
                        // Delete the video from the user's private collection
                        parentVideoRepository.deleteVideo(request.videoId)
                            .onSuccess {
                                // Video deleted successfully, now update the request status
                                repository.updateRequestStatus(requestId, status, rejectionMessage)
                                    .onSuccess {
                                        _uiState.value = _uiState.value.copy(
                                            successMessage = "Video '${request.videoName}' has been removed"
                                        )
                                        loadRequests() // Refresh requests list
                                        // Clear success message after 3 seconds
                                        kotlinx.coroutines.delay(3000)
                                        _uiState.value = _uiState.value.copy(successMessage = null)
                                    }
                                    .onFailure { exception ->
                                        _uiState.value = _uiState.value.copy(
                                            error = "Video deleted but failed to update request status: ${exception.message}"
                                        )
                                    }
                            }
                            .onFailure { exception ->
                                _uiState.value = _uiState.value.copy(
                                    error = "Failed to delete video: ${exception.message}"
                                )
                                return@launch
                            }
                        return@launch // Exit early since we handled the status update above
                    } else {
                        // DELETE request without videoId - this shouldn't happen, but handle gracefully
                        _uiState.value = _uiState.value.copy(
                            error = "Cannot delete video: Video ID is missing"
                        )
                        return@launch
                    }
                }
            }
            
            // For non-DELETE requests or DELETE requests that are being rejected, just update the status
            repository.updateRequestStatus(requestId, status, rejectionMessage)
                .onSuccess {
                    loadRequests() // Refresh
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to update request status"
                    )
                }
        }
    }
    
    suspend fun getPendingRequestCount(): Int {
        return repository.getPendingRequestCount().getOrElse { 0 }
    }
}

