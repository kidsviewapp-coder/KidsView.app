package why.xee.kidsview.data.cache

import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.utils.AppLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class for cached video results
 */
data class CachedVideoResult(
    val videos: List<VideoItem>,
    val timestamp: Long = System.currentTimeMillis(),
    val query: String = ""
)

/**
 * Firebase Firestore cache for video results
 * Reduces YouTube API calls by caching search results and playlist videos
 */
@Singleton
class VideoCache @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val CACHE_COLLECTION = "video_cache"
        private const val CACHE_TTL_HOURS = 12L // Cache expires after 12 hours
        private const val MILLIS_PER_HOUR = 3600000L
    }
    
    /**
     * Get cached videos for a search query
     */
    suspend fun getCachedVideos(query: String): List<VideoItem>? {
        return try {
            val cacheKey = "search_${query.hashCode()}"
            val document = firestore.collection(CACHE_COLLECTION)
                .document(cacheKey)
                .get()
                .await()
            
            if (document.exists()) {
                val timestamp = document.getLong("timestamp") ?: 0L
                val now = System.currentTimeMillis()
                
                // Check if cache is still valid (within TTL)
                if (now - timestamp < CACHE_TTL_HOURS * MILLIS_PER_HOUR) {
                    @Suppress("UNCHECKED_CAST")
                    val videosData = document.get("videos") as? List<Map<String, Any>>
                    videosData?.mapNotNull { it.toVideoItem() }
                } else {
                    // Cache expired, delete it
                    firestore.collection(CACHE_COLLECTION).document(cacheKey).delete().await()
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            AppLogger.e("Error reading cache: ${e.message}", e)
            null
        }
    }
    
    /**
     * Cache videos for a search query
     */
    suspend fun cacheVideos(query: String, videos: List<VideoItem>) {
        try {
            val cacheKey = "search_${query.hashCode()}"
            val videosData = videos.map { it.toMap() }
            
            firestore.collection(CACHE_COLLECTION)
                .document(cacheKey)
                .set(mapOf(
                    "query" to query,
                    "videos" to videosData,
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
        } catch (e: Exception) {
            AppLogger.e("Error caching videos: ${e.message}", e)
        }
    }
    
    // Playlist caching removed - app only shows individual videos for child safety
    // These methods are no longer used but kept for reference
    /*
    suspend fun getCachedPlaylist(playlistId: String): List<VideoItem>? {
        // Removed - playlists not supported
        return null
    }
    
    suspend fun cachePlaylist(playlistId: String, videos: List<VideoItem>) {
        // Removed - playlists not supported
    }
    */
    
    /**
     * Clear all cache (useful for testing or manual refresh)
     */
    suspend fun clearCache() {
        try {
            val snapshot = firestore.collection(CACHE_COLLECTION).get().await()
            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            AppLogger.e("Error clearing cache: ${e.message}", e)
        }
    }
    
    /**
     * Extension function to convert Map to VideoItem
     */
    private fun Map<String, Any>.toVideoItem(): VideoItem? {
        return try {
            VideoItem(
                id = get("id") as? String ?: return null,
                title = get("title") as? String ?: "",
                description = get("description") as? String ?: "",
                channelTitle = get("channelTitle") as? String ?: "",
                thumbnailUrl = get("thumbnailUrl") as? String ?: "",
                publishedAt = get("publishedAt") as? String ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extension function to convert VideoItem to Map
     */
    private fun VideoItem.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "channelTitle" to channelTitle,
            "thumbnailUrl" to thumbnailUrl,
            "publishedAt" to publishedAt
        )
    }
}

