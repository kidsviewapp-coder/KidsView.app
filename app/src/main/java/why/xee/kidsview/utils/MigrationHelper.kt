package why.xee.kidsview.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.data.model.FirestoreCategory

/**
 * Helper class for migrating data from global collections to per-user collections
 * 
 * This utility migrates existing videos and categories from:
 * - OLD: parent_selected_videos/{videoId}
 * - NEW: users/{userId}/videos/{videoId}
 * 
 * - OLD: categories/{categoryId}
 * - NEW: users/{userId}/categories/{categoryId}
 * 
 * Usage:
 * - Call migrateAllForCurrentUser() once per user after they authenticate
 * - Migration is idempotent (safe to call multiple times)
 * - Only migrates data that doesn't already exist in user's collection
 */
object MigrationHelper {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Migrate all videos from global collection to current user's collection
     * @return Result containing the number of videos migrated
     */
    suspend fun migrateVideosForCurrentUser(): Result<Int> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            AppLogger.d("MigrationHelper: Starting video migration for user $userId")
            
            // Get all videos from old global collection
            val oldCollection = firestore.collection("parent_selected_videos")
            val snapshot = oldCollection.get().await()
            
            if (snapshot.isEmpty) {
                AppLogger.d("MigrationHelper: No videos to migrate for user $userId")
                return Result.success(0)
            }
            
            val userVideosCollection = firestore
                .collection("users")
                .document(userId)
                .collection("videos")
            
            var migratedCount = 0
            var skippedCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val video = FirestoreVideo.fromDocument(doc)
                    if (video != null) {
                        // Check if video already exists in user's collection
                        val existingDoc = userVideosCollection.document(video.videoId).get().await()
                        if (!existingDoc.exists()) {
                            // Only migrate if not already in user's collection
                            userVideosCollection.document(video.videoId)
                                .set(video.toMap())
                                .await()
                            migratedCount++
                        } else {
                            skippedCount++
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("MigrationHelper: Failed to migrate video ${doc.id}", e)
                }
            }
            
            AppLogger.d("MigrationHelper: Video migration complete for user $userId - Migrated: $migratedCount, Skipped: $skippedCount")
            Result.success(migratedCount)
        } catch (e: Exception) {
            AppLogger.e("MigrationHelper: Failed to migrate videos", e)
            Result.failure(e)
        }
    }
    
    /**
     * Migrate all categories from global collection to current user's collection
     * @return Result containing the number of categories migrated
     */
    suspend fun migrateCategoriesForCurrentUser(): Result<Int> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            AppLogger.d("MigrationHelper: Starting category migration for user $userId")
            
            // Get all categories from old global collection
            val oldCollection = firestore.collection("categories")
            val snapshot = oldCollection.get().await()
            
            if (snapshot.isEmpty) {
                AppLogger.d("MigrationHelper: No categories to migrate for user $userId")
                return Result.success(0)
            }
            
            val userCategoriesCollection = firestore
                .collection("users")
                .document(userId)
                .collection("categories")
            
            var migratedCount = 0
            var skippedCount = 0
            
            snapshot.documents.forEach { doc ->
                try {
                    val category = FirestoreCategory.fromDocument(doc)
                    if (category != null) {
                        // Check if category already exists in user's collection
                        val existingDoc = userCategoriesCollection.document(category.categoryId).get().await()
                        if (!existingDoc.exists()) {
                            // Only migrate if not already in user's collection
                            userCategoriesCollection.document(category.categoryId)
                                .set(category.toMap())
                                .await()
                            migratedCount++
                        } else {
                            skippedCount++
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("MigrationHelper: Failed to migrate category ${doc.id}", e)
                }
            }
            
            AppLogger.d("MigrationHelper: Category migration complete for user $userId - Migrated: $migratedCount, Skipped: $skippedCount")
            Result.success(migratedCount)
        } catch (e: Exception) {
            AppLogger.e("MigrationHelper: Failed to migrate categories", e)
            Result.failure(e)
        }
    }
    
    /**
     * Migrate all data for current user (videos + categories)
     * @return Result containing pair of (videosMigrated, categoriesMigrated)
     */
    suspend fun migrateAllForCurrentUser(): Result<Pair<Int, Int>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            AppLogger.d("MigrationHelper: Starting full migration for user $userId")
            
            val videosResult = migrateVideosForCurrentUser()
            val categoriesResult = migrateCategoriesForCurrentUser()
            
            if (videosResult.isSuccess && categoriesResult.isSuccess) {
                val videoCount = videosResult.getOrNull() ?: 0
                val categoryCount = categoriesResult.getOrNull() ?: 0
                
                // Mark migration as complete for this user
                markMigrationComplete(userId)
                
                AppLogger.d("MigrationHelper: Full migration complete for user $userId - Videos: $videoCount, Categories: $categoryCount")
                Result.success(Pair(videoCount, categoryCount))
            } else {
                val error = videosResult.exceptionOrNull() ?: categoriesResult.exceptionOrNull()
                Result.failure(error ?: Exception("Migration failed"))
            }
        } catch (e: Exception) {
            AppLogger.e("MigrationHelper: Failed to migrate all data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if migration is complete for current user
     * @return true if migration is complete, false otherwise
     */
    suspend fun isMigrationComplete(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            val userDoc = firestore
                .collection("users")
                .document(userId)
                .get()
                .await()
            
            userDoc.getBoolean("migrationComplete") ?: false
        } catch (e: Exception) {
            AppLogger.e("MigrationHelper: Failed to check migration status", e)
            false
        }
    }
    
    /**
     * Mark migration as complete for a user
     * @param userId The user ID to mark as migrated
     */
    private suspend fun markMigrationComplete(userId: String) {
        try {
            firestore
                .collection("users")
                .document(userId)
                .set(mapOf("migrationComplete" to true), com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            AppLogger.d("MigrationHelper: Marked migration complete for user $userId")
        } catch (e: Exception) {
            AppLogger.e("MigrationHelper: Failed to mark migration complete", e)
        }
    }
}

