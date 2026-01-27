package why.xee.kidsview.data.repository

import why.xee.kidsview.data.model.FirestoreCategory
import why.xee.kidsview.utils.AuthHelper
import why.xee.kidsview.utils.AppLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing video categories in Firestore
 * Uses per-user collections: users/{userId}/categories/{categoryId}
 * Each user has their own private categories
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /**
     * Get the user's categories collection reference
     * @param userId The authenticated user's ID
     * @return Collection reference for user's categories
     */
    private fun getUserCategoriesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("categories")
    
    /**
     * Get the user's videos collection reference (for counting videos in categories)
     * @param userId The authenticated user's ID
     * @return Collection reference for user's videos
     */
    private fun getUserVideosCollection(userId: String) =
        firestore.collection("users").document(userId).collection("videos")

    /**
     * Create a new category in the current user's collection
     * During migration: Also writes to old collection for backward compatibility
     * @param category The category to create
     * @return Result indicating success or failure
     */
    suspend fun createCategory(category: FirestoreCategory): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userCategoriesCollection = getUserCategoriesCollection(userId)
            
            // Write to NEW collection (per-user)
            userCategoriesCollection.document(category.categoryId)
                .set(category.toMap())
                .await()
            
            // MIGRATION MODE: Also write to OLD collection
            // Remove this after migration is complete (Phase 4)
            try {
                val oldCollection = firestore.collection("categories")
                oldCollection.document(category.categoryId)
                    .set(category.toMap())
                    .await()
                AppLogger.d("CategoryRepository: Category saved to both collections (migration mode)")
            } catch (e: Exception) {
                // If old collection write fails, log but don't fail the operation
                AppLogger.w("CategoryRepository: Failed to write to old collection (migration mode)", e)
            }
            
            AppLogger.d("CategoryRepository: Category created for user $userId: ${category.categoryId}")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            AppLogger.e("CategoryRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("CategoryRepository: Failed to create category", e)
            Result.failure(e)
        }
    }

    /**
     * Get all categories for the current user, ordered by timestamp (newest first)
     * Only returns categories from the authenticated user's collection
     */
    suspend fun getAllCategories(): Result<List<FirestoreCategory>> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userCategoriesCollection = getUserCategoriesCollection(userId)
            
            val snapshot = userCategoriesCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val categories = snapshot.documents.mapNotNull { doc ->
                FirestoreCategory.fromDocument(doc)
            }
            
            AppLogger.d("CategoryRepository: Loaded ${categories.size} categories for user $userId")
            Result.success(categories)
        } catch (e: IllegalStateException) {
            AppLogger.e("CategoryRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("CategoryRepository: Failed to load categories", e)
            Result.failure(e)
        }
    }

    /**
     * Get category by ID from the current user's collection
     * @param categoryId The ID of the category to retrieve
     * @return Result containing the category if found, null otherwise
     */
    suspend fun getCategoryById(categoryId: String): Result<FirestoreCategory?> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userCategoriesCollection = getUserCategoriesCollection(userId)
            
            val doc = userCategoriesCollection.document(categoryId).get().await()
            if (doc.exists()) {
                Result.success(FirestoreCategory.fromDocument(doc))
            } else {
                Result.success(null)
            }
        } catch (e: IllegalStateException) {
            AppLogger.e("CategoryRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("CategoryRepository: Failed to get category", e)
            Result.failure(e)
        }
    }

    /**
     * Update category in the current user's collection
     * @param category The category to update
     * @return Result indicating success or failure
     */
    suspend fun updateCategory(category: FirestoreCategory): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userCategoriesCollection = getUserCategoriesCollection(userId)
            
            userCategoriesCollection.document(category.categoryId)
                .update(category.toMap())
                .await()
            
            AppLogger.d("CategoryRepository: Category updated for user $userId: ${category.categoryId}")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            AppLogger.e("CategoryRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("CategoryRepository: Failed to update category", e)
            Result.failure(e)
        }
    }

    /**
     * Delete category and remove categoryId from all videos in that category
     * Only affects the current user's data
     * Favorites category cannot be deleted
     * @param categoryId The ID of the category to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            // Prevent deletion of Favorites category
            if (categoryId == "favorites") {
                AppLogger.w("CategoryRepository: Attempted to delete Favorites category - operation blocked")
                return Result.failure(IllegalArgumentException("Favorites category cannot be deleted"))
            }
            
            val userId = AuthHelper.requireUserId()
            val userCategoriesCollection = getUserCategoriesCollection(userId)
            val userVideosCollection = getUserVideosCollection(userId)
            
            // Remove categoryId from all videos in this category (user's videos only)
            val videos = userVideosCollection
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()

            videos.documents.forEach { doc ->
                doc.reference.update("categoryId", com.google.firebase.firestore.FieldValue.delete()).await()
            }

            // Delete the category
            userCategoriesCollection.document(categoryId).delete().await()
            
            AppLogger.d("CategoryRepository: Category deleted for user $userId: $categoryId (removed from ${videos.size()} videos)")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            AppLogger.e("CategoryRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("CategoryRepository: Failed to delete category", e)
            Result.failure(e)
        }
    }

    /**
     * Update video count for a category in the current user's collection
     * Counts only videos from the current user's collection
     * @param categoryId The ID of the category to update
     * @return Result indicating success or failure
     */
    suspend fun updateVideoCount(categoryId: String): Result<Unit> {
        return try {
            val userId = AuthHelper.requireUserId()
            val userCategoriesCollection = getUserCategoriesCollection(userId)
            val userVideosCollection = getUserVideosCollection(userId)
            
            val count = userVideosCollection
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()
                .size()

            userCategoriesCollection.document(categoryId)
                .update("videoCount", count)
                .await()

            AppLogger.d("CategoryRepository: Updated video count for category $categoryId: $count (user $userId)")
            Result.success(Unit)
        } catch (e: IllegalStateException) {
            AppLogger.e("CategoryRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("CategoryRepository: Failed to update video count", e)
            Result.failure(e)
        }
    }

    /**
     * Ensure the Favorites category exists for the current user
     * Creates it if it doesn't exist
     * @return Result containing the category ID if successful
     */
    suspend fun ensureFavoritesCategoryExists(): Result<String> {
        return try {
            val userId = AuthHelper.requireUserId()
            val favoritesCategoryId = "favorites"
            val userCategoriesCollection = getUserCategoriesCollection(userId)
            
            // Check if category already exists
            val doc = userCategoriesCollection.document(favoritesCategoryId).get().await()
            
            if (!doc.exists()) {
                // Create the Favorites category
                val favoritesCategory = FirestoreCategory(
                    categoryId = favoritesCategoryId,
                    name = "Favorites",
                    description = "Your favorite videos",
                    icon = "❤️",
                    color = "#E74C3C", // Red color for favorites
                    timestamp = System.currentTimeMillis(),
                    videoCount = 0
                )
                
                userCategoriesCollection.document(favoritesCategoryId)
                    .set(favoritesCategory.toMap())
                    .await()
                
                AppLogger.d("CategoryRepository: Created Favorites category for user $userId")
            } else {
                AppLogger.d("CategoryRepository: Favorites category already exists for user $userId")
            }
            
            Result.success(favoritesCategoryId)
        } catch (e: IllegalStateException) {
            AppLogger.e("CategoryRepository: User not authenticated", e)
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("CategoryRepository: Failed to ensure Favorites category exists", e)
            Result.failure(e)
        }
    }
}

