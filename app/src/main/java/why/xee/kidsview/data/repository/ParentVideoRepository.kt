package why.xee.kidsview.data.repository

import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.utils.AuthHelper
import why.xee.kidsview.utils.AppLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing parent-selected videos in Firestore
 * Uses per-user collections: users/{userId}/videos/{videoId}
 * Each user has their own private video collection
 */
@Singleton
class ParentVideoRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Get the user's video collection reference
     * @param userId The authenticated user's ID
     * @return Collection reference for user's videos
     */
    private fun getUserVideosCollection(userId: String) =
        firestore.collection("users").document(userId).collection("videos")

    /**
     * Save a video to Firestore (user's private collection)
     * Uses only the new per-user structure: users/{userId}/videos/{videoId}
     * @param video The video to save
     * @return Result indicating success or failure
     */
    suspend fun saveVideo(video: FirestoreVideo): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userVideosCollection = getUserVideosCollection(userId)
            
            userVideosCollection.document(video.videoId)
                .set(video.toMap())
                .await()
            
            AppLogger.d("ParentVideoRepository: Video saved for user $userId: ${video.videoId}")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            AppLogger.e("ParentVideoRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("ParentVideoRepository: Failed to save video", e)
            Result.failure(e)
        }
    }

    /**
     * Get all videos for the current user, ordered by timestamp (newest first)
     * Only reads from the new per-user collection: users/{userId}/videos
     */
    suspend fun getAllVideos(): Result<List<FirestoreVideo>> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userVideosCollection = getUserVideosCollection(userId)
            
            // Read from NEW collection (per-user)
            val snapshot = userVideosCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val videos = snapshot.documents.mapNotNull { doc ->
                FirestoreVideo.fromDocument(doc)
            }
            
            AppLogger.d("ParentVideoRepository: Loaded ${videos.size} videos for user $userId")
            Result.success(videos)
        } catch (e: IllegalStateException) {
            AppLogger.e("ParentVideoRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("ParentVideoRepository: Failed to load videos", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a video by videoId from the current user's collection
     * Uses only the new per-user structure: users/{userId}/videos/{videoId}
     * @param videoId The ID of the video to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteVideo(videoId: String): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userVideosCollection = getUserVideosCollection(userId)
            
            userVideosCollection.document(videoId)
                .delete()
                .await()
            
            AppLogger.d("ParentVideoRepository: Video deleted for user $userId: $videoId")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            AppLogger.e("ParentVideoRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("ParentVideoRepository: Failed to delete video", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a video is already saved in the current user's collection
     * @param videoId The ID of the video to check
     * @return true if the video exists in the user's collection, false otherwise
     */
    suspend fun isVideoSaved(videoId: String): Boolean {
        return try {
            val userId = AuthHelper.requireUserId()
            val userVideosCollection = getUserVideosCollection(userId)
            
            val doc = userVideosCollection.document(videoId).get().await()
            doc.exists()
        } catch (e: IllegalStateException) {
            AppLogger.e("ParentVideoRepository: User not authenticated", e)
            false
        } catch (e: Exception) {
            AppLogger.e("ParentVideoRepository: Failed to check if video is saved", e)
            false
        }
    }
    
    /**
     * Get videos by category ID from the current user's collection
     * Note: Sorting is done in memory to avoid needing a composite index
     * @param categoryId The category ID to filter by
     * @return Result containing list of videos in the category
     */
    suspend fun getVideosByCategory(categoryId: String): Result<List<FirestoreVideo>> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userVideosCollection = getUserVideosCollection(userId)
            
            val snapshot = userVideosCollection
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()
            
            val videos = snapshot.documents.mapNotNull { doc ->
                FirestoreVideo.fromDocument(doc)
            }.sortedByDescending { it.timestamp } // Sort in memory instead
            
            AppLogger.d("ParentVideoRepository: Loaded ${videos.size} videos in category $categoryId for user $userId")
            Result.success(videos)
        } catch (e: IllegalStateException) {
            AppLogger.e("ParentVideoRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("ParentVideoRepository: Failed to load videos by category", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update video's category in the current user's collection
     * @param videoId The ID of the video to update
     * @param categoryId The new category ID, or null to remove the category
     * @return Result indicating success or failure
     */
    suspend fun updateVideoCategory(videoId: String, categoryId: String?): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userVideosCollection = getUserVideosCollection(userId)
            
            if (categoryId != null) {
                // Set categoryId
                userVideosCollection.document(videoId)
                    .update("categoryId", categoryId)
                    .await()
            } else {
                // Remove categoryId field
                userVideosCollection.document(videoId)
                    .update("categoryId", com.google.firebase.firestore.FieldValue.delete())
                    .await()
            }
            
            AppLogger.d("ParentVideoRepository: Updated video category for user $userId: $videoId -> $categoryId")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            AppLogger.e("ParentVideoRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("ParentVideoRepository: Failed to update video category", e)
            Result.failure(e)
        }
    }
}

