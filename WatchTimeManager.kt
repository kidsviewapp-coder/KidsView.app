package why.xee.kidsview.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Calendar

/**
 * WatchTimeManager - Manages daily watch time, wallet system, and ad-based time rewards
 * 
 * Features:
 * - Default base watch time: 60 minutes (1 hour) per day
 * - Wallet system: Earn 30 minutes per rewarded ad
 * - Maximum daily watch time: 180 minutes (3 hours)
 * - Automatic midnight reset (12:00 AM)
 * - Reset to max requires 3 consecutive rewarded ads
 * - Timer only counts during video playback
 */
class WatchTimeManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "watchtime_prefs"
        
        // SharedPreferences keys
        private const val KEY_BASE_TIME = "base_time_minutes"           // Default: 60 minutes
        private const val KEY_APPLIED_EXTRA_TIME = "applied_extra_time" // Applied from wallet
        private const val KEY_WALLET_TIME = "wallet_time_minutes"      // Earned time in wallet
        private const val KEY_TIME_USED_TODAY = "time_used_today_ms"   // Milliseconds used today
        private const val KEY_LAST_RESET_DATE = "last_reset_date"      // Timestamp of last reset
        private const val KEY_RESET_AD_COUNT = "reset_ad_count"        // Current reset ad count (0-3)
        private const val KEY_RESET_AD_START_TIME = "reset_ad_start_time" // When reset sequence started
        
        // Constants
        private const val DEFAULT_BASE_TIME = 60        // 1 hour default
        private const val MAX_DAILY_TIME = 180           // 3 hours maximum
        private const val WALLET_EARN_PER_AD = 30       // 30 minutes per ad
        private const val RESET_ADS_REQUIRED = 3        // 3 consecutive ads for reset
        private const val RESET_AD_TIMEOUT_MS = 300000L // 5 minutes timeout between ads
        
        @Volatile
        private var INSTANCE: WatchTimeManager? = null
        
        fun getInstance(context: Context): WatchTimeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchTimeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Initialize: Check for midnight reset on app start
     */
    init {
        checkMidnightReset()
    }
    
    // ==================== Core Time Management ====================
    
    /**
     * Get base watch time (default: 60 minutes / 1 hour)
     */
    fun getBaseTime(): Int {
        return prefs.getInt(KEY_BASE_TIME, DEFAULT_BASE_TIME).coerceIn(1, MAX_DAILY_TIME)
    }
    
    /**
     * Set base watch time (1-180 minutes)
     */
    fun setBaseTime(minutes: Int) {
        val clamped = minutes.coerceIn(1, MAX_DAILY_TIME)
        prefs.edit().putInt(KEY_BASE_TIME, clamped).apply()
        Log.d("WatchTimeManager", "Base time set to $clamped minutes")
    }
    
    /**
     * Get applied extra time from wallet (increases daily limit)
     */
    fun getAppliedExtraTime(): Int {
        checkMidnightReset() // Check reset before returning
        return prefs.getInt(KEY_APPLIED_EXTRA_TIME, 0)
    }
    
    /**
     * Get wallet time (earned but not yet applied)
     */
    fun getWalletTime(): Int {
        checkMidnightReset() // Check reset before returning
        return prefs.getInt(KEY_WALLET_TIME, 0)
    }
    
    /**
     * Get effective daily watch time limit (base + applied)
     * Maximum: 180 minutes (3 hours)
     */
    fun getEffectiveTimeLimit(): Int {
        val base = getBaseTime()
        val applied = getAppliedExtraTime()
        val total = base + applied
        return total.coerceAtMost(MAX_DAILY_TIME)
    }
    
    /**
     * Get time used today in milliseconds
     */
    fun getTimeUsedToday(): Long {
        checkMidnightReset() // Check reset before returning
        return prefs.getLong(KEY_TIME_USED_TODAY, 0)
    }
    
    /**
     * Get time used today in minutes
     */
    fun getTimeUsedTodayMinutes(): Int {
        return (getTimeUsedToday() / (60 * 1000)).toInt()
    }
    
    /**
     * Get remaining watch time in minutes
     */
    fun getRemainingTime(): Int {
        val effectiveLimit = getEffectiveTimeLimit()
        val usedMinutes = getTimeUsedTodayMinutes()
        return (effectiveLimit - usedMinutes).coerceAtLeast(0)
    }
    
    // ==================== Wallet System ====================
    
    /**
     * Watch rewarded ad to earn 30 minutes to wallet
     * @return true if successfully added, false if wallet would exceed limit
     */
    fun watchAdForWallet(): Boolean {
        checkMidnightReset()
        
        val currentWallet = getWalletTime()
        val currentApplied = getAppliedExtraTime()
        val base = getBaseTime()
        
        // Check if adding 30 minutes would exceed maximum possible wallet
        // Maximum wallet = MAX_DAILY_TIME - base - currentApplied
        val maxPossibleWallet = MAX_DAILY_TIME - base - currentApplied
        val newWallet = currentWallet + WALLET_EARN_PER_AD
        
        if (newWallet > maxPossibleWallet) {
            Log.w("WatchTimeManager", "Cannot add to wallet: Would exceed maximum (current: $currentWallet, max: $maxPossibleWallet)")
            return false
        }
        
        prefs.edit().putInt(KEY_WALLET_TIME, newWallet).apply()
        Log.d("WatchTimeManager", "Added ${WALLET_EARN_PER_AD} minutes to wallet. New wallet: $newWallet minutes")
        return true
    }
    
    /**
     * Apply time from wallet to increase daily watch time
     * @param minutes Number of minutes to apply (must be available in wallet)
     * @return true if successfully applied, false if insufficient wallet or would exceed max
     */
    fun applyWalletTime(minutes: Int): Boolean {
        checkMidnightReset()
        
        if (minutes <= 0) {
            Log.w("WatchTimeManager", "Cannot apply $minutes minutes (must be > 0)")
            return false
        }
        
        val currentWallet = getWalletTime()
        if (minutes > currentWallet) {
            Log.w("WatchTimeManager", "Insufficient wallet: Need $minutes, have $currentWallet")
            return false
        }
        
        val currentApplied = getAppliedExtraTime()
        val base = getBaseTime()
        val newApplied = currentApplied + minutes
        
        // Check if new applied time would exceed maximum
        val newEffective = base + newApplied
        if (newEffective > MAX_DAILY_TIME) {
            // Apply only what fits within the limit
            val maxCanApply = MAX_DAILY_TIME - base - currentApplied
            if (maxCanApply <= 0) {
                Log.w("WatchTimeManager", "Cannot apply: Already at maximum daily time")
                return false
            }
            
            // Apply partial amount
            val actualApplied = maxCanApply.coerceAtMost(minutes)
            val newWallet = currentWallet - actualApplied
            prefs.edit()
                .putInt(KEY_WALLET_TIME, newWallet)
                .putInt(KEY_APPLIED_EXTRA_TIME, currentApplied + actualApplied)
                .apply()
            
            Log.d("WatchTimeManager", "Applied $actualApplied minutes (requested $minutes, max available: $maxCanApply)")
            return true
        }
        
        // Apply full amount
        val newWallet = currentWallet - minutes
        prefs.edit()
            .putInt(KEY_WALLET_TIME, newWallet)
            .putInt(KEY_APPLIED_EXTRA_TIME, newApplied)
            .apply()
        
        Log.d("WatchTimeManager", "Applied $minutes minutes from wallet. New applied: $newApplied, New wallet: $newWallet")
        return true
    }
    
    // ==================== Reducing Watch Time ====================
    
    /**
     * Reduce daily watch time (reduces applied time, returns to wallet)
     * @param newEffectiveTime New effective time limit (base + applied)
     * @return true if successfully reduced, false if invalid
     */
    fun reduceWatchTime(newEffectiveTime: Int): Boolean {
        checkMidnightReset()
        
        val base = getBaseTime()
        val currentEffective = getEffectiveTimeLimit()
        
        // Validate new effective time
        if (newEffectiveTime < base || newEffectiveTime > MAX_DAILY_TIME) {
            Log.w("WatchTimeManager", "Invalid new effective time: $newEffectiveTime (base: $base, max: $MAX_DAILY_TIME)")
            return false
        }
        
        if (newEffectiveTime >= currentEffective) {
            Log.w("WatchTimeManager", "New effective time ($newEffectiveTime) must be less than current ($currentEffective)")
            return false
        }
        
        val currentApplied = getAppliedExtraTime()
        val newApplied = newEffectiveTime - base
        val timeToReturn = currentApplied - newApplied
        
        if (timeToReturn <= 0) {
            Log.w("WatchTimeManager", "No time to return to wallet")
            return false
        }
        
        val currentWallet = getWalletTime()
        val newWallet = currentWallet + timeToReturn
        
        prefs.edit()
            .putInt(KEY_APPLIED_EXTRA_TIME, newApplied)
            .putInt(KEY_WALLET_TIME, newWallet)
            .apply()
        
        Log.d("WatchTimeManager", "Reduced watch time: Effective $currentEffective -> $newEffectiveTime, Returned $timeToReturn to wallet")
        return true
    }
    
    // ==================== Reset Watch Time (3 Consecutive Ads) ====================
    
    /**
     * Get current reset ad count (0-3)
     */
    fun getResetAdCount(): Int {
        checkMidnightReset()
        return prefs.getInt(KEY_RESET_AD_COUNT, 0)
    }
    
    /**
     * Get reset progress message (e.g., "Ads watched: 1/3")
     */
    fun getResetProgressMessage(): String {
        val count = getResetAdCount()
        return "Ads watched: $count/$RESET_ADS_REQUIRED"
    }
    
    /**
     * Watch an ad for reset (must watch 3 consecutive ads)
     * @return ResetResult with success status and progress
     */
    fun watchAdForReset(): ResetResult {
        checkMidnightReset()
        
        val currentCount = getResetAdCount()
        val startTime = prefs.getLong(KEY_RESET_AD_START_TIME, 0)
        val now = System.currentTimeMillis()
        
        // Check if reset sequence has timed out (more than 5 minutes between ads)
        if (currentCount > 0 && startTime > 0) {
            val timeSinceLastAd = now - startTime
            if (timeSinceLastAd > RESET_AD_TIMEOUT_MS) {
                // Timeout: Reset the count
                prefs.edit().putInt(KEY_RESET_AD_COUNT, 0).apply()
                Log.w("WatchTimeManager", "Reset ad sequence timed out. Restarting from 0.")
                return ResetResult(
                    success = false,
                    adCount = 0,
                    message = "Reset timed out. Please start again.",
                    isComplete = false
                )
            }
        }
        
        // Increment ad count
        val newCount = currentCount + 1
        
        // If this is the first ad, record start time
        if (newCount == 1) {
            prefs.edit()
                .putInt(KEY_RESET_AD_COUNT, newCount)
                .putLong(KEY_RESET_AD_START_TIME, now)
                .apply()
        } else {
            prefs.edit().putInt(KEY_RESET_AD_COUNT, newCount).apply()
        }
        
        // Check if all 3 ads are complete
        if (newCount >= RESET_ADS_REQUIRED) {
            // Reset to maximum
            val success = resetWatchTime()
            return if (success) {
                ResetResult(
                    success = true,
                    adCount = RESET_ADS_REQUIRED,
                    message = "Reset complete! Daily time set to maximum (3 hours).",
                    isComplete = true
                )
            } else {
                ResetResult(
                    success = false,
                    adCount = newCount,
                    message = "Reset failed. Please try again.",
                    isComplete = false
                )
            }
        }
        
        // Still need more ads
        return ResetResult(
            success = true,
            adCount = newCount,
            message = "Ads watched: $newCount/$RESET_ADS_REQUIRED. Watch ${RESET_ADS_REQUIRED - newCount} more ad(s).",
            isComplete = false
        )
    }
    
    /**
     * Reset daily watch time to maximum (3 hours)
     * Called after 3 consecutive ads are watched
     * @return true if successfully reset
     */
    private fun resetWatchTime(): Boolean {
        checkMidnightReset()
        
        val base = getBaseTime()
        val currentApplied = getAppliedExtraTime()
        val currentWallet = getWalletTime()
        
        // Calculate how much time we need to reach maximum
        val currentEffective = base + currentApplied
        val timeNeeded = MAX_DAILY_TIME - currentEffective
        
        if (timeNeeded <= 0) {
            Log.d("WatchTimeManager", "Already at maximum time. No reset needed.")
            // Still reset the ad count and time used
            prefs.edit()
                .putInt(KEY_RESET_AD_COUNT, 0)
                .putLong(KEY_TIME_USED_TODAY, 0)
                .putLong(KEY_RESET_AD_START_TIME, 0)
                .apply()
            return true
        }
        
        // Try to get time from wallet first
        var remainingNeeded = timeNeeded
        var newWallet = currentWallet
        var newApplied = currentApplied
        
        if (currentWallet > 0) {
            val fromWallet = currentWallet.coerceAtMost(remainingNeeded)
            newWallet = currentWallet - fromWallet
            newApplied = currentApplied + fromWallet
            remainingNeeded -= fromWallet
        }
        
        // If still need more time, it means we can't reach maximum
        // But we'll still apply what we can and reset time used
        if (remainingNeeded > 0) {
            Log.w("WatchTimeManager", "Cannot reach maximum: Need $remainingNeeded more minutes (wallet: $currentWallet, applied: $currentApplied)")
            // Still reset time used and ad count
            prefs.edit()
                .putInt(KEY_WALLET_TIME, newWallet)
                .putInt(KEY_APPLIED_EXTRA_TIME, newApplied)
                .putLong(KEY_TIME_USED_TODAY, 0)
                .putInt(KEY_RESET_AD_COUNT, 0)
                .putLong(KEY_RESET_AD_START_TIME, 0)
                .apply()
            return true // Consider it successful even if not at max
        }
        
        // Successfully reset to maximum
        prefs.edit()
            .putInt(KEY_WALLET_TIME, newWallet)
            .putInt(KEY_APPLIED_EXTRA_TIME, newApplied)
            .putLong(KEY_TIME_USED_TODAY, 0)
            .putInt(KEY_RESET_AD_COUNT, 0)
            .putLong(KEY_RESET_AD_START_TIME, 0)
            .apply()
        
        Log.d("WatchTimeManager", "Reset complete: Effective time = $MAX_DAILY_TIME minutes (Base: $base + Applied: $newApplied)")
        return true
    }
    
    /**
     * Cancel reset sequence (if user abandons reset)
     */
    fun cancelReset(): Boolean {
        val currentCount = getResetAdCount()
        if (currentCount == 0) {
            return false // Nothing to cancel
        }
        
        prefs.edit()
            .putInt(KEY_RESET_AD_COUNT, 0)
            .putLong(KEY_RESET_AD_START_TIME, 0)
            .apply()
        
        Log.d("WatchTimeManager", "Reset sequence cancelled")
        return true
    }
    
    // ==================== Timer Management ====================
    
    /**
     * Start timer (call when video starts playing)
     * Returns the start time in milliseconds
     */
    fun startTimer(): Long {
        checkMidnightReset()
        return System.currentTimeMillis()
    }
    
    /**
     * Stop timer and add elapsed time to daily usage
     * @param startTime Start time from startTimer()
     */
    fun stopTimer(startTime: Long) {
        checkMidnightReset()
        
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed <= 0) return
        
        val currentUsed = getTimeUsedToday()
        val newUsed = currentUsed + elapsed
        
        // Don't allow exceeding effective limit
        val effectiveLimitMs = getEffectiveTimeLimit() * 60 * 1000L
        val clampedUsed = newUsed.coerceAtMost(effectiveLimitMs)
        
        prefs.edit().putLong(KEY_TIME_USED_TODAY, clampedUsed).apply()
        
        Log.d("WatchTimeManager", "Added ${elapsed / 1000}s to watch time. Total used: ${clampedUsed / 1000}s / ${effectiveLimitMs / 1000}s")
    }
    
    /**
     * Add time directly (for incremental updates during playback)
     * @param milliseconds Time to add in milliseconds
     */
    fun addTimeUsed(milliseconds: Long) {
        checkMidnightReset()
        
        if (milliseconds <= 0) return
        
        val currentUsed = getTimeUsedToday()
        val newUsed = currentUsed + milliseconds
        
        // Don't allow exceeding effective limit
        val effectiveLimitMs = getEffectiveTimeLimit() * 60 * 1000L
        val clampedUsed = newUsed.coerceAtMost(effectiveLimitMs)
        
        prefs.edit().putLong(KEY_TIME_USED_TODAY, clampedUsed).apply()
    }
    
    /**
     * Check if watch time limit is exceeded
     */
    fun isTimeLimitExceeded(): Boolean {
        val effectiveLimitMs = getEffectiveTimeLimit() * 60 * 1000L
        return getTimeUsedToday() >= effectiveLimitMs
    }
    
    /**
     * Get formatted time display: "Used today: HH:MM / HH:MM"
     */
    fun getTimeDisplayString(): String {
        val usedMs = getTimeUsedToday()
        val effectiveMs = getEffectiveTimeLimit() * 60 * 1000L
        
        val usedMinutes = (usedMs / (60 * 1000)).toInt()
        val effectiveMinutes = (effectiveMs / (60 * 1000)).toInt()
        
        val usedHours = usedMinutes / 60
        val usedMins = usedMinutes % 60
        val effectiveHours = effectiveMinutes / 60
        val effectiveMins = effectiveMinutes % 60
        
        return String.format("Used today: %02d:%02d / %02d:%02d", 
            usedHours, usedMins, effectiveHours, effectiveMins)
    }
    
    // ==================== Midnight Reset ====================
    
    /**
     * Check if it's past midnight since last reset
     */
    private fun isPastMidnight(lastResetTime: Long): Boolean {
        if (lastResetTime == 0L) return true
        
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        // Get today's midnight
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayMidnight = calendar.timeInMillis
        
        // Get last reset day's midnight
        calendar.timeInMillis = lastResetTime
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val lastResetMidnight = calendar.timeInMillis
        
        // Check if we've passed midnight since last reset
        return now >= todayMidnight && todayMidnight > lastResetMidnight
    }
    
    /**
     * Check for midnight reset and reset if needed
     * Should be called on app start and before any time operations
     */
    fun checkMidnightReset() {
        val lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, 0)
        
        if (isPastMidnight(lastResetDate)) {
            // Reset all daily values
            prefs.edit()
                .putLong(KEY_TIME_USED_TODAY, 0)
                .putInt(KEY_APPLIED_EXTRA_TIME, 0)
                .putInt(KEY_WALLET_TIME, 0)
                .putInt(KEY_BASE_TIME, DEFAULT_BASE_TIME) // Reset to default 60 minutes
                .putInt(KEY_RESET_AD_COUNT, 0)
                .putLong(KEY_RESET_AD_START_TIME, 0)
                .putLong(KEY_LAST_RESET_DATE, System.currentTimeMillis())
                .apply()
            
            Log.d("WatchTimeManager", "Midnight reset: All daily values reset to defaults")
        }
    }
    
    // ==================== Data Classes ====================
    
    /**
     * Result of watching ad for reset
     */
    data class ResetResult(
        val success: Boolean,
        val adCount: Int,
        val message: String,
        val isComplete: Boolean
    )
    
    // ==================== Utility Methods ====================
    
    /**
     * Get all current state (for debugging/logging)
     */
    fun getStateString(): String {
        return """
            WatchTimeManager State:
            - Base Time: ${getBaseTime()} minutes
            - Applied Extra Time: ${getAppliedExtraTime()} minutes
            - Wallet Time: ${getWalletTime()} minutes
            - Effective Limit: ${getEffectiveTimeLimit()} minutes
            - Time Used: ${getTimeUsedTodayMinutes()} minutes
            - Remaining: ${getRemainingTime()} minutes
            - Reset Ad Count: ${getResetAdCount()}/$RESET_ADS_REQUIRED
            - Display: ${getTimeDisplayString()}
        """.trimIndent()
    }
    
    /**
     * Force reset (for testing/debugging only)
     */
    fun forceReset() {
        prefs.edit()
            .putLong(KEY_TIME_USED_TODAY, 0)
            .putInt(KEY_APPLIED_EXTRA_TIME, 0)
            .putInt(KEY_WALLET_TIME, 0)
            .putInt(KEY_BASE_TIME, DEFAULT_BASE_TIME)
            .putInt(KEY_RESET_AD_COUNT, 0)
            .putLong(KEY_RESET_AD_START_TIME, 0)
            .putLong(KEY_LAST_RESET_DATE, System.currentTimeMillis())
            .apply()
        
        Log.d("WatchTimeManager", "Force reset completed")
    }
}
