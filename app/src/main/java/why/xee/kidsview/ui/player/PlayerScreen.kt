package why.xee.kidsview.ui.player

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import why.xee.kidsview.utils.AdManager
import why.xee.kidsview.ui.components.ChildFriendlyPlayerControls
import why.xee.kidsview.ui.viewmodel.PlayerViewModel
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.data.preferences.AuthMode
import why.xee.kidsview.utils.ReviewerUnlockManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.draw.clip
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * Video Player Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoId: String,
    isParentMode: Boolean = false, // Only track time if false (kids mode)
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val preferencesManager = viewModel.preferencesManager
    val scope = rememberCoroutineScope()
    
    // Track the current video ID and mode to detect changes
    var currentVideoId by remember { mutableStateOf("") }
    var currentIsParentMode by remember { mutableStateOf(false) }
    var hasLoadedInitialVideo by remember { mutableStateOf(false) }
    var isVideoReady by remember { mutableStateOf(false) } // Track if current video is ready for playback control
    var localIsPlaying by remember { mutableStateOf(false) } // Track actual player state locally
    
    // CRITICAL: Ensure time limits are never applied in parent mode
    // This is especially important for favorites folder videos
    LaunchedEffect(isParentMode) {
        if (isParentMode) {
            // Explicitly clear time limit state when in parent mode
            // This ensures favorites and other parent-mode videos always play
            why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Parent mode detected, clearing time limit state")
            viewModel.clearTimeLimitState()
        }
    }
    
    LaunchedEffect(videoId, isParentMode) {
        // Load video on initial load or when video ID or mode changes
        val videoChanged = currentVideoId != videoId
        val modeChanged = currentIsParentMode != isParentMode
        
        if (videoChanged) {
            currentVideoId = videoId
            isVideoReady = false // Reset ready state when video changes
        }
        
        if (modeChanged) {
            currentIsParentMode = isParentMode
        }
        
        if (!hasLoadedInitialVideo || videoChanged || modeChanged) {
            hasLoadedInitialVideo = true
            // Load video and check time limit immediately (only for kids mode)
            // Always reload when mode changes to ensure correct time limit handling
            viewModel.loadVideo(videoId, isParentMode)
            // If time limit is exceeded, the checkTimeLimit() in loadVideo() will set isTimeLimitReached = true
            // This will prevent the player from rendering
        }
    }
    
    // Also watch for video ID changes from ViewModel (when next/previous is clicked)
    LaunchedEffect(uiState.videoId) {
        if (uiState.videoId.isNotEmpty() && uiState.videoId != currentVideoId) {
            currentVideoId = uiState.videoId
            isVideoReady = false // Reset ready state when video changes
            // Don't reset localIsPlaying here - it will be set to true after video loads and auto-plays
            // The onNextVideoClick/onPreviousVideoClick handlers will set it correctly
            // Ensure playerRef is updated when video changes
            // The YouTubePlayerView update block should handle this, but we ensure it here too
        }
    }
    
    // Restore portrait orientation when leaving this screen
    DisposableEffect(activity, isParentMode) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewModel.onVideoStop(isParentMode) // Ensure time is saved and tracking stops (only in kids mode)
        }
    }
    
    // Track if we should show error fallback
    var shouldHandleError by remember { mutableStateOf(false) }
    var errorVideoId by remember { mutableStateOf<String?>(null) }
    var showVideoErrorDialog by remember { mutableStateOf(false) }
    var playerRef by remember { mutableStateOf<com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer?>(null) }
    
    // Controls always visible (no auto-hide)
    var showControls by remember { mutableStateOf(true) }
    
    // Unlock YouTube controls state
    var isOverlayEnabled by remember { mutableStateOf(true) } // Overlay blocks YouTube controls when true
    var unlockRemainingSeconds by remember { mutableStateOf(0) } // Countdown timer for unlock
    
    // Password/PIN authentication dialog state
    var showUnlockAuthDialog by remember { mutableStateOf(false) }
    var enteredInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    val authMode = preferencesManager.getAuthMode()
    val isPinMode = authMode == AuthMode.PIN
    
    // Stop video when time limit is reached (only in kids mode)
    LaunchedEffect(uiState.isTimeLimitReached, isParentMode) {
        if (!isParentMode && uiState.isTimeLimitReached && playerRef != null) {
            try {
                playerRef?.pause()
                viewModel.onVideoStop(isParentMode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Handle error fallback: auto-skip to next video in kids mode, show error dialog in parent mode
    LaunchedEffect(shouldHandleError, errorVideoId, isParentMode, playerRef) {
        if (shouldHandleError && errorVideoId != null) {
            val failedVideoId = errorVideoId!!
            why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Handling video error - videoId=$failedVideoId, isParentMode=$isParentMode")
            
            if (!isParentMode) {
                // Kids Mode: Auto-skip to next video immediately to avoid bad user experience
                // Ensure error dialog is not shown
                showVideoErrorDialog = false
                scope.launch {
                    why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Kids mode - attempting auto-skip to next video")
                    
                    // Ensure approved videos list is loaded (in case it wasn't loaded yet)
                    if (viewModel.uiState.value.approvedVideos.isEmpty()) {
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Approved videos list empty, reloading...")
                        viewModel.reloadApprovedVideos()
                        delay(300) // Wait for list to load
                    }
                    
                    // Get the current state and verify the failed video ID matches what we're trying to play
                    val currentState = viewModel.uiState.value
                    val currentPlayingVideoId = currentState.videoId.ifEmpty { currentVideoId }
                    
                    // CRITICAL: Only delete if the failed video ID matches the currently playing video
                    // This prevents deleting good videos when errors occur
                    // Also verify the failed video exists in the approved videos list
                    val failedVideoExists = currentState.approvedVideos.any { it.videoId == failedVideoId }
                    
                    if (failedVideoId != currentPlayingVideoId) {
                        why.xee.kidsview.utils.AppLogger.w("PlayerScreen: Failed video ID ($failedVideoId) doesn't match current video ($currentPlayingVideoId), skipping deletion")
                        // Still try to get next video, but don't delete anything
                        val nextVideoId = viewModel.getNextVideoId()
                        if (nextVideoId != null && nextVideoId != failedVideoId) {
                            viewModel.loadVideo(nextVideoId, isParentMode)
                            delay(200)
                            playerRef?.loadVideo(nextVideoId, 0f)
                            currentVideoId = nextVideoId
                        } else {
                            onNavigateBack()
                        }
                        return@launch
                    }
                    
                    if (!failedVideoExists) {
                        why.xee.kidsview.utils.AppLogger.w("PlayerScreen: Failed video ID ($failedVideoId) not found in approved videos list, skipping deletion")
                        // Video not in list, just try to get next video
                        val nextVideoId = viewModel.getNextVideoId()
                        if (nextVideoId != null && nextVideoId != failedVideoId) {
                            viewModel.loadVideo(nextVideoId, isParentMode)
                            delay(200)
                            playerRef?.loadVideo(nextVideoId, 0f)
                            currentVideoId = nextVideoId
                        } else {
                            onNavigateBack()
                        }
                        return@launch
                    }
                    
                    // Get the current index of the failed video BEFORE deletion
                    val failedVideoIndex = currentState.approvedVideos.indexOfFirst { it.videoId == failedVideoId }
                    why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Failed video index = $failedVideoIndex, currentVideoIndex = ${currentState.currentVideoIndex}, currentVideoId = $currentPlayingVideoId, failedVideoId = $failedVideoId")
                    
                    // Delete ONLY the failed video (the one that actually failed)
                    // Wait for deletion to complete before proceeding
                    why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Deleting failed video: $failedVideoId")
                    val deleteResult = viewModel.deleteInaccessibleVideo(failedVideoId)
                    
                    if (deleteResult.isSuccess) {
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Video deleted successfully, reloading list...")
                    } else {
                        why.xee.kidsview.utils.AppLogger.w("PlayerScreen: Failed to delete video, but continuing with next video logic")
                    }
                    
                    // Reload the approved videos list after deletion, passing the deleted video ID
                    viewModel.reloadApprovedVideos(failedVideoId)
                    delay(500) // Wait for list to reload (longer delay for slower devices)
                    
                    // Try to get next video after deletion and reload
                    var nextVideoId: String? = null
                    val reloadedState = viewModel.uiState.value
                    
                    if (failedVideoIndex >= 0 && failedVideoIndex < reloadedState.approvedVideos.size) {
                        // After deletion, the video that was at failedVideoIndex+1 is now at failedVideoIndex
                        nextVideoId = reloadedState.approvedVideos[failedVideoIndex].videoId
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: After reload, found next video at index $failedVideoIndex: $nextVideoId")
                    } else if (failedVideoIndex >= 0 && failedVideoIndex == reloadedState.approvedVideos.size) {
                        // Deleted video was the last one, no next video
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Deleted video was the last one, no next video")
                    } else {
                        // Try using getNextVideoId() as fallback
                        nextVideoId = viewModel.getNextVideoId()
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Using getNextVideoId() fallback: $nextVideoId")
                    }
                    
                    // If still no next video, try one more time with a fresh reload
                    if (nextVideoId == null || nextVideoId == failedVideoId) {
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Next video not found, trying one more reload...")
                        viewModel.reloadApprovedVideos()
                        delay(300)
                        nextVideoId = viewModel.getNextVideoId()
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: After second reload, next video: $nextVideoId")
                    }
                    
                    if (nextVideoId != null && nextVideoId != failedVideoId && nextVideoId.isNotEmpty()) {
                        // Load and play next video automatically
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Loading next video: $nextVideoId")
                        viewModel.loadVideo(nextVideoId, isParentMode)
                        delay(300) // Small delay before loading video (longer for slower devices)
                        try {
                            playerRef?.loadVideo(nextVideoId, 0f)
                            // Update currentVideoId to reflect the new video
                            currentVideoId = nextVideoId
                            why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Successfully loaded next video: $nextVideoId")
                        } catch (e: Exception) {
                            why.xee.kidsview.utils.AppLogger.e("PlayerScreen: Error loading next video: $nextVideoId", e)
                            // If loading fails, try navigating back
                            onNavigateBack()
                        }
                    } else {
                        // No next video available - go back to list
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: No next video available, navigating back")
                        onNavigateBack()
                    }
                }
            } else {
                // Parent Mode: Show error dialog
                why.xee.kidsview.utils.AppLogger.d("PlayerScreen: Parent mode - showing error dialog")
                // Delete the inaccessible video from Firestore (async, don't wait)
                scope.launch {
                    viewModel.deleteInaccessibleVideo(failedVideoId)
                }
                showVideoErrorDialog = true
            }
            
            // Reset error state
            shouldHandleError = false
            errorVideoId = null
        }
    }
    
    // Time limit reached - show message overlay (only in kids mode)
    if (!isParentMode && uiState.isTimeLimitReached) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⏰",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "Time Limit Reached",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Contact your parent for assistance",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    } else {
        // Show player (skip time limit check in parent mode)
        // Always show fullscreen player without top bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Fullscreen YouTube Player - landscape will be set when video starts playing
            // Use uiState.videoId to ensure it updates when next/previous video is clicked
            // The key(videoId) in YouTubePlayerView will recreate the view when videoId changes
            // This ensures playerRef is properly updated via onPlayerReady callback
            YouTubePlayerView(
                videoId = uiState.videoId.ifEmpty { videoId },
                modifier = Modifier.fillMaxSize(),
                isParentMode = isParentMode, // Pass mode to configure player style
                onPlayerReady = { player ->
                    // CRITICAL: Update playerRef when player is ready (including after video changes)
                    // This ensures buttons continue to work after next/previous navigation
                    playerRef = player
                    // Also start progress tracking when player is ready
                    if (!isParentMode && !uiState.isTimeLimitReached) {
                        viewModel.startProgressTracking(player)
                    }
                },
                onPlayerRefUpdate = { player ->
                    // CRITICAL: Update playerRef from update block when video changes
                    // This ensures buttons continue to work after next/previous navigation
                    playerRef = player
                    // Restart progress tracking for the new video
                    if (!isParentMode && !uiState.isTimeLimitReached) {
                        viewModel.startProgressTracking(player)
                    }
                },
                isParentModeForUpdate = isParentMode,
                viewModelForUpdate = viewModel,
                onVideoReady = { 
                    // Mark video as ready for playback control
                    isVideoReady = true
                    why.xee.kidsview.utils.AppLogger.d("Video ready: videoId=${uiState.videoId.ifEmpty { currentVideoId }}, isVideoReady=true")
                    // Check time limit before allowing video to play (only in kids mode)
                    if (!isParentMode && uiState.isTimeLimitReached) {
                        playerRef?.pause()
                        return@YouTubePlayerView
                    }
                    // Set sensor-based landscape orientation when video is ready (allows both left and right landscape)
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE 
                },
                onVideoPlay = { 
                    // Double-check time limit before playing (only in kids mode)
                    if (!isParentMode && uiState.isTimeLimitReached) {
                        playerRef?.pause()
                        viewModel.onVideoStop(isParentMode)
                    } else {
                        viewModel.onVideoPlay(isParentMode)
                        localIsPlaying = true // Update local state immediately
                        why.xee.kidsview.utils.AppLogger.d("PlayerScreen: onVideoPlay called, localIsPlaying set to true")
                        showControls = true // Show controls when video plays
                    }
                },
                onVideoPause = { 
                    viewModel.onVideoPause(isParentMode)
                    localIsPlaying = false // Update local state immediately
                    why.xee.kidsview.utils.AppLogger.d("PlayerScreen: onVideoPause called, localIsPlaying set to false")
                    showControls = true // Show controls when paused
                },
                onVideoStop = { viewModel.onVideoStop(isParentMode) },
                onVideoEnded = {
                    // CRITICAL: Handle video end - auto-play next or return to list
                    if (!isParentMode) {
                        viewModel.onVideoStop(isParentMode)
                        val nextVideoId = viewModel.getNextVideoId()
                        if (nextVideoId != null) {
                            // Auto-play next video
                            viewModel.loadVideo(nextVideoId, isParentMode)
                            playerRef?.loadVideo(nextVideoId, 0f)
                        } else {
                            // No more videos - return to list
                            onNavigateBack()
                        }
                    }
                },
                onError = { errorVideo ->
                    // Handle YouTube player error (e.g., VIDEO_NOT_PLAYABLE_IN_EMBED)
                    why.xee.kidsview.utils.AppLogger.e("PlayerScreen: Video error detected - videoId=$errorVideo, isParentMode=$isParentMode")
                    shouldHandleError = true
                    errorVideoId = errorVideo
                },
                onProgressUpdate = { currentTime, duration ->
                    // Update progress for custom progress bar
                    viewModel.updateVideoProgress(currentTime, duration)
                }
            )
            
            // Kids Mode Lock Feature: Full-screen transparent overlay to block touch input
            // CRITICAL: Only active in Kids Mode (NOT Parent Mode)
            // In Parent Mode: No overlay, full YouTube controls are accessible
            if (!isParentMode) {
                // Full-screen transparent overlay that blocks all touch input when locked
                // Positioned above YouTube player but below custom controls
                // YouTube UI remains fully visible - we only intercept touch events
                if (isOverlayEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0.85f) // Above YouTube player (0.5f), below custom controls (2f)
                            .background(Color.Transparent) // Fully transparent - YouTube UI visible
                            .clickable(enabled = true) { 
                                // Block all touch events - prevents accidental taps by children
                                // YouTube player and controls remain visible but non-interactive
                            }
                    )
                } else {
                    // Overlay is unlocked - show status message
                    if (unlockRemainingSeconds > 0) {
                        Card(
                        modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 80.dp)
                                .zIndex(1f), // Above overlay, below custom controls
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Controls unlocked for $unlockRemainingSeconds seconds",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            // Parent Mode: No overlay - YouTube controls are fully accessible
            
            // Player Controls - Show in both Kids Mode and Parent Mode
            // In Parent Mode: Always visible with back button for better navigation
            // In Kids Mode: Shows with lock overlay feature
            // KIDS MODE LOCK FEATURE IMPLEMENTATION:
            // 
            // 1. OVERLAY PLACEMENT IN VIEW HIERARCHY:
            //    - Full-screen transparent Box with zIndex(0.85f)
            //    - Positioned above YouTube player (0.5f) but below custom controls (2f)
            //    - YouTube player UI remains fully visible (overlay is transparent)
            //    - Custom controls (back, play/pause, next/previous) remain functional above overlay
            //
            // 2. TOUCH INTERCEPTION:
            //    - Overlay uses .clickable(enabled = true) to intercept all touch events
            //    - When isOverlayEnabled=true: All touches are blocked, YouTube controls non-interactive
            //    - When isOverlayEnabled=false: Overlay removed, YouTube controls fully interactive
            //    - YouTube branding and controls remain visible per YouTube policy
            //
            // 3. 5-SECOND AUTO-LOCK TIMER:
            //    - Lock button (UnlockControlsButton) sets isOverlayEnabled=false and unlockRemainingSeconds=5
            //    - Coroutine counts down from 5 to 0, updating unlockRemainingSeconds each second
            //    - After 5 seconds, isOverlayEnabled=true and unlockRemainingSeconds=0 (auto-lock)
            //    - User can unlock again at any time by pressing the lock button
            //
            // 4. LOCK BUTTON:
            //    - Reuses existing UnlockControlsButton in top app bar (next to back button)
            //    - Shows lock icon when locked, countdown (5,4,3,2,1) when unlocked
            //    - Button always accessible and functional (zIndex 10f)
            
            ChildFriendlyPlayerControls(
                    isPlaying = localIsPlaying, // Use local state which updates immediately
                    currentTime = uiState.currentTime,
                    duration = uiState.videoDuration,
                    hasPreviousVideo = uiState.approvedVideos.isNotEmpty() && 
                                     uiState.currentVideoIndex > 0,
                    hasNextVideo = uiState.approvedVideos.isNotEmpty() && 
                                 uiState.currentVideoIndex >= 0 &&
                                 uiState.currentVideoIndex < uiState.approvedVideos.size - 1,
                    isParentMode = isParentMode, // Pass parent mode to control button visibility
                    onPlayPauseClick = {
                        // Ensure playerRef is valid - it should be set in onPlayerReady or onPlayerRefUpdate
                        val currentPlayer = playerRef
                        
                        if (currentPlayer != null) {
                            try {
                                // Always toggle based on current local state
                                // If there's a mismatch, the onVideoPlay/onVideoPause callbacks will correct it
                                if (localIsPlaying) {
                                    // Video is playing (or should be) - pause it
                                    currentPlayer.pause()
                                    why.xee.kidsview.utils.AppLogger.d("PlayPause: Pausing video, localIsPlaying=$localIsPlaying")
                                    // Immediately update local state optimistically
                                    localIsPlaying = false
                                } else {
                                    // Video is paused (or should be) - play it
                                    currentPlayer.play()
                                    why.xee.kidsview.utils.AppLogger.d("PlayPause: Playing video, localIsPlaying=$localIsPlaying")
                                    // Immediately update local state optimistically
                                    localIsPlaying = true
                                }
                            } catch (e: Exception) {
                                why.xee.kidsview.utils.AppLogger.e("Error controlling playback: ${e.message}", e)
                            }
                        } else {
                            why.xee.kidsview.utils.AppLogger.w("Cannot control playback: playerRef is null, localIsPlaying=$localIsPlaying, uiState.isVideoPlaying=${uiState.isVideoPlaying}, currentVideoId=$currentVideoId")
                        }
                    },
                    onPreviousVideoClick = {
                        val previousVideoId: String? = viewModel.playPreviousVideo()
                        why.xee.kidsview.utils.AppLogger.d("Previous button clicked, previousVideoId: $previousVideoId")
                        if (previousVideoId != null) {
                            viewModel.onVideoStop(isParentMode)
                            // Reset video ready state when navigating to a new video
                            isVideoReady = false
                            // Update current video ID - this will trigger YouTubePlayerView to recreate with new videoId
                            // The key(videoId) in YouTubePlayerView will cause it to recreate, and onPlayerReady will be called again
                            currentVideoId = previousVideoId
                            viewModel.loadVideo(previousVideoId, isParentMode)
                            // Wait a moment for the player to update, then load the video
                            // The update block in YouTubePlayerView will handle loading, but we also try direct load
                            scope.launch {
                                delay(100) // Small delay to ensure player is ready
                                playerRef?.loadVideo(previousVideoId, 0f)
                                // Video auto-plays after loadVideo, so set playing state to true
                                // Small additional delay to account for video starting
                                delay(200)
                                localIsPlaying = true
                                why.xee.kidsview.utils.AppLogger.d("Previous video loaded, localIsPlaying set to true")
                            }
                        } else {
                            why.xee.kidsview.utils.AppLogger.w("Previous video ID is null, cannot navigate")
                        }
                    },
                    onNextVideoClick = {
                        val nextVideoId: String? = viewModel.playNextVideo()
                        why.xee.kidsview.utils.AppLogger.d("Next button clicked, nextVideoId: $nextVideoId")
                        if (nextVideoId != null) {
                            viewModel.onVideoStop(isParentMode)
                            // Reset video ready state when navigating to a new video
                            isVideoReady = false
                            // Update current video ID - this will trigger YouTubePlayerView to recreate with new videoId
                            // The key(videoId) in YouTubePlayerView will cause it to recreate, and onPlayerReady will be called again
                            currentVideoId = nextVideoId
                            viewModel.loadVideo(nextVideoId, isParentMode)
                            // Wait a moment for the player to update, then load the video
                            // The update block in YouTubePlayerView will handle loading, but we also try direct load
                            scope.launch {
                                delay(100) // Small delay to ensure player is ready
                                playerRef?.loadVideo(nextVideoId, 0f)
                                // Video auto-plays after loadVideo, so set playing state to true
                                // Small additional delay to account for video starting
                                delay(200)
                                localIsPlaying = true
                                why.xee.kidsview.utils.AppLogger.d("Next video loaded, localIsPlaying set to true")
                            }
                        } else {
                            why.xee.kidsview.utils.AppLogger.w("Next video ID is null, cannot navigate")
                        }
                    },
                    onBackClick = {
                        viewModel.onVideoStop(isParentMode)
                        onNavigateBack()
                    },
                    onUnlockControlsClick = {
                        // Check reviewer unlock first - if active, immediately grant access
                        if (ReviewerUnlockManager.isUnlocked()) {
                            // Reviewer mode active - immediately unlock without showing dialog
                            isOverlayEnabled = false
                            unlockRemainingSeconds = 5
                            scope.launch {
                                for (i in 5 downTo 1) {
                                    delay(1000)
                                    unlockRemainingSeconds = i - 1
                                }
                                // Re-enable overlay after countdown
                                isOverlayEnabled = true
                                unlockRemainingSeconds = 0
                            }
                        } else {
                            // Show password/PIN authentication dialog
                            showUnlockAuthDialog = true
                            enteredInput = ""
                            authError = null
                        }
                    },
                    unlockRemainingSeconds = unlockRemainingSeconds,
                    onSeek = { seekTime ->
                        // Seek to the specified time in the video
                        playerRef?.seekTo(seekTime)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f) // Our controls on top - buttons and seek bar will work
                )
            
            // YouTube Branding (Required by YouTube API Terms of Service)
            // Must be visible and not hidden - placed at bottom-right
            // Show in parent mode, or in kids mode (but smaller and less prominent)
            if (isParentMode) {
                Text(
                    text = "Powered by YouTube",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .zIndex(0.5f)
                )
            } else {
                // In kids mode, show smaller branding that doesn't interfere with controls
                Text(
                    text = "YouTube",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 80.dp, end = 12.dp) // Above controls
                        .zIndex(0.5f)
                )
            }
        }
        
        // Password/PIN Authentication Dialog for Unlock
        // Don't show dialog if reviewer unlock is active
        if (showUnlockAuthDialog && !isParentMode && !ReviewerUnlockManager.isUnlocked()) {
            UnlockAuthDialog(
                isPinMode = isPinMode,
                enteredInput = enteredInput,
                onInputChange = { newInput ->
                    enteredInput = newInput
                    authError = null
                    
                    // Auto-verify when required length is reached for PIN
                    if (isPinMode && newInput.length >= 6) {
                        val isValid = preferencesManager.verifyAuth(newInput)
                        if (isValid) {
                            showUnlockAuthDialog = false
                            enteredInput = ""
                            authError = null
                            // Unlock overlay for 5 seconds
                            isOverlayEnabled = false
                            unlockRemainingSeconds = 5
                            scope.launch {
                                for (i in 5 downTo 1) {
                                    delay(1000)
                                    unlockRemainingSeconds = i - 1
                                }
                                // Re-enable overlay after countdown
                                isOverlayEnabled = true
                                unlockRemainingSeconds = 0
                            }
                        } else {
                            authError = "Incorrect PIN. Please try again."
                            scope.launch {
                                delay(1000)
                                enteredInput = ""
                                authError = null
                            }
                        }
                    }
                },
                onVerifyClick = {
                    if (!isPinMode && enteredInput.length >= 8) {
                        val isValid = preferencesManager.verifyAuth(enteredInput)
                        if (isValid) {
                            showUnlockAuthDialog = false
                            enteredInput = ""
                            authError = null
                            // Unlock overlay for 5 seconds
                            isOverlayEnabled = false
                            unlockRemainingSeconds = 5
                            scope.launch {
                                for (i in 5 downTo 1) {
                                    delay(1000)
                                    unlockRemainingSeconds = i - 1
                                }
                                // Re-enable overlay after countdown
                                isOverlayEnabled = true
                                unlockRemainingSeconds = 0
                            }
                        } else {
                            authError = "Incorrect Password. Please try again."
                            scope.launch {
                                delay(1000)
                                enteredInput = ""
                                authError = null
                            }
                        }
                    }
                },
                onDismiss = {
                    showUnlockAuthDialog = false
                    enteredInput = ""
                    authError = null
                },
                error = authError
            )
        }
        
        // Video Error Dialog - shown when video cannot be played (only in parent mode)
        if (showVideoErrorDialog && isParentMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "Video Not Compatible",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "This video is not compatible with the app and cannot be played.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                showVideoErrorDialog = false
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Go Back to Video List")
                        }
                    }
                }
            }
        }
    }
}

