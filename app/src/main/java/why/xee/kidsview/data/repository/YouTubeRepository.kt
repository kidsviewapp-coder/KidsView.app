package why.xee.kidsview.data.repository

import why.xee.kidsview.data.api.YouTubeApiService
import why.xee.kidsview.data.cache.VideoCache
import why.xee.kidsview.data.exception.AppException
import why.xee.kidsview.data.model.CartoonDatabase
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.data.model.YouTubeItem
import why.xee.kidsview.data.model.YouTubeVideoDetailsResponse
import why.xee.kidsview.data.model.YouTubeVideoDetail
import why.xee.kidsview.utils.AppLogger
import why.xee.kidsview.utils.SearchQueryNormalizer
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for YouTube data operations with Firebase caching
 * Uses cache-first strategy to reduce API quota usage
 */
@Singleton
class YouTubeRepository @Inject constructor(
    private val apiService: YouTubeApiService,
    private val apiKeyManager: why.xee.kidsview.utils.ApiKeyManager,
    private val videoCache: VideoCache
) {
    
    /**
     * Search for videos using a query string
     * Cache-first strategy: Checks Firebase cache before making API call
     * Uses normalized query for better search results (more forgiving, like real YouTube)
     */
    suspend fun searchVideos(query: String): Result<List<VideoItem>> {
        // Normalize query for better search results (handles whitespace, etc.)
        val normalizedQuery = SearchQueryNormalizer.normalizeQuery(query)
        
        // Check cache with original query first
        val cachedVideos = videoCache.getCachedVideos(normalizedQuery)
        if (cachedVideos != null && cachedVideos.isNotEmpty()) {
            return Result.success(cachedVideos)
        }
        
        val apiKey = apiKeyManager.getCurrentApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(
                AppException.YouTubeApiException.MissingApiKey()
            )
        }
        
        return try {
            // Use normalized query for API call
            // YouTube API's relevance algorithm handles:
            // - Spelling variations and typos
            // - Synonyms and related terms
            // - Context understanding
            // - Fuzzy matching
            val response = apiService.searchVideos(
                query = normalizedQuery,
                maxResults = 25, // Increased from 20 for better results
                apiKey = apiKey
            )
            
            // Check for API errors in response
            if (response.error != null) {
                val errorMessage = response.error.message ?: "Unknown API error"
                val errorCode = response.error.code
                
                val exception = when (errorCode) {
                    403 -> AppException.YouTubeApiException.InvalidApiKey()
                    429 -> AppException.YouTubeApiException.RateLimitExceeded()
                    400 -> AppException.YouTubeApiException.Unknown("Invalid request: $errorMessage")
                    else -> AppException.YouTubeApiException.Unknown("YouTube API Error: $errorMessage")
                }
                return Result.failure(exception)
            }
            
            val videos = response.items?.mapNotNull { it.toVideoItem() } ?: emptyList()
            
            if (videos.isNotEmpty()) {
                // Cache with normalized query
                videoCache.cacheVideos(normalizedQuery, videos)
            }
            
            Result.success(videos)
        } catch (e: UnknownHostException) {
            AppLogger.e("Network error: No internet connection", e)
            Result.failure(AppException.NetworkException.NoInternetConnection(e))
        } catch (e: SocketTimeoutException) {
            AppLogger.e("Network error: Request timeout", e)
            Result.failure(AppException.NetworkException.TimeoutException(e))
        } catch (e: HttpException) {
            AppLogger.e("HTTP error: ${e.code()}", e)
            val exception = when (e.code()) {
                403 -> AppException.YouTubeApiException.InvalidApiKey(e)
                429 -> AppException.YouTubeApiException.RateLimitExceeded(e)
                400 -> AppException.YouTubeApiException.Unknown("Invalid request: ${e.message()}", e)
                else -> AppException.YouTubeApiException.Unknown("API error: ${e.message()}", e)
            }
            Result.failure(exception)
        } catch (e: AppException) {
            // Re-throw app-specific exceptions
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("Search error: ${e.message}", e)
            Result.failure(AppException.NetworkException.UnknownNetworkError("An error occurred. Please try again.", e))
        }
    }
    
    /**
     * Search for cartoon videos by country and genre
     * Looks up cartoons from the database, builds a search query, and fetches videos from YouTube
     * 
     * @param country Country name (e.g., "Pakistan", "India", "International")
     * @param genre Genre name (e.g., "Action / Adventure", "Comedy / Fun")
     * @return Result containing list of VideoItem objects, or empty list on error
     */
    suspend fun searchCartoonsByCountryAndGenre(
        country: String,
        genre: String
    ): Result<List<VideoItem>> {
        // Get cartoons from database
        val cartoons = CartoonDatabase.getCartoons(country, genre)
        
        if (cartoons.isEmpty()) {
            AppLogger.w("No cartoons found for country: $country, genre: $genre")
            return Result.success(emptyList())
        }
        
        val searchQuery = "${cartoons.joinToString(" | ")} cartoon"
        
        val apiKey = apiKeyManager.getCurrentApiKey()
        if (apiKey.isBlank()) {
            AppLogger.w("No cartoons found for country: $country, genre: $genre - API key missing")
            return Result.failure(AppException.YouTubeApiException.MissingApiKey())
        }
        
        return try {
            // Call YouTube API with specific parameters for cartoon search
            val response = apiService.searchVideos(
                query = searchQuery,
                maxResults = 30,
                safeSearch = "strict",
                order = "relevance",
                apiKey = apiKey
            )
            
            // Check for API errors
            if (response.error != null) {
                val errorMessage = response.error.message ?: "Unknown API error"
                AppLogger.e("YouTube API Error: $errorMessage")
                val exception = when (response.error.code) {
                    403 -> AppException.YouTubeApiException.InvalidApiKey()
                    429 -> AppException.YouTubeApiException.RateLimitExceeded()
                    else -> AppException.YouTubeApiException.Unknown("YouTube API Error: $errorMessage")
                }
                return Result.failure(exception)
            }
            
            val videos = response.items?.mapNotNull { it.toVideoItem() } ?: emptyList()
            
            if (videos.isNotEmpty()) {
                videoCache.cacheVideos(searchQuery, videos)
            }
            
            Result.success(videos)
        } catch (e: UnknownHostException) {
            AppLogger.e("Network error searching cartoons: No internet", e)
            Result.success(emptyList()) // Return empty list on network error as per requirements
        } catch (e: SocketTimeoutException) {
            AppLogger.e("Network error searching cartoons: Timeout", e)
            Result.success(emptyList()) // Return empty list on network error as per requirements
        } catch (e: HttpException) {
            AppLogger.e("HTTP error searching cartoons: ${e.code()}", e)
            Result.success(emptyList()) // Return empty list on network error as per requirements
        } catch (e: Exception) {
            AppLogger.e("Error searching cartoons: ${e.message}", e)
            Result.success(emptyList()) // Return empty list on network error as per requirements
        }
    }
    
    // Playlist functionality removed - app only shows individual videos for child safety
    // All video fetching now uses search API with type=video only
    
    /**
     * Get video details including duration by video ID
     */
    suspend fun getVideoDetails(videoId: String): Result<VideoItem> {
        // Get current API key (rotated based on hour)
        val apiKey = apiKeyManager.getCurrentApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(AppException.YouTubeApiException.MissingApiKey())
        }
        
        return try {
            val response = apiService.getVideoDetails(
                videoIds = videoId,
                apiKey = apiKey
            )
            
            // Check for API errors in response
            if (response.error != null) {
                val errorMessage = response.error.message ?: "Unknown API error"
                val exception = when (response.error.code) {
                    404 -> AppException.YouTubeApiException.VideoNotFound(videoId)
                    403 -> AppException.YouTubeApiException.InvalidApiKey()
                    429 -> AppException.YouTubeApiException.RateLimitExceeded()
                    else -> AppException.YouTubeApiException.Unknown("YouTube API Error: $errorMessage")
                }
                return Result.failure(exception)
            }
            
            val videoDetail = response.items?.firstOrNull()
                ?: return Result.failure(AppException.YouTubeApiException.VideoNotFound(videoId))
            
            val duration = videoDetail.contentDetails?.duration?.let { 
                why.xee.kidsview.utils.DurationUtils.parseISO8601Duration(it)
            } ?: ""
            
            val thumbnailUrl = videoDetail.snippet.thumbnails.high?.url 
                ?: videoDetail.snippet.thumbnails.medium?.url 
                ?: videoDetail.snippet.thumbnails.default?.url 
                ?: ""
            
            val videoItem = VideoItem(
                id = videoDetail.id,
                title = videoDetail.snippet.title,
                description = videoDetail.snippet.description,
                channelTitle = videoDetail.snippet.channelTitle,
                thumbnailUrl = thumbnailUrl,
                publishedAt = videoDetail.snippet.publishedAt,
                duration = duration
            )
            
            Result.success(videoItem)
        } catch (e: UnknownHostException) {
            AppLogger.e("Network error: No internet connection", e)
            Result.failure(AppException.NetworkException.NoInternetConnection(e))
        } catch (e: SocketTimeoutException) {
            AppLogger.e("Network error: Request timeout", e)
            Result.failure(AppException.NetworkException.TimeoutException(e))
        } catch (e: HttpException) {
            AppLogger.e("HTTP error: ${e.code()}", e)
            val exception = when (e.code()) {
                404 -> AppException.YouTubeApiException.VideoNotFound(videoId, e)
                403 -> AppException.YouTubeApiException.InvalidApiKey(e)
                429 -> AppException.YouTubeApiException.RateLimitExceeded(e)
                else -> AppException.YouTubeApiException.Unknown("API error: ${e.message()}", e)
            }
            Result.failure(exception)
        } catch (e: AppException) {
            Result.failure(e)
        } catch (e: Exception) {
            AppLogger.e("Get video details error: ${e.message}", e)
            Result.failure(AppException.NetworkException.UnknownNetworkError("An error occurred. Please try again.", e))
        }
    }
    
    /**
     * Extension function to convert YouTubeItem to VideoItem
     * ONLY processes individual videos - filters out playlists for child safety
     */
    private fun YouTubeItem.toVideoItem(): VideoItem? {
        // Reject playlist items - only accept individual videos
        // Check if this is a playlist (has playlistId but no videoId)
        if (id?.playlistId != null && id?.videoId == null) {
            AppLogger.d("Filtered out playlist item: ${id?.playlistId}")
            return null
        }
        
        // Only process items with a valid videoId (individual videos)
        val videoId = id?.videoId ?: return null
        
        val thumbnailUrl = snippet.thumbnails.high?.url 
            ?: snippet.thumbnails.medium?.url 
            ?: snippet.thumbnails.default?.url 
            ?: ""
        
        return VideoItem(
            id = videoId,
            title = snippet.title,
            description = snippet.description,
            channelTitle = snippet.channelTitle,
            thumbnailUrl = thumbnailUrl,
            publishedAt = snippet.publishedAt
        )
    }
}

