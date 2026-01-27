package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.data.repository.YouTubeRepository
import why.xee.kidsview.data.repository.ParentVideoRepository
import why.xee.kidsview.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Player Screen
 */
data class PlayerUiState(
    val videoId: String = "",
    val recommendedVideos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTimeLimitReached: Boolean = false,
    val isVideoPlaying: Boolean = false,
    val approvedVideos: List<FirestoreVideo> = emptyList(), // List of all approved videos for Kids Mode
    val currentVideoIndex: Int = -1, // Index of current video in approved videos list
    val videoDuration: Float = 0f, // Video duration in seconds
    val currentTime: Float = 0f, // Current playback time in seconds
    val showControls: Boolean = true // Show/hide custom controls overlay
)

/**
 * ViewModel for Player Screen
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    val preferencesManager: PreferencesManager, // Made public for access in PlayerScreen
    private val parentVideoRepository: ParentVideoRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    private var timeTrackingJob: Job? = null
    private var videoStartTime: Long = 0
    private var progressTrackingJob: Job? = null
    
    fun loadVideo(videoId: String, isParentMode: Boolean = false) {
        // CRITICAL: In parent mode, NEVER apply time limits
        // This ensures favorites and other parent-mode videos play without restrictions
        AppLogger.d("PlayerViewModel: loadVideo called with videoId=$videoId, isParentMode=$isParentMode")
        val isExceeded = if (!isParentMode) {
            preferencesManager.isTimeLimitEnabled() && preferencesManager.isTimeLimitExceeded()
        } else {
            false // Always false in parent mode - time limits never apply
        }
        AppLogger.d("PlayerViewModel: isExceeded=$isExceeded (should be false in parent mode)")
        
        // Load approved videos list for Kids Mode (for next video functionality)
        if (!isParentMode) {
            loadApprovedVideos(videoId)
        }
        
        _uiState.value = _uiState.value.copy(
            videoId = videoId,
            isTimeLimitReached = isExceeded, // Always false in parent mode
            isVideoPlaying = false,
            currentTime = 0f,
            videoDuration = 0f
        )
        loadRecommendedVideos()
        
        // Double-check time limit (only for kids mode)
        // This ensures bonus time added via rewarded ads is immediately recognized
        // IMPORTANT: Never check time limit in parent mode
        if (!isParentMode && !isExceeded) {
            checkTimeLimit()
        } else if (isParentMode) {
            // Explicitly clear time limit state in parent mode
            _uiState.value = _uiState.value.copy(isTimeLimitReached = false)
        }
    }
    
    private fun loadApprovedVideos(currentVideoId: String) {
        viewModelScope.launch {
            parentVideoRepository.getAllVideos()
                .onSuccess { videos ->
                    val currentIndex = videos.indexOfFirst { it.videoId == currentVideoId }
                    _uiState.value = _uiState.value.copy(
                        approvedVideos = videos,
                        currentVideoIndex = if (currentIndex >= 0) currentIndex else -1
                    )
                }
                .onFailure {
                    // If loading fails, continue without next video functionality
                    _uiState.value = _uiState.value.copy(
                        approvedVideos = emptyList(),
                        currentVideoIndex = -1
                    )
                }
        }
    }
    
    /**
     * Reload approved videos list (useful after deleting a video)
     * @param deletedVideoId The ID of the video that was deleted (to find next video)
     */
    fun reloadApprovedVideos(deletedVideoId: String? = null) {
        viewModelScope.launch {
            parentVideoRepository.getAllVideos()
                .onSuccess { videos ->
                    if (deletedVideoId != null) {
                        // Video was deleted - find the next video after the deleted one
                        // First, find what index the deleted video was at (in the old list)
                        val oldList = _uiState.value.approvedVideos
                        val deletedIndex = oldList.indexOfFirst { it.videoId == deletedVideoId }
                        
                        if (deletedIndex >= 0 && deletedIndex < oldList.size) {
                            // After deletion, the video that was at deletedIndex+1 is now at deletedIndex
                            // So we should use deletedIndex to get the next video
                            val newIndex = if (deletedIndex < videos.size) deletedIndex else videos.size - 1
                            _uiState.value = _uiState.value.copy(
                                approvedVideos = videos,
                                currentVideoIndex = if (newIndex >= 0 && newIndex < videos.size) newIndex else -1
                            )
                            AppLogger.d("PlayerViewModel: After deletion, next video index = $newIndex")
                        } else {
                            // Deleted video not found in old list, try to find current video
                            val currentVideoId = _uiState.value.videoId
                            val currentIndex = videos.indexOfFirst { it.videoId == currentVideoId }
                            _uiState.value = _uiState.value.copy(
                                approvedVideos = videos,
                                currentVideoIndex = if (currentIndex >= 0) currentIndex else -1
                            )
                        }
                    } else {
                        // Normal reload - find current video in new list
                        val currentVideoId = _uiState.value.videoId
                        val currentIndex = videos.indexOfFirst { it.videoId == currentVideoId }
                        _uiState.value = _uiState.value.copy(
                            approvedVideos = videos,
                            currentVideoIndex = if (currentIndex >= 0) currentIndex else -1
                        )
                    }
                }
                .onFailure {
                    // If loading fails, continue without next video functionality
                    _uiState.value = _uiState.value.copy(
                        approvedVideos = emptyList(),
                        currentVideoIndex = -1
                    )
                }
        }
    }
    
    fun getPreviousVideoId(): String? {
        val state = _uiState.value
        if (state.approvedVideos.isEmpty() || state.currentVideoIndex < 0) {
            return null
        }
        val previousIndex = state.currentVideoIndex - 1
        return if (previousIndex >= 0) {
            state.approvedVideos[previousIndex].videoId
        } else {
            null // No previous video
        }
    }
    
    fun getNextVideoId(): String? {
        val state = _uiState.value
        if (state.approvedVideos.isEmpty() || state.currentVideoIndex < 0) {
            return null
        }
        val nextIndex = state.currentVideoIndex + 1
        return if (nextIndex < state.approvedVideos.size) {
            state.approvedVideos[nextIndex].videoId
        } else {
            null // No more videos
        }
    }
    
    fun playPreviousVideo(): String? {
        val previousIndex = _uiState.value.currentVideoIndex - 1
        if (previousIndex >= 0 && previousIndex < _uiState.value.approvedVideos.size) {
            val previousVideo = _uiState.value.approvedVideos[previousIndex]
            _uiState.value = _uiState.value.copy(
                currentVideoIndex = previousIndex,
                videoId = previousVideo.videoId,
                currentTime = 0f,
                videoDuration = 0f,
                isVideoPlaying = false // Reset playing state when navigating
            )
            return previousVideo.videoId
        }
        return null
    }
    
    fun playNextVideo(): String? {
        val nextIndex = _uiState.value.currentVideoIndex + 1
        if (nextIndex >= 0 && nextIndex < _uiState.value.approvedVideos.size) {
            val nextVideo = _uiState.value.approvedVideos[nextIndex]
            _uiState.value = _uiState.value.copy(
                currentVideoIndex = nextIndex,
                videoId = nextVideo.videoId,
                currentTime = 0f,
                videoDuration = 0f,
                isVideoPlaying = false // Reset playing state when navigating
            )
            return nextVideo.videoId
        }
        return null
    }
    
    fun updateVideoProgress(currentTime: Float, duration: Float) {
        _uiState.value = _uiState.value.copy(
            currentTime = currentTime,
            videoDuration = duration
        )
    }
    
    fun toggleControlsVisibility() {
        _uiState.value = _uiState.value.copy(
            showControls = !_uiState.value.showControls
        )
    }
    
    fun onVideoPlay(isParentMode: Boolean = false) {
        if (_uiState.value.isVideoPlaying) return // Already playing
        
        // Only check time limit if NOT in parent mode
        if (!isParentMode && preferencesManager.isTimeLimitEnabled() && preferencesManager.isTimeLimitExceeded()) {
            _uiState.value = _uiState.value.copy(isTimeLimitReached = true, isVideoPlaying = false)
            stopTimeTracking()
            return
        }
        
        _uiState.value = _uiState.value.copy(isVideoPlaying = true)
        videoStartTime = System.currentTimeMillis()
        if (!isParentMode) {
            startTimeTracking() // Only track time in kids mode
        }
    }
    
    fun onVideoPause(isParentMode: Boolean = false) {
        if (!_uiState.value.isVideoPlaying) return
        stopTimeTracking()
        _uiState.value = _uiState.value.copy(isVideoPlaying = false)
        
        // Only save elapsed time if NOT in parent mode
        if (!isParentMode) {
            val elapsed = System.currentTimeMillis() - videoStartTime
            if (elapsed > 0) {
                preferencesManager.addTimeUsed(elapsed)
            }
        }
    }
    
    fun onVideoStop(isParentMode: Boolean = false) {
        stopTimeTracking()
        _uiState.value = _uiState.value.copy(isVideoPlaying = false)
        
        // Only save elapsed time if NOT in parent mode
        if (!isParentMode) {
            val elapsed = System.currentTimeMillis() - videoStartTime
            if (elapsed > 0) {
                preferencesManager.addTimeUsed(elapsed)
            }
        }
    }
    
    private fun startTimeTracking() {
        stopTimeTracking() // Stop any existing tracking
        
        if (!preferencesManager.isTimeLimitEnabled()) return
        
        timeTrackingJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Check every second
                
                if (!_uiState.value.isVideoPlaying) break
                
                // Check if time limit exceeded (this will refresh with new bonus time)
                // The checkTimeLimit() call ensures bonus time is recognized immediately
                if (preferencesManager.isTimeLimitExceeded()) {
                    // Double-check to ensure we're still over the limit after bonus time refresh
                    checkTimeLimit()
                    if (preferencesManager.isTimeLimitExceeded()) {
                        _uiState.value = _uiState.value.copy(
                            isTimeLimitReached = true,
                            isVideoPlaying = false
                        )
                        stopTimeTracking()
                        break
                    } else {
                        // Bonus time was added, continue playing
                        _uiState.value = _uiState.value.copy(isTimeLimitReached = false)
                    }
                }
                
                // Save time incrementally (every 10 seconds)
                val elapsed = System.currentTimeMillis() - videoStartTime
                if (elapsed > 10000) { // Every 10 seconds
                    preferencesManager.addTimeUsed(10000)
                    videoStartTime = System.currentTimeMillis()
                }
            }
        }
    }
    
    private fun stopTimeTracking() {
        timeTrackingJob?.cancel()
        timeTrackingJob = null
    }
    
    private fun checkTimeLimit() {
        // This method is only called when NOT in parent mode (guarded in loadVideo)
        // Double-check to ensure time limits never apply in parent mode
        if (preferencesManager.isTimeLimitEnabled() && preferencesManager.isTimeLimitExceeded()) {
            _uiState.value = _uiState.value.copy(isTimeLimitReached = true)
        } else {
            // Ensure time limit is not reached if disabled or not exceeded
            _uiState.value = _uiState.value.copy(isTimeLimitReached = false)
        }
    }
    
    /**
     * Clear time limit state - used to ensure parent mode videos always play
     * This is especially important for favorites folder videos
     */
    fun clearTimeLimitState() {
        _uiState.value = _uiState.value.copy(isTimeLimitReached = false)
    }
    
    private fun loadRecommendedVideos() {
        // Load recommended videos (using search as fallback)
        // In a real app, you'd use YouTube's related videos API
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                recommendedVideos = emptyList()
            )
        }
    }
    
    fun startProgressTracking(player: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer) {
        stopProgressTracking()
        // Progress tracking is handled via onCurrentSecond callback in YouTubePlayerView
        // This method is kept for compatibility but doesn't need to do anything
        // The actual tracking happens in the player listener callbacks
    }
    
    private fun stopProgressTracking() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }
    
    /**
     * Delete an inaccessible video from Firestore
     * Called when a video fails to play (e.g., embedding restrictions)
     * Returns a suspend function result to allow waiting for completion
     */
    suspend fun deleteInaccessibleVideo(videoId: String): Result<Unit> {
        return try {
            AppLogger.d("PlayerViewModel: Deleting inaccessible video: $videoId")
            val result = parentVideoRepository.deleteVideo(videoId)
            result.onSuccess {
                AppLogger.d("PlayerViewModel: Successfully deleted inaccessible video: $videoId")
            }.onFailure { exception ->
                AppLogger.e("PlayerViewModel: Failed to delete inaccessible video: $videoId", exception)
            }
            result
        } catch (e: Exception) {
            AppLogger.e("PlayerViewModel: Exception while deleting inaccessible video: $videoId", e)
            Result.failure(e)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Note: onVideoStop() defaults to kids mode, but it's safe because:
        // 1. In parent mode, time tracking was never started (isVideoPlaying = false)
        // 2. The method checks isVideoPlaying before saving time
        onVideoStop() // Save any remaining time (only if tracking was active)
        stopTimeTracking()
        stopProgressTracking()
    }
}