/**
 * YouTube Player View Composable
 * @param isParentMode If false (Kids Mode), uses MINIMAL player style and auto-closes on video end
 */
@Composable
fun YouTubePlayerView(
    videoId: String,
    modifier: Modifier = Modifier,
    isParentMode: Boolean = true, // Default to parent mode for backward compatibility
    onVideoReady: (() -> Unit)? = null,
    onVideoPlay: (() -> Unit)? = null,
    onVideoPause: (() -> Unit)? = null,
    onVideoStop: (() -> Unit)? = null,
    onVideoEnded: (() -> Unit)? = null, // Callback when video ends (for Kids Mode auto-close)
    onPlayerReady: ((YouTubePlayer) -> Unit)? = null,
    onPlayerRefUpdate: ((YouTubePlayer) -> Unit)? = null, // Callback to update playerRef from update block
    isParentModeForUpdate: Boolean = true, // Pass isParentMode for update block
    viewModelForUpdate: PlayerViewModel? = null, // Pass viewModel for update block
    onError: ((String) -> Unit)? = null,
    onProgressUpdate: ((Float, Float) -> Unit)? = null // (currentTime, duration) in seconds
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    if (videoId.isNotEmpty()) {
        androidx.compose.runtime.key(videoId) {
            AndroidView(
            factory = { context ->
                try {
                    // Create the YouTubePlayerView instance
                    YouTubePlayerView(context).apply {
                        // Disable automatic initialization since we're initializing manually
                        enableAutomaticInitialization = false
                        
                        // Configure player options based on mode
                        // CRITICAL for Kids Mode: MINIMAL style with limited controls suitable for child safety
                        val playerOptions = IFramePlayerOptions.Builder(context)
                            .apply {
                                if (!isParentMode) {
                                    // Kids Mode: MINIMAL player style (no native controls)
                                    // This prevents YouTube's native seekbar from appearing
                                    controls(0) // MINIMAL style - no YouTube native controls
                                    rel(0) // Don't show related videos (officially supported parameter)
                                    ivLoadPolicy(3) // Hide video annotations
                                    ccLoadPolicy(0) // Don't show captions by default
                                } else {
                                    // Parent Mode: Normal player with controls
                                    controls(1) // Show player controls
                                    rel(0) // Don't show related videos (still disabled for safety)
                                    ivLoadPolicy(3) // Hide video annotations
                                    ccLoadPolicy(0) // Don't show captions by default
                                }
                            }
                            .build()
                        
                        // Store videoId in a local variable to capture it correctly
                        val currentVideoId = videoId
                        
                        // Store duration when available (use array to make it mutable and accessible)
                        val storedDuration = floatArrayOf(0f)
                        
                        // Add the view to the lifecycle of the composable
                        lifecycleOwner.lifecycle.addObserver(this)
                        
                        // Initialize the player with a listener
                        initialize(
                            object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    // CRITICAL: Always update playerRef when player is ready
                                    // This ensures play/pause works after video changes
                                    onPlayerReady?.invoke(youTubePlayer)
                                    why.xee.kidsview.utils.AppLogger.d("YouTubePlayer: onReady called, playerRef updated for videoId=$currentVideoId")
                                    
                                    // Check time limit before loading video
                                    val preferencesManager = (context.applicationContext as? android.app.Application)?.let {
                                        // We'll check in PlayerViewModel instead - this is just for safety
                                        // The actual check happens in PlayerViewModel.checkTimeLimit()
                                    }
                                    
                                    // Load the video once the player is ready (but it may be paused immediately if time limit exceeded)
                                    // CRITICAL: Load video without playlist context
                                    // This ensures YouTube doesn't detect playlist relationships
                                    try {
                                        if (currentVideoId.isNotEmpty()) {
                                            // Load video by ID only - no playlist parameters
                                            // Player options (rel=0) prevent playlist UI from appearing
                                            youTubePlayer.loadVideo(currentVideoId, 0f)
                                            // Set landscape orientation when video is ready to play
                                            onVideoReady?.invoke()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        // Trigger error handler on exception
                                        onError?.invoke(currentVideoId)
                                    }
                                }
                                
                                override fun onStateChange(
                                    youTubePlayer: YouTubePlayer,
                                    state: PlayerConstants.PlayerState
                                ) {
                                    super.onStateChange(youTubePlayer, state)
                                    why.xee.kidsview.utils.AppLogger.d("YouTubePlayer: onStateChange called with state=$state")
                                    when (state) {
                                        PlayerConstants.PlayerState.PLAYING -> {
                                            why.xee.kidsview.utils.AppLogger.d("YouTubePlayer: State is PLAYING, calling onVideoPlay")
                                            onVideoPlay?.invoke()
                                        }
                                        PlayerConstants.PlayerState.PAUSED -> {
                                            why.xee.kidsview.utils.AppLogger.d("YouTubePlayer: State is PAUSED, calling onVideoPause")
                                            onVideoPause?.invoke()
                                        }
                                        PlayerConstants.PlayerState.ENDED -> {
                                            // CRITICAL: Detect video end for Kids Mode auto-close
                                            onVideoStop?.invoke()
                                            // Trigger onVideoEnded callback (used for Kids Mode auto-close)
                                            onVideoEnded?.invoke()
                                        }
                                        PlayerConstants.PlayerState.UNKNOWN -> {
                                            onVideoStop?.invoke()
                                        }
                                        else -> {}
                                    }
                                }
                                
                                override fun onError(
                                    youTubePlayer: YouTubePlayer,
                                    error: PlayerConstants.PlayerError
                                ) {
                                    // Handle YouTube player errors (e.g., VIDEO_NOT_PLAYABLE_IN_EMBED)
                                    why.xee.kidsview.utils.AppLogger.e("Player error: $error for video: $currentVideoId")
                                    onError?.invoke(currentVideoId)
                                }
                                
                                override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {
                                    // Video loaded successfully
                                    super.onVideoId(youTubePlayer, videoId)
                                    // Mark video as ready when video ID is confirmed
                                    // This ensures play/pause works after video navigation
                                    // Note: onVideoReady should also set this, but this is a backup
                                    why.xee.kidsview.utils.AppLogger.d("YouTubePlayer: onVideoId called for videoId=$videoId")
                                }
                                
                                override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                    // Track current playback time for progress bar
                                    super.onCurrentSecond(youTubePlayer, second)
                                    // Use stored duration if available
                                    if (storedDuration[0] > 0) {
                                        onProgressUpdate?.invoke(second, storedDuration[0])
                                    }
                                }
                                
                                override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                                    // Track video duration
                                    super.onVideoDuration(youTubePlayer, duration)
                                    // Store duration for use in onCurrentSecond
                                    storedDuration[0] = duration
                                    // Duration is now stored and will be used in onCurrentSecond
                                }
                            },
                            playerOptions
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Return a placeholder view on error
                    android.view.View(context)
                }
            },
            update = { view ->
                // Update video when videoId changes
                if (view is YouTubePlayerView && videoId.isNotEmpty()) {
                    try {
                        view.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                            override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                                try {
                                    // CRITICAL: Update playerRef when video changes via update block
                                    // This ensures buttons continue to work after next/previous navigation
                                    // The playerRef is used by button handlers, so it must be kept in sync
                                    onPlayerRefUpdate?.invoke(youTubePlayer)
                                    why.xee.kidsview.utils.AppLogger.d("YouTubePlayer: update block - playerRef updated for videoId=$videoId")
                                    // Reset video ready state - will be set to true in onVideoReady
                                    // Note: We can't access isVideoReady here, but onVideoReady will set it
                                    // Load video by ID only - no playlist parameters
                                    // Player options ensure no playlist UI appears
                                    youTubePlayer.loadVideo(videoId, 0f)
                                    why.xee.kidsview.utils.AppLogger.d("YouTubePlayer: loadVideo called for videoId=$videoId in update block")
                                    // Note: Video will load and onStateChange will be called with the actual state
                                    // The optimistic state update in onPlayPauseClick ensures immediate UI response
                                    // Restart progress tracking for the new video
                                    if (!isParentModeForUpdate && viewModelForUpdate != null) {
                                        viewModelForUpdate.startProgressTracking(youTubePlayer)
                                    }
                                } catch (e: Exception) {
                                    why.xee.kidsview.utils.AppLogger.e("YouTubePlayer: Error in update block: ${e.message}", e)
                                    e.printStackTrace()
                                }
                            }
                        })
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            modifier = modifier
        )
        }
    }
}

