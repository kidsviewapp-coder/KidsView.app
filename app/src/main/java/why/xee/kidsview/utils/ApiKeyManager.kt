package why.xee.kidsview.utils

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages YouTube API key rotation based on hour of day
 * Rotates through available API keys to distribute quota usage
 */
@Singleton
class ApiKeyManager @Inject constructor(
    private val apiKeys: List<String>
) {
    
    /**
     * Gets the current API key based on hour of day
     * Uses modulo operation to cycle through available keys
     */
    fun getCurrentApiKey(): String {
        if (apiKeys.isEmpty()) {
            return ""
        }
        
        // Get current hour (0-23)
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        // Use hour modulo number of keys to rotate
        val keyIndex = currentHour % apiKeys.size
        
        return apiKeys[keyIndex]
    }
    
    /**
     * Get all available API keys (for debugging/monitoring)
     */
    fun getAllApiKeys(): List<String> = apiKeys
    
    /**
     * Get the number of API keys configured
     */
    fun getApiKeyCount(): Int = apiKeys.size
}

