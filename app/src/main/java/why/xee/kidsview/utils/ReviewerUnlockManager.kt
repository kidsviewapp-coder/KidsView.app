package why.xee.kidsview.utils

/**
 * Session-only reviewer unlock manager for Google Play reviewers
 * 
 * This provides a temporary bypass for parent PIN/password authentication
 * that is only active during the current app session. The flag is NOT persisted
 * and will reset on app restart or process death.
 * 
 * Activation: 
 * - Enter reviewer passkey "791989" in PIN mode during initial setup
 * - Enter reviewer password "791989" in password mode during initial setup
 * Effect: All parent authentication checks return true while flag is active
 */
object ReviewerUnlockManager {
    /**
     * Reviewer passkey constant (for PIN mode)
     */
    private const val REVIEWER_PASSKEY = "791989"
    
    /**
     * Reviewer password constant (for password mode) - same as passkey
     */
    private const val REVIEWER_PASSWORD = "791989"
    
    /**
     * Session-only flag indicating if reviewer unlock is active
     * This is NOT persisted and resets on app restart
     */
    @Volatile
    private var isReviewerParentVerified: Boolean = false
    
    /**
     * Check if the provided input matches the reviewer passkey (PIN)
     */
    fun isReviewerPasskey(input: String): Boolean {
        return input == REVIEWER_PASSKEY
    }
    
    /**
     * Check if the provided input matches the reviewer password
     */
    fun isReviewerPassword(input: String): Boolean {
        return input.equals(REVIEWER_PASSWORD, ignoreCase = true)
    }
    
    /**
     * Check if the provided input matches either reviewer passkey or password
     */
    fun isReviewerCode(input: String): Boolean {
        return isReviewerPasskey(input) || isReviewerPassword(input)
    }
    
    /**
     * Check if reviewer unlock is currently active
     */
    fun isUnlocked(): Boolean = isReviewerParentVerified
    
    /**
     * Activate reviewer unlock (session-only, not persisted)
     */
    fun activate() {
        isReviewerParentVerified = true
        AppLogger.d("ReviewerUnlockManager: Reviewer PIN used – full unlock applied")
    }
    
    /**
     * Reset the flag (called on app start to ensure clean state)
     */
    fun reset() {
        isReviewerParentVerified = false
    }
}
