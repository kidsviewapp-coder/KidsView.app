package why.xee.kidsview.data.exception

/**
 * Base exception class for app-specific errors
 */
sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * Network-related exceptions
     */
    sealed class NetworkException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class NoInternetConnection(cause: Throwable? = null) : 
            NetworkException("No internet connection. Please check your network and try again.", cause)
        
        class TimeoutException(cause: Throwable? = null) : 
            NetworkException("Request timed out. Please try again.", cause)
        
        class UnknownNetworkError(message: String, cause: Throwable? = null) : 
            NetworkException(message, cause)
    }
    
    /**
     * YouTube API-related exceptions
     */
    sealed class YouTubeApiException(message: String, val errorCode: ErrorCode, cause: Throwable? = null) : 
        AppException(message, cause) {
        
        enum class ErrorCode {
            MISSING_API_KEY,
            INVALID_API_KEY,
            RATE_LIMIT_EXCEEDED,
            QUOTA_EXCEEDED,
            VIDEO_NOT_FOUND,
            UNKNOWN
        }
        
        class MissingApiKey(cause: Throwable? = null) : 
            YouTubeApiException(
                "YouTube API key is missing. Please contact support.",
                ErrorCode.MISSING_API_KEY,
                cause
            )
        
        class InvalidApiKey(cause: Throwable? = null) : 
            YouTubeApiException(
                "YouTube API key is invalid. Please contact support.",
                ErrorCode.INVALID_API_KEY,
                cause
            )
        
        class RateLimitExceeded(cause: Throwable? = null) : 
            YouTubeApiException(
                "Too many requests. Please try again later.",
                ErrorCode.RATE_LIMIT_EXCEEDED,
                cause
            )
        
        class QuotaExceeded(cause: Throwable? = null) : 
            YouTubeApiException(
                "API quota exceeded. Please try again later.",
                ErrorCode.QUOTA_EXCEEDED,
                cause
            )
        
        class VideoNotFound(videoId: String, cause: Throwable? = null) : 
            YouTubeApiException(
                "Video not found: $videoId",
                ErrorCode.VIDEO_NOT_FOUND,
                cause
            )
        
        class Unknown(message: String, cause: Throwable? = null) : 
            YouTubeApiException(
                message,
                ErrorCode.UNKNOWN,
                cause
            )
    }
    
    /**
     * Authentication-related exceptions
     */
    sealed class AuthException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class InvalidPin(message: String = "Invalid PIN. Please try again.") : AuthException(message)
        class InvalidPassword(message: String = "Invalid password. Please try again.") : AuthException(message)
        class PinNotSet : AuthException("PIN is not set. Please set up a PIN first.")
        class PasswordNotSet : AuthException("Password is not set. Please set up a password first.")
        class WeakPin(message: String = "PIN is too weak. Please choose a stronger PIN.") : AuthException(message)
        class WeakPassword(message: String = "Password is too weak. Please choose a stronger password.") : AuthException(message)
    }
    
    /**
     * Data-related exceptions
     */
    sealed class DataException(message: String, cause: Throwable? = null) : AppException(message, cause) {
        class CacheError(message: String, cause: Throwable? = null) : DataException(message, cause)
        class StorageError(message: String, cause: Throwable? = null) : DataException(message, cause)
    }
}

