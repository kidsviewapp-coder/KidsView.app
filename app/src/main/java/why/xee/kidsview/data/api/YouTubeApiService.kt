package why.xee.kidsview.data.api

import why.xee.kidsview.data.model.YouTubeResponse
import why.xee.kidsview.data.model.YouTubeVideoDetailsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * YouTube Data API v3 service interface
 */
interface YouTubeApiService {
    
    /**
     * Search for videos
     * @param query Search query
     * @param maxResults Maximum number of results (default: 20)
     * @param apiKey YouTube API key
     * @param safeSearch Safe search mode (strict for child safety)
     */
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 25, // Increased for better results
        @Query("safeSearch") safeSearch: String = "strict", // Child safety enabled
        @Query("order") order: String = "relevance", // Relevance ranking - YouTube's algorithm handles spelling variations, synonyms, and context
        @Query("key") apiKey: String
    ): YouTubeResponse
    
    // Playlist functionality removed - app only shows individual videos for child safety
    // All video fetching now uses search API with type=video only
    
    /**
     * Get video details by video IDs
     * @param videoIds Comma-separated list of video IDs
     * @param apiKey YouTube API key
     */
    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("id") videoIds: String,
        @Query("key") apiKey: String
    ): YouTubeVideoDetailsResponse
}