/**
 * Authentication Dialog for Unlocking Player Controls
 */
@Composable
private fun UnlockAuthDialog(
    isPinMode: Boolean,
    enteredInput: String,
    onInputChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onDismiss: () -> Unit,
    error: String?
) {
    // Custom dialog that wraps content
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // No backdrop - dialog appears directly over content
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title section with close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    // Close button at top right
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .padding(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Title section - organized and centered
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isPinMode) "Enter Parent PIN" else "Enter Parent Password",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = if (isPinMode) 
                                "Enter 6-digit PIN" 
                            else 
                                "Enter password (min 8 chars)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // PIN Display (dots) - organized
                if (isPinMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        repeat(6) { index ->
                            val isFilled = index < enteredInput.length
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                            )
                        }
                    }
                }
                
                // Password Input Field
                if (!isPinMode) {
                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = enteredInput,
                        onValueChange = onInputChange,
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "👁️" else "👁️‍🗨️",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                }
                
                // Error message
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                
                // PIN Pad (only for PIN mode) - Standard keypad with minimal spacing
                if (isPinMode) {
                    Column(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: 1, 2, 3
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UnlockNumberButton("1") { onInputChange(enteredInput + "1") }
                            UnlockNumberButton("2") { onInputChange(enteredInput + "2") }
                            UnlockNumberButton("3") { onInputChange(enteredInput + "3") }
                        }
                        
                        // Row 2: 4, 5, 6
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UnlockNumberButton("4") { onInputChange(enteredInput + "4") }
                            UnlockNumberButton("5") { onInputChange(enteredInput + "5") }
                            UnlockNumberButton("6") { onInputChange(enteredInput + "6") }
                        }
                        
                        // Row 3: 7, 8, 9
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UnlockNumberButton("7") { onInputChange(enteredInput + "7") }
                            UnlockNumberButton("8") { onInputChange(enteredInput + "8") }
                            UnlockNumberButton("9") { onInputChange(enteredInput + "9") }
                        }
                        
                        // Row 4: Clear, 0, Delete
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UnlockNumberButton("C") { onInputChange("") }
                            UnlockNumberButton("0") { onInputChange(enteredInput + "0") }
                            UnlockNumberButton("⌫") { 
                                if (enteredInput.isNotEmpty()) {
                                    onInputChange(enteredInput.dropLast(1))
                                }
                            }
                        }
                    }
                }
                
                // Password mode buttons
                if (!isPinMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = onVerifyClick,
                            enabled = enteredInput.length >= 8,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Verify", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    // Cancel button for PIN mode - organized at bottom
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun UnlockNumberButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(50.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), // More transparent
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

