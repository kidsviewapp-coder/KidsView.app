package why.xee.kidsview.data.repository

import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.utils.AuthHelper
import why.xee.kidsview.utils.AppLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing favorite videos in Firestore
 * Uses separate collection: users/{userId}/favorites/{videoId}
 * This is completely independent from the category system
 */
@Singleton
class FavoritesRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Get the user's favorites collection reference
     * @param userId The authenticated user's ID
     * @return Collection reference for user's favorites
     */
    private fun getUserFavoritesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("favorites")
    
    /**
     * Check if an exception is a permission denied error
     */
    private fun isPermissionDenied(exception: Exception): Boolean {
        return exception is FirebaseFirestoreException && 
               exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    }
    
    /**
     * Get a user-friendly error message from an exception
     */
    private fun getErrorMessage(exception: Exception): String {
        return when {
            isPermissionDenied(exception) -> 
                "Permission denied. Please make sure Firestore security rules are updated to allow access to favorites."
            exception is IllegalStateException -> 
                "You must be logged in to use favorites."
            else -> 
                "An error occurred. Please try again."
        }
    }

    /**
     * Add a video to favorites in Firestore
     * @param video The video to add to favorites
     * @return Result indicating success or failure
     */
    suspend fun addToFavorites(video: FirestoreVideo): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            val favoritesCollection = getUserFavoritesCollection(userId)
            
            favoritesCollection.document(video.videoId)
                .set(video.toMap())
                .await()
            
            AppLogger.d("FavoritesRepository: Video added to favorites for user $userId: ${video.videoId}")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            val errorMsg = getErrorMessage(e)
            AppLogger.e("FavoritesRepository: User not authenticated", e)
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            val errorMsg = getErrorMessage(e)
            AppLogger.e("FavoritesRepository: Failed to add video to favorites: $errorMsg", e)
            Result.failure(Exception(errorMsg, e))
        }
    }

    /**
     * Remove a video from favorites in Firestore
     * @param videoId The ID of the video to remove
     * @return Result indicating success or failure
     */
    suspend fun removeFromFavorites(videoId: String): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            val favoritesCollection = getUserFavoritesCollection(userId)
            
            favoritesCollection.document(videoId)
                .delete()
                .await()
            
            AppLogger.d("FavoritesRepository: Video removed from favorites for user $userId: $videoId")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            val errorMsg = getErrorMessage(e)
            AppLogger.e("FavoritesRepository: User not authenticated", e)
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            val errorMsg = getErrorMessage(e)
            AppLogger.e("FavoritesRepository: Failed to remove video from favorites: $errorMsg", e)
            Result.failure(Exception(errorMsg, e))
        }
    }

    /**
     * Check if a video is in favorites
     * @param videoId The ID of the video to check
     * @return Result containing true if video is favorited, false otherwise
     */
    suspend fun isInFavorites(videoId: String): Result<Boolean> {
        return try {
            val userId = AuthHelper.requireUserId()
            val favoritesCollection = getUserFavoritesCollection(userId)
            
            val doc = favoritesCollection.document(videoId).get().await()
            Result.success(doc.exists())
        } catch (e: IllegalStateException) {
            val errorMsg = getErrorMessage(e)
            AppLogger.e("FavoritesRepository: User not authenticated", e)
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            // For permission errors, return false instead of failing
            // This allows the app to continue working with local DB only
            if (isPermissionDenied(e)) {
                AppLogger.w("FavoritesRepository: Permission denied checking favorites, using local DB only")
                Result.success(false)
            } else {
                val errorMsg = getErrorMessage(e)
                AppLogger.e("FavoritesRepository: Failed to check if video is in favorites: $errorMsg", e)
                Result.failure(Exception(errorMsg, e))
            }
        }
    }

    /**
     * Get all favorite videos from Firestore
     * @return Result containing list of favorite videos
     */
    suspend fun getAllFavorites(): Result<List<FirestoreVideo>> {
        return try {
            val userId = AuthHelper.requireUserId()
            val favoritesCollection = getUserFavoritesCollection(userId)
            
            val snapshot = favoritesCollection.get().await()
            val favorites = snapshot.documents.mapNotNull { doc ->
                FirestoreVideo.fromDocument(doc)
            }
            
            AppLogger.d("FavoritesRepository: Loaded ${favorites.size} favorite videos for user $userId")
            Result.success(favorites)
        } catch (e: IllegalStateException) {
            val errorMsg = getErrorMessage(e)
            AppLogger.e("FavoritesRepository: User not authenticated", e)
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            val errorMsg = getErrorMessage(e)
            AppLogger.e("FavoritesRepository: Failed to load favorite videos: $errorMsg", e)
            Result.failure(Exception(errorMsg, e))
        }
    }

    /**
     * Get all favorite video IDs (for quick checking)
     * @return Result containing set of favorite video IDs
     */
    suspend fun getFavoriteVideoIds(): Result<Set<String>> {
        return try {
            val userId = AuthHelper.requireUserId()
            val favoritesCollection = getUserFavoritesCollection(userId)
            
            val snapshot = favoritesCollection.get().await()
            val videoIds = snapshot.documents.mapNotNull { doc -> doc.id }.toSet()
            
            Result.success(videoIds)
        } catch (e: IllegalStateException) {
            val errorMsg = getErrorMessage(e)
            AppLogger.e("FavoritesRepository: User not authenticated", e)
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            // For permission errors, return empty set instead of failing
            // This allows the app to continue working with local DB only
            if (isPermissionDenied(e)) {
                AppLogger.w("FavoritesRepository: Permission denied loading favorite IDs, using empty set")
                Result.success(emptySet())
            } else {
                val errorMsg = getErrorMessage(e)
                AppLogger.e("FavoritesRepository: Failed to load favorite video IDs: $errorMsg", e)
                Result.failure(Exception(errorMsg, e))
            }
        }
    }
}

