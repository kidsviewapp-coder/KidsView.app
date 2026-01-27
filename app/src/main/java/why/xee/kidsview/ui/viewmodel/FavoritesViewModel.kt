package why.xee.kidsview.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import why.xee.kidsview.data.local.FavoriteVideo
import why.xee.kidsview.data.local.FavoriteVideoDao
import why.xee.kidsview.data.repository.FavoritesRepository
import why.xee.kidsview.data.repository.ParentVideoRepository
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Favorites Screen
 */
data class FavoritesUiState(
    val favorites: List<FavoriteVideo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null, // Success message for favorite/unfavorite actions
    val promotedVideoId: String? = null, // Video ID that was just promoted to kids list (to show dialog)
    val promotedVideoTitle: String? = null, // Video title for the dialog
    val videosInFavoritesCategory: Set<String> = emptySet(), // Video IDs that are in Favorites collection (to show promote button)
    val savedVideoIds: Set<String> = emptySet() // All video IDs saved in Firestore (to determine if video can be promoted)
)

/**
 * ViewModel for Favorites Screen
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteDao: FavoriteVideoDao,
    private val favoritesRepository: FavoritesRepository,
    private val parentVideoRepository: ParentVideoRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()
    
    init {
        // Load favorites FIRST (from local DB) - this ensures UI shows data immediately
        loadFavorites()
        // Then sync with Firestore in background (non-blocking)
        syncFavoritesWithFirestore()
        loadVideosInFavoritesCategory()
    }
    
    /**
     * Sync local database with Firestore favorites collection
     * This ensures consistency between local DB and Firestore
     */
    private fun syncFavoritesWithFirestore() {
        viewModelScope.launch {
            try {
                // Get favorites from Firestore (source of truth)
                val firestoreResult = favoritesRepository.getAllFavorites()
                
                // Only proceed if Firestore access was successful
                // If there's a permission error, don't sync (keep local DB as is)
                if (firestoreResult.isFailure) {
                    val exception = firestoreResult.exceptionOrNull()
                    if (exception?.message?.contains("PERMISSION_DENIED") == true || 
                        exception?.message?.contains("permission") == true) {
                        AppLogger.w("FavoritesViewModel: Permission denied accessing Firestore - keeping local DB as is")
                        return@launch
                    }
                }
                
                val firestoreFavorites = firestoreResult.getOrNull() ?: emptyList()
                val firestoreVideoIds = firestoreFavorites.map { it.videoId }.toSet()
                AppLogger.d("FavoritesViewModel: Firestore has ${firestoreFavorites.size} favorites")
                
                // Get favorites from local DB
                val localFavorites = favoriteDao.getAllFavorites().first()
                val localVideoIds = localFavorites.map { it.videoId }.toSet()
                AppLogger.d("FavoritesViewModel: Local DB has ${localFavorites.size} favorites")
                
                // Only remove from local DB if Firestore access was successful
                // This prevents deleting favorites when there are permission errors
                if (firestoreResult.isSuccess) {
                    // Remove from local DB if not in Firestore (cleanup orphaned entries)
                    localVideoIds.forEach { videoId ->
                        if (!firestoreVideoIds.contains(videoId)) {
                            AppLogger.w("FavoritesViewModel: Removing orphaned favorite $videoId from local DB (not in Firestore)")
                            favoriteDao.deleteFavorite(videoId)
                        }
                    }
                }
                
                // Add to local DB if in Firestore but not in local DB
                firestoreFavorites.forEach { firestoreVideo ->
                    if (!localVideoIds.contains(firestoreVideo.videoId)) {
                        favoriteDao.insertFavorite(
                            FavoriteVideo(
                                videoId = firestoreVideo.videoId,
                                title = firestoreVideo.title,
                                description = "", // FirestoreVideo doesn't have description
                                channelTitle = firestoreVideo.channel,
                                thumbnailUrl = firestoreVideo.thumbnail,
                                publishedAt = "" // FirestoreVideo doesn't have publishedAt
                            )
                        )
                        AppLogger.d("FavoritesViewModel: Added ${firestoreVideo.videoId} to local DB (from Firestore)")
                    }
                }
                
                // For videos that were in local DB but not in Firestore, try to add them to Firestore
                // This handles cases where Firestore save failed during favoriting
                // But only if they still exist after cleanup
                val remainingLocalIds = favoriteDao.getAllFavorites().first().map { it.videoId }.toSet()
                remainingLocalIds.forEach { videoId ->
                    if (!firestoreVideoIds.contains(videoId)) {
                        val localFavorite = localFavorites.find { it.videoId == videoId }
                        if (localFavorite != null) {
                            // Try to add to Firestore
                            val firestoreVideo = FirestoreVideo(
                                videoId = localFavorite.videoId,
                                title = localFavorite.title,
                                channel = localFavorite.channelTitle,
                                thumbnail = localFavorite.thumbnailUrl,
                                duration = "",
                                quality = "auto",
                                categoryId = null
                            )
                            favoritesRepository.addToFavorites(firestoreVideo).onSuccess {
                                AppLogger.d("FavoritesViewModel: Synced $videoId to Firestore")
                            }.onFailure { exception ->
                                // If Firestore save fails, remove from local DB to keep them in sync
                                AppLogger.w("FavoritesViewModel: Failed to sync $videoId to Firestore, removing from local DB", exception)
                                favoriteDao.deleteFavorite(videoId)
                            }
                        }
                    }
                }
                
                AppLogger.d("FavoritesViewModel: Sync completed")
            } catch (e: Exception) {
                AppLogger.e("FavoritesViewModel: Failed to sync favorites with Firestore", e)
            }
        }
    }
    
    /**
     * Load all favorite videos from local database
     * This uses Flow so it automatically updates when favorites change
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            favoriteDao.getAllFavorites()
                .collect { favorites ->
                    AppLogger.d("FavoritesViewModel: Favorites Flow emitted ${favorites.size} favorites")
                    favorites.forEach { favorite ->
                        AppLogger.d("FavoritesViewModel: Favorite video: ${favorite.videoId} - ${favorite.title}")
                    }
                    // Update UI state immediately - use a new copy to force recomposition
                    _uiState.value = FavoritesUiState(
                        favorites = favorites,
                        isLoading = _uiState.value.isLoading,
                        error = _uiState.value.error,
                        successMessage = _uiState.value.successMessage,
                        promotedVideoId = _uiState.value.promotedVideoId,
                        promotedVideoTitle = _uiState.value.promotedVideoTitle,
                        videosInFavoritesCategory = _uiState.value.videosInFavoritesCategory,
                        savedVideoIds = _uiState.value.savedVideoIds
                    )
                    AppLogger.d("FavoritesViewModel: UI state updated with ${favorites.size} favorites")
                }
        }
    }
    
    /**
     * Force reload favorites from local database
     * Useful for debugging or manual refresh
     */
    fun reloadFavorites() {
        // The Flow will automatically emit when data changes
        // But we can trigger a sync to ensure consistency
        syncFavoritesWithFirestore()
    }
    
    /**
     * Sync favorites with Firestore (public function for manual sync)
     */
    fun syncFavorites() {
        syncFavoritesWithFirestore()
    }
    
    /**
     * Refresh the list of favorite videos in Firestore
     * This determines which videos can show the "Add to Kids List" button
     */
    fun refreshVideosInFavoritesCategory() {
        loadVideosInFavoritesCategory()
    }
    
    /**
     * Load videos that are in the separate Favorites collection in Firestore
     * This is used to determine which videos should show the "Add to Kids List" button
     * Also loads all saved video IDs to determine if videos can be promoted
     */
    private fun loadVideosInFavoritesCategory() {
        viewModelScope.launch {
            // Get favorite video IDs from the separate favorites collection
            val favoritesResult = favoritesRepository.getFavoriteVideoIds()
            // Get all saved video IDs to check if videos can be promoted
            val allVideosResult = parentVideoRepository.getAllVideos()
            
            if (favoritesResult.isSuccess) {
                val favoriteVideoIds = favoritesResult.getOrNull() ?: emptySet()
                val allSavedVideoIds = if (allVideosResult.isSuccess) {
                    allVideosResult.getOrNull()?.map { it.videoId }?.toSet() ?: emptySet()
                } else {
                    emptySet()
                }
                
                _uiState.value = _uiState.value.copy(
                    videosInFavoritesCategory = favoriteVideoIds,
                    savedVideoIds = allSavedVideoIds
                )
                AppLogger.d("FavoritesViewModel: Loaded ${favoriteVideoIds.size} videos in favorites collection")
            } else {
                // If loading fails, check if it's a permission error
                val error = favoritesResult.exceptionOrNull()
                val errorMsg = error?.message ?: "Unknown error"
                if (errorMsg.contains("PERMISSION_DENIED") || errorMsg.contains("permission")) {
                    AppLogger.w("FavoritesViewModel: Permission denied loading favorites collection - using local DB only")
                    // Don't show error to user - app will work with local DB only
                } else {
                    AppLogger.e("FavoritesViewModel: Failed to load favorites collection: $errorMsg", error)
                }
                // Use empty set and continue with local DB only
                _uiState.value = _uiState.value.copy(
                    videosInFavoritesCategory = emptySet(),
                    savedVideoIds = if (allVideosResult.isSuccess) {
                        allVideosResult.getOrNull()?.map { it.videoId }?.toSet() ?: emptySet()
                    } else {
                        emptySet()
                    }
                )
            }
        }
    }
    
    /**
     * Check if a video is favorited
     * Uses the UI state as the source of truth (most accurate and up-to-date)
     */
    suspend fun isFavorite(videoId: String): Boolean {
        // Check UI state first (this is the actual list shown to user)
        val isInUiState = _uiState.value.favorites.any { it.videoId == videoId }
        if (isInUiState) {
            return true
        }
        
        // If not in UI state, check local DB
        val isInLocalDB = favoriteDao.isFavorite(videoId)
        if (isInLocalDB) {
            // Video is in local DB but not in UI state - this is a sync issue
            // The Flow should have emitted it, but if it didn't, trigger a sync
            AppLogger.w("FavoritesViewModel: Video $videoId is in local DB but not in UI state - syncing")
            syncFavoritesWithFirestore()
            // After sync, check again
            return _uiState.value.favorites.any { it.videoId == videoId }
        }
        
        // If not in local DB, check Firestore (in case of sync issue)
        val isInFirestore = favoritesRepository.isInFavorites(videoId).getOrNull() ?: false
        if (isInFirestore) {
            // If in Firestore but not local DB, sync it
            AppLogger.w("FavoritesViewModel: Video $videoId is in Firestore but not local DB - syncing")
            syncFavoritesWithFirestore()
            // After sync, check again
            return _uiState.value.favorites.any { it.videoId == videoId }
        }
        
        return false
    }
    
    /**
     * Toggle favorite status for VideoItem (used in parent mode)
     */
    fun toggleFavorite(video: why.xee.kidsview.data.model.VideoItem) {
        viewModelScope.launch {
            // Check both UI state and local DB to ensure accuracy
            val isInUiState = _uiState.value.favorites.any { it.videoId == video.id }
            val isInLocalDB = favoriteDao.isFavorite(video.id)
            val isFavorite = isInUiState || isInLocalDB
            
            if (isFavorite) {
                // Remove from favorites - remove from BOTH local DB and Firestore
                if (isInLocalDB) {
                    favoriteDao.deleteFavorite(video.id)
                    AppLogger.d("FavoritesViewModel: Removed ${video.id} from local DB")
                    // Flow will automatically update the UI, but show message immediately
                    _uiState.value = _uiState.value.copy(successMessage = "Removed from favorites")
                }
                // Remove from Favorites collection in Firestore
                favoritesRepository.removeFromFavorites(video.id).onSuccess {
                    AppLogger.d("FavoritesViewModel: Removed ${video.id} from Firestore")
                    // Reload videos in favorites collection to update button visibility
                    loadVideosInFavoritesCategory()
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to remove from favorites"
                    AppLogger.e("FavoritesViewModel: Failed to remove ${video.id} from Firestore: $errorMsg", exception)
                    // Still show success for local removal, but warn about Firestore
                    loadVideosInFavoritesCategory()
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Removed from favorites (local only)",
                        error = errorMsg
                    )
                }
                // Clear messages after delay
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null, error = null)
            } else {
                // Add to favorites - add to BOTH local DB and Firestore
                val favoriteVideo = FavoriteVideo(
                    videoId = video.id,
                    title = video.title,
                    description = video.description,
                    channelTitle = video.channelTitle,
                    thumbnailUrl = video.thumbnailUrl,
                    publishedAt = video.publishedAt
                )
                
                // Add to local DB first (immediate UI update via Flow)
                favoriteDao.insertFavorite(favoriteVideo)
                AppLogger.d("FavoritesViewModel: Added ${video.id} to local DB")
                // Flow will automatically update the UI, but show message immediately
                _uiState.value = _uiState.value.copy(successMessage = "Added to favorites")
                
                // Add to Favorites collection in Firestore
                addVideoToFavoritesCollection(video).onSuccess {
                    // Wait a bit for Firestore operation to complete, then reload
                    kotlinx.coroutines.delay(500)
                    loadVideosInFavoritesCategory()
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: "Failed to sync to cloud"
                    AppLogger.e("FavoritesViewModel: Failed to add ${video.id} to Firestore: $errorMsg", exception)
                    // Still show success for local addition, but warn about Firestore
                    loadVideosInFavoritesCategory()
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Added to favorites (local only)",
                        error = errorMsg
                    )
                }
                // Clear messages after delay
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null, error = null)
            }
        }
    }
    
    /**
     * Toggle favorite status for FirestoreVideo (used in kids mode)
     */
    fun toggleFavorite(video: why.xee.kidsview.data.model.FirestoreVideo) {
        viewModelScope.launch {
            // Check both UI state and local DB to ensure accuracy
            val isInUiState = _uiState.value.favorites.any { it.videoId == video.videoId }
            val isInLocalDB = favoriteDao.isFavorite(video.videoId)
            val isFavorite = isInUiState || isInLocalDB
            
            if (isFavorite) {
                // Remove from favorites - remove from BOTH local DB and Firestore
                if (isInLocalDB) {
                favoriteDao.deleteFavorite(video.videoId)
                    AppLogger.d("FavoritesViewModel: Removed ${video.videoId} from local DB")
                }
                // Remove from Favorites collection in Firestore
                favoritesRepository.removeFromFavorites(video.videoId).onSuccess {
                    AppLogger.d("FavoritesViewModel: Removed ${video.videoId} from Firestore")
                }.onFailure { exception ->
                    AppLogger.e("FavoritesViewModel: Failed to remove ${video.videoId} from Firestore", exception)
                }
                // Reload videos in favorites collection to update button visibility
                loadVideosInFavoritesCategory()
                // Show success message
                _uiState.value = _uiState.value.copy(successMessage = "Removed from favorites")
                // Clear message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } else {
                // Add to favorites - add to BOTH local DB and Firestore
                val favoriteVideo = FavoriteVideo(
                        videoId = video.videoId,
                        title = video.title,
                        description = "", // FirestoreVideo doesn't have description
                        channelTitle = video.channel,
                        thumbnailUrl = video.thumbnail,
                        publishedAt = "" // FirestoreVideo doesn't have publishedAt
                    )
                
                // Add to local DB first (immediate UI update)
                favoriteDao.insertFavorite(favoriteVideo)
                AppLogger.d("FavoritesViewModel: Added ${video.videoId} to local DB")
                
                // Add to Favorites collection in Firestore
                favoritesRepository.addToFavorites(video).onSuccess {
                    AppLogger.d("FavoritesViewModel: Added ${video.videoId} to Firestore")
                }.onFailure { exception ->
                    AppLogger.e("FavoritesViewModel: Failed to add ${video.videoId} to Firestore", exception)
                }
                
                // Wait a bit for Firestore operation to complete, then reload
                kotlinx.coroutines.delay(500) // Small delay to ensure Firestore update completes
                loadVideosInFavoritesCategory()
                
                // Show success message
                _uiState.value = _uiState.value.copy(successMessage = "Added to favorites")
                // Clear message after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            }
        }
    }
    
    /**
     * Remove favorite
     */
    fun removeFavorite(videoId: String) {
        viewModelScope.launch {
            favoriteDao.deleteFavorite(videoId)
            // Remove from Favorites collection in Firestore
            favoritesRepository.removeFromFavorites(videoId)
            // Reload videos in favorites collection to update button visibility
            loadVideosInFavoritesCategory()
            // Show success message
            _uiState.value = _uiState.value.copy(successMessage = "Removed from favorites")
            // Clear message after 3 seconds
            kotlinx.coroutines.delay(3000)
            _uiState.value = _uiState.value.copy(successMessage = null)
        }
    }

    /**
     * Promote a favorited video to kids list by saving it to regular videos collection
     * This makes the video available in kids mode
     * Shows a dialog asking if user wants to keep it in favorites or delete it
     * @param videoId The ID of the video to promote
     */
    fun promoteToKidsList(videoId: String) {
        viewModelScope.launch {
            // Check if video is in favorites collection
            val isInFavorites = favoritesRepository.isInFavorites(videoId).getOrNull() ?: false
            
            if (isInFavorites) {
                // Get video from favorites collection
                val favoritesResult = favoritesRepository.getAllFavorites()
                val favoriteVideo = favoritesResult.getOrNull()?.find { it.videoId == videoId }
                
                if (favoriteVideo != null) {
                    val videoTitle = favoriteVideo.title
                    
                    // Check if video already exists in regular videos collection
                    val isVideoSaved = parentVideoRepository.isVideoSaved(videoId)
                    
                    if (!isVideoSaved) {
                        // Save video to regular videos collection (without category, so it appears in kids list)
                        val videoToSave = favoriteVideo.copy(categoryId = null)
                        parentVideoRepository.saveVideo(videoToSave).onSuccess {
                            AppLogger.d("FavoritesViewModel: Promoted video $videoId to kids list")
                            // Reload videos in favorites collection to update button visibility
                            loadVideosInFavoritesCategory()
                            // Set state to show dialog
                            _uiState.value = _uiState.value.copy(
                                promotedVideoId = videoId,
                                promotedVideoTitle = videoTitle
                            )
                        }.onFailure { exception ->
                            AppLogger.e("FavoritesViewModel: Failed to promote video to kids list", exception)
                            _uiState.value = _uiState.value.copy(error = "Failed to add video to kids list")
                            kotlinx.coroutines.delay(3000)
                            _uiState.value = _uiState.value.copy(error = null)
                        }
                    } else {
                        // Video already exists in regular collection, just show dialog
                        loadVideosInFavoritesCategory()
                        _uiState.value = _uiState.value.copy(
                            promotedVideoId = videoId,
                            promotedVideoTitle = videoTitle
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(error = "Video not found in favorites")
                    kotlinx.coroutines.delay(3000)
                    _uiState.value = _uiState.value.copy(error = null)
                }
            } else {
                _uiState.value = _uiState.value.copy(error = "Video is not in favorites")
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(error = null)
            }
        }
    }

    /**
     * Handle user's choice after promoting video to kids list
     * @param videoId The ID of the video
     * @param keepInFavorites If true, keeps video in favorites; if false, removes from favorites
     */
    fun handlePromoteChoice(videoId: String, keepInFavorites: Boolean) {
        viewModelScope.launch {
            if (keepInFavorites) {
                // Keep in favorites - video is already promoted, just clear the dialog state
                _uiState.value = _uiState.value.copy(
                    promotedVideoId = null,
                    promotedVideoTitle = null,
                    successMessage = "Video added to kids list and kept in favorites"
                )
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            } else {
                // Remove from favorites
                favoriteDao.deleteFavorite(videoId)
                // Remove from Favorites collection in Firestore
                favoritesRepository.removeFromFavorites(videoId)
                // Reload videos in favorites collection to update button visibility
                loadVideosInFavoritesCategory()
                // Just update the UI state
                _uiState.value = _uiState.value.copy(
                    promotedVideoId = null,
                    promotedVideoTitle = null,
                    successMessage = "Video added to kids list and removed from favorites"
                )
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(successMessage = null)
            }
        }
    }

    /**
     * Dismiss the promote dialog without making a choice
     */
    fun dismissPromoteDialog() {
        _uiState.value = _uiState.value.copy(
            promotedVideoId = null,
            promotedVideoTitle = null
        )
    }

    /**
     * Add video to Favorites collection in Firestore
     * If video doesn't exist in regular videos collection, it will be saved to favorites only
     * When promoting to kids list, it will be saved to regular videos collection
     */
    private suspend fun addVideoToFavoritesCollection(video: why.xee.kidsview.data.model.VideoItem): Result<Unit> {
        return try {
            // Convert VideoItem to FirestoreVideo (no category - favorites is separate)
            val firestoreVideo = FirestoreVideo.fromVideoItem(
                videoItem = video,
                duration = "", // Duration not available from search results
                quality = "auto",
                categoryId = null // No category - favorites is separate collection
            )
            
            // Add to favorites collection (separate from regular videos)
            val result = favoritesRepository.addToFavorites(firestoreVideo)
            result.onSuccess {
                AppLogger.d("FavoritesViewModel: Added video ${video.id} to Favorites collection")
            }.onFailure { exception ->
                AppLogger.e("FavoritesViewModel: Failed to add video ${video.id} to Favorites collection", exception)
            }
            // Return the result from the repository
            result
        } catch (e: Exception) {
            AppLogger.e("FavoritesViewModel: Failed to add video to Favorites collection", e)
            Result.failure(e)
        }
    }
}

