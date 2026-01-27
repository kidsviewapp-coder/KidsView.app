package why.xee.kidsview.utils

import com.google.firebase.auth.FirebaseAuth
import why.xee.kidsview.utils.AppLogger

/**
 * Helper class for Firebase Authentication operations
 * Ensures users are authenticated before accessing Firestore data
 */
object AuthHelper {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Get the current user's ID
     * @return User ID if authenticated, null otherwise
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Check if user is authenticated
     * @return true if authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Get current user ID or throw exception
     * Use this when you're certain the user should be authenticated
     * @throws IllegalStateException if user is not authenticated
     */
    fun requireUserId(): String {
        val userId = getCurrentUserId()
        if (userId == null) {
            AppLogger.e("AuthHelper: User not authenticated - cannot access user data")
            throw IllegalStateException("User must be authenticated to access user data")
        }
        return userId
    }
    
    /**
     * Wait for authentication to complete (for anonymous auth)
     * This is useful when the app first starts and anonymous auth is in progress
     */
    suspend fun waitForAuthentication(maxWaitMs: Long = 5000): String? {
        var waited = 0L
        val waitInterval = 100L
        
        while (!isAuthenticated() && waited < maxWaitMs) {
            kotlinx.coroutines.delay(waitInterval)
            waited += waitInterval
        }
        
        return getCurrentUserId()
    }
}

