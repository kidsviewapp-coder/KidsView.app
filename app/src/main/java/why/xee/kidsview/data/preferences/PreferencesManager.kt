package why.xee.kidsview.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import why.xee.kidsview.utils.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * Authentication mode for parent access
 */
enum class AuthMode {
    PIN,    // 6-digit PIN
    PASSWORD // 8+ character password
}

/**
 * Manages app preferences using SharedPreferences
 */
@Singleton
class PreferencesManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "kidsview_prefs"
        private const val KEY_THEME = "theme"
        private const val KEY_AUTH_MODE = "auth_mode"
        private const val KEY_PARENT_PIN = "parent_pin"
        private const val KEY_PARENT_PASSWORD = "parent_password"
        private const val KEY_APP_LOCKED = "app_locked"
        private const val KEY_SCREEN_LOCK_FEATURE_ENABLED = "screen_lock_feature_enabled" // Whether screen lock feature is enabled (via ad)
        private const val KEY_SCREEN_LOCK_FEATURE_ENABLE_TIME = "screen_lock_feature_enable_time" // Timestamp when screen lock feature was enabled via ad
        private const val KEY_TIME_LIMIT_ENABLED = "time_limit_enabled"
        private const val KEY_TIME_LIMIT_MINUTES = "time_limit_minutes" // Base daily limit in minutes (default 60, max 180)
        private const val KEY_TIME_USED_TODAY = "time_used_today"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_TIME_BONUS_MINUTES = "time_bonus_minutes" // Extra minutes granted via rewarded ads (DEPRECATED - use KEY_TIME_BONUS_ENTRIES)
        private const val KEY_TIME_BONUS_DATE = "time_bonus_date"       // Day index when bonus was granted (DEPRECATED)
        private const val KEY_TIME_BONUS_ENTRIES = "time_bonus_entries" // JSON array of earned time entries (wallet) with timestamps
        private const val KEY_APPLIED_EARNED_TIME = "applied_earned_time" // Earned time manually applied to today's limit
        private const val KEY_SECURITY_QUESTION_1 = "security_question_1"
        private const val KEY_SECURITY_ANSWER_1 = "security_answer_1"
        private const val KEY_SECURITY_QUESTION_2 = "security_question_2"
        private const val KEY_SECURITY_ANSWER_2 = "security_answer_2"
        private const val KEY_SECURITY_QUESTION_3 = "security_question_3"
        private const val KEY_SECURITY_ANSWER_3 = "security_answer_3"
        private const val KEY_REJECTION_READ_PREFIX = "rejection_read_" // Prefix for rejection read status
        private const val KEY_UNLOCKED_THEMES = "unlocked_themes" // Comma-separated list of unlocked theme names
        private const val KEY_INSTRUCTIONS_SHOWN_VERSION = "instructions_shown_version" // Version code when instructions were last shown
        private const val KEY_NEW_YEAR_SPLASH_DISABLED = "new_year_splash_disabled" // User disabled New Year splash
        
        // Removed DEFAULT_PIN - force users to set their own PIN
        private const val DEFAULT_THEME = "system"
        private const val FREE_THEME = "system" // System theme is always free
        private const val MIN_PIN_LENGTH = 6
        private const val MIN_PASSWORD_LENGTH = 8
        
        // Common weak PINs that should be rejected
        private val WEAK_PINS = setOf(
            "123456", "000000", "111111", "222222", "333333", "444444", "555555", "666666",
            "777777", "888888", "999999", "123123", "654321", "121212", "112233", "123321"
        )
    }

    // Theme Management
    fun getSelectedTheme(): String {
        return prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    }

    fun setSelectedTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }
    
    // Theme Unlock Management
    fun isThemeUnlocked(themeName: String): Boolean {
        // System theme is always free
        if (themeName == FREE_THEME) return true
        
        val unlockedThemes = prefs.getString(KEY_UNLOCKED_THEMES, "") ?: ""
        return unlockedThemes.split(",").contains(themeName)
    }
    
    fun unlockTheme(themeName: String) {
        // System theme is always unlocked, no need to save
        if (themeName == FREE_THEME) return
        
        val unlockedThemes = prefs.getString(KEY_UNLOCKED_THEMES, "") ?: ""
        val themesList = unlockedThemes.split(",").toMutableSet()
        themesList.add(themeName)
        prefs.edit().putString(KEY_UNLOCKED_THEMES, themesList.joinToString(",")).apply()
    }
    
    fun getUnlockedThemes(): Set<String> {
        val unlockedThemes = prefs.getString(KEY_UNLOCKED_THEMES, "") ?: ""
        val themesSet = unlockedThemes.split(",").toMutableSet()
        themesSet.add(FREE_THEME) // Always include system theme
        themesSet.remove("") // Remove empty strings
        return themesSet
    }

    // Authentication Mode Management
    fun getAuthMode(): AuthMode {
        val modeString = prefs.getString(KEY_AUTH_MODE, AuthMode.PIN.name) ?: AuthMode.PIN.name
        return try {
            AuthMode.valueOf(modeString)
        } catch (e: Exception) {
            AuthMode.PIN
        }
    }

    fun setAuthMode(mode: AuthMode) {
        prefs.edit().putString(KEY_AUTH_MODE, mode.name).apply()
    }

    // PIN Management (6 digits minimum)
    fun getParentPin(): String {
        return prefs.getString(KEY_PARENT_PIN, "") ?: ""
    }

    fun setParentPin(pin: String): Boolean {
        // Validate PIN length
        if (pin.length < MIN_PIN_LENGTH) {
            return false
        }
        
        // Validate PIN contains only digits
        if (!pin.all { it.isDigit() }) {
            return false
        }
        
        // Reject weak/common PINs
        if (WEAK_PINS.contains(pin)) {
            return false
        }
        
        // Check for sequential patterns (e.g., 123456, 234567)
        if (isSequentialPin(pin)) {
            return false
        }
        
        // Check for repeated digits (e.g., 111111, 222222)
        if (pin.all { it == pin[0] }) {
            return false
        }
        
        prefs.edit().putString(KEY_PARENT_PIN, pin).apply()
        return true
    }
    
    /**
     * Check if PIN is sequential (e.g., 123456, 654321)
     */
    private fun isSequentialPin(pin: String): Boolean {
        if (pin.length < 2) return false
        
        var isAscending = true
        var isDescending = true
        
        for (i in 1 until pin.length) {
            val current = pin[i].digitToIntOrNull() ?: return false
            val previous = pin[i - 1].digitToIntOrNull() ?: return false
            
            if (current != previous + 1) isAscending = false
            if (current != previous - 1) isDescending = false
        }
        
        return isAscending || isDescending
    }

    // Password Management (8 characters minimum)
    fun getParentPassword(): String {
        return prefs.getString(KEY_PARENT_PASSWORD, "") ?: ""
    }

    fun setParentPassword(password: String): Boolean {
        // Validate password length
        if (password.length < MIN_PASSWORD_LENGTH) {
            return false
        }
        
        // Check for common weak passwords
        val weakPasswords = setOf(
            "password", "12345678", "qwerty", "abc123", "password123",
            "admin", "letmein", "welcome", "monkey", "1234567890"
        )
        if (weakPasswords.contains(password.lowercase())) {
            return false
        }
        
        // Check if password is all the same character
        if (password.all { it == password[0] }) {
            return false
        }
        
        // Check for at least one numeric digit
        if (!password.any { it.isDigit() }) {
            return false
        }
        
        // Check for at least one special character (@#$%&*!)
        val specialChars = "@#$%&*!"
        if (!password.any { it in specialChars }) {
            return false
        }
        
        prefs.edit().putString(KEY_PARENT_PASSWORD, password).apply()
        return true
    }
    
    fun validatePasswordRequirements(password: String): Pair<Boolean, String?> {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Pair(false, "Password must be at least $MIN_PASSWORD_LENGTH characters long")
        }
        
        val weakPasswords = setOf(
            "password", "12345678", "qwerty", "abc123", "password123",
            "admin", "letmein", "welcome", "monkey", "1234567890"
        )
        if (weakPasswords.contains(password.lowercase())) {
            return Pair(false, "This password is too common. Please choose a stronger password.")
        }
        
        if (password.all { it == password[0] }) {
            return Pair(false, "Password cannot be all the same character")
        }
        
        if (!password.any { it.isDigit() }) {
            return Pair(false, "Password must include at least one numeric digit (0-9)")
        }
        
        val specialChars = "@#$%&*!"
        if (!password.any { it in specialChars }) {
            return Pair(false, "Password must include at least one special character (@#$%&*!)")
        }
        
        return Pair(true, null)
    }

    // Verify authentication
    fun verifyAuth(input: String): Boolean {
        // Check reviewer unlock first (session-only bypass)
        if (why.xee.kidsview.utils.ReviewerUnlockManager.isUnlocked()) {
            return true
        }
        
        return when (getAuthMode()) {
            AuthMode.PIN -> input == getParentPin()
            AuthMode.PASSWORD -> input == getParentPassword()
        }
    }

    // Check if auth is set (and not empty)
    fun isAuthSet(): Boolean {
        // Check reviewer unlock first (session-only bypass)
        if (why.xee.kidsview.utils.ReviewerUnlockManager.isUnlocked()) {
            return true
        }
        
        return when (getAuthMode()) {
            AuthMode.PIN -> {
                val pin = getParentPin()
                pin.isNotEmpty() && pin.length >= MIN_PIN_LENGTH
            }
            AuthMode.PASSWORD -> {
                val password = getParentPassword()
                password.isNotEmpty() && password.length >= MIN_PASSWORD_LENGTH
            }
        }
    }

    // App Lock Management
    fun isAppLocked(): Boolean {
        return prefs.getBoolean(KEY_APP_LOCKED, false)
    }

    fun setAppLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCKED, locked).apply()
    }
    
    /**
     * Check if Screen Lock feature is enabled (via rewarded ad)
     * Feature disables after 24 hours and requires watching ad again
     */
    fun isScreenLockFeatureEnabled(): Boolean {
        if (!prefs.getBoolean(KEY_SCREEN_LOCK_FEATURE_ENABLED, false)) {
            return false
        }
        
        // Check if 24 hours have passed since feature was enabled
        val enableTime = prefs.getLong(KEY_SCREEN_LOCK_FEATURE_ENABLE_TIME, 0)
        if (enableTime == 0L) {
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val hoursPassed = (currentTime - enableTime) / (1000 * 60 * 60)
        if (hoursPassed >= 24) {
            // 24 hours passed, disable the feature
            prefs.edit().putBoolean(KEY_SCREEN_LOCK_FEATURE_ENABLED, false).putLong(KEY_SCREEN_LOCK_FEATURE_ENABLE_TIME, 0).apply()
            return false
        }
        
        return true
    }
    
    /**
     * Enable Screen Lock feature (after watching rewarded ad)
     * Sets the enable timestamp for 24-hour tracking
     */
    fun enableScreenLockFeature() {
        prefs.edit()
            .putBoolean(KEY_SCREEN_LOCK_FEATURE_ENABLED, true)
            .putLong(KEY_SCREEN_LOCK_FEATURE_ENABLE_TIME, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Get the time remaining until screen lock feature automatically disables (in hours)
     * Returns 0 if feature is disabled or if 24 hours have passed
     */
    fun getScreenLockFeatureRemainingHours(): Int {
        if (!prefs.getBoolean(KEY_SCREEN_LOCK_FEATURE_ENABLED, false)) {
            return 0
        }
        
        val enableTime = prefs.getLong(KEY_SCREEN_LOCK_FEATURE_ENABLE_TIME, 0)
        if (enableTime == 0L) return 0
        
        val currentTime = System.currentTimeMillis()
        val hoursPassed = (currentTime - enableTime) / (1000 * 60 * 60)
        val remaining = 24 - hoursPassed.toInt()
        return remaining.coerceAtLeast(0)
    }

    // Time Limit Management
    fun isTimeLimitEnabled(): Boolean {
        return prefs.getBoolean(KEY_TIME_LIMIT_ENABLED, false)
    }

    fun setTimeLimitEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TIME_LIMIT_ENABLED, enabled).apply()
    }

    /**
     * Base daily time limit in minutes (default 60, max 180 minutes / 3 hours).
     * This is the default daily watch time that resets automatically each day.
     */
    fun getBaseTimeLimitMinutes(): Int {
        val stored = prefs.getInt(KEY_TIME_LIMIT_MINUTES, 60) // Default changed to 60 minutes
        return stored.coerceIn(1, 180)
    }
    
    /**
     * Effective daily limit = base (default 60, max 180 minutes) + manually applied earned time.
     * Earned time must be manually applied by parent and does not auto-apply.
     * Daily cap is 180 minutes and must never be exceeded.
     */
    fun getTimeLimitMinutes(): Int {
        val base = getBaseTimeLimitMinutes()
        val appliedEarned = getAppliedEarnedTimeToday()
        val total = base + appliedEarned
        // Cap at 180 minutes - never exceed daily cap
        return total.coerceAtMost(180)
    }
    
    /**
     * Get earned time that has been manually applied to today's limit.
     * This resets daily at midnight (12:00am) and does not auto-apply.
     */
    fun getAppliedEarnedTimeToday(): Int {
        val lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, 0)
        
        if (isPastMidnight(lastResetDate)) {
            // Past midnight, reset applied earned time
            prefs.edit().putInt(KEY_APPLIED_EARNED_TIME, 0).apply()
            return 0
        }
        
        return prefs.getInt(KEY_APPLIED_EARNED_TIME, 0)
    }
    
    /**
     * Clear applied earned time (return it to wallet by removing from applied state).
     * This is used when base time limit is reduced.
     */
    fun clearAppliedEarnedTime() {
        prefs.edit().putInt(KEY_APPLIED_EARNED_TIME, 0).apply()
    }
    
    /**
     * Set applied earned time to a specific value.
     * Used when reducing effective time limit.
     * @param minutes New applied earned time value
     */
    fun setAppliedEarnedTime(minutes: Int) {
        val clamped = minutes.coerceAtLeast(0)
        prefs.edit().putInt(KEY_APPLIED_EARNED_TIME, clamped).apply()
    }
    
    /**
     * Reset wallet at midnight (12:00am) - clears all earned time entries
     */
    private fun resetWalletAtMidnight() {
        // Clear all wallet entries at midnight
        prefs.edit().putString(KEY_TIME_BONUS_ENTRIES, "[]").apply()
        AppLogger.d("PreferencesManager: Wallet reset at midnight")
    }
    
    /**
     * Set base daily limit (1–180 minutes / up to 3 hours). Changing base resets today's usage.
     */
    fun setTimeLimitMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 180)
        val oldLimit = getBaseTimeLimitMinutes()
        prefs.edit().putInt(KEY_TIME_LIMIT_MINUTES, clamped).apply()
        // If the base time limit changed, reset the usage to allow immediate use of new limit
        if (oldLimit != clamped) {
            resetTimeUsedToday()
        }
    }

    /**
     * Check if it's past midnight (12:00am) since last reset
     */
    private fun isPastMidnight(lastResetTime: Long): Boolean {
        if (lastResetTime == 0L) return true
        
        val calendar = java.util.Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        // Get today's midnight
        calendar.timeInMillis = now
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val todayMidnight = calendar.timeInMillis
        
        // Get last reset day's midnight
        calendar.timeInMillis = lastResetTime
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val lastResetMidnight = calendar.timeInMillis
        
        // Check if we've passed midnight since last reset
        return now >= todayMidnight && todayMidnight > lastResetMidnight
    }
    
    fun getTimeUsedToday(): Long {
        // Reset if it's past midnight (12:00am)
        val lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, 0)
        
        if (isPastMidnight(lastResetDate)) {
            // Past midnight, reset usage, applied earned time, and wallet
            resetTimeUsedToday()
            resetWalletAtMidnight()
            return 0
        }
        
        return prefs.getLong(KEY_TIME_USED_TODAY, 0)
    }

    fun addTimeUsed(milliseconds: Long) {
        // Check if we need to reset first (past midnight)
        val lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, 0)
        val current: Long = if (isPastMidnight(lastResetDate)) {
            // Past midnight, reset first
            resetTimeUsedToday()
            0L
        } else {
            // Not past midnight, read current value directly (without calling getTimeUsedToday to avoid double-check)
            prefs.getLong(KEY_TIME_USED_TODAY, 0)
        }
        prefs.edit().putLong(KEY_TIME_USED_TODAY, current + milliseconds).apply()
    }

    fun resetTimeUsedToday() {
        prefs.edit()
            .putLong(KEY_TIME_USED_TODAY, 0)
            .putLong(KEY_LAST_RESET_DATE, System.currentTimeMillis())
            .putInt(KEY_APPLIED_EARNED_TIME, 0) // Reset applied earned time
            .apply()
    }
    
    /**
     * Reset Daily Limit: Requires rewarded ad completion, deducts 60 minutes from earned time.
     * Restores remaining watch time to 180 minutes (daily cap).
     * 
     * Deduction priority:
     * 1. First try to deduct from wallet (earned time)
     * 2. If wallet doesn't have enough, deduct from applied time
     * 3. If still not enough, combine from both wallet and applied time
     * 
     * @return ResetDailyLimitResult with success status and deduction details
     */
    fun resetDailyLimit(): ResetDailyLimitResult {
        val REQUIRED_MINUTES = 60
        val functionStartTime = System.currentTimeMillis()
        Log.d("ResetDailyLimit", "═══════════════════════════════════════════════════════════")
        Log.d("ResetDailyLimit", "Function started at ${functionStartTime}")
        
        // OPTIMIZATION: Parse wallet entries ONCE and reuse - skip expiration check during reset for speed
        // Expiration is checked on read, not during write operations
        val parseStartTime = System.currentTimeMillis()
        val entries = getRewardTimeEntries()
        val parseDuration = System.currentTimeMillis() - parseStartTime
        Log.d("ResetDailyLimit", "getRewardTimeEntries() took ${parseDuration}ms - Found ${entries.size} entries")
        
        // Calculate wallet time from cached entries (no JSON parse)
        val calcStartTime = System.currentTimeMillis()
        val walletTime = entries.sumOf { it.minutes }
        val appliedTime = getAppliedEarnedTimeToday()
        val totalAvailable = walletTime + appliedTime
        val calcDuration = System.currentTimeMillis() - calcStartTime
        Log.d("ResetDailyLimit", "Time calculation took ${calcDuration}ms - Wallet: $walletTime, Applied: $appliedTime, Total: $totalAvailable")
        
        // Check if we have enough total time
        if (totalAvailable < REQUIRED_MINUTES) {
            return ResetDailyLimitResult(
                success = false,
                message = "Insufficient earned time. Need $REQUIRED_MINUTES minutes, have $totalAvailable minutes (Wallet: $walletTime, Applied: $appliedTime)."
            )
        }
        
        var walletDeducted = 0
        var appliedTimeDeducted = 0
        var remainingToDeduct = REQUIRED_MINUTES
        
        // Step 1: Deduct from wallet first (up to available amount)
        // Use cached entries directly instead of calling consumeRewardTime (which would parse JSON again)
        val walletDeductionStartTime = System.currentTimeMillis()
        val updatedEntries = mutableListOf<RewardTimeEntry>()
        if (walletTime > 0 && remainingToDeduct > 0) {
            walletDeducted = walletTime.coerceAtMost(remainingToDeduct)
            var remainingToConsume = walletDeducted
            Log.d("ResetDailyLimit", "Deducting $walletDeducted minutes from wallet (wallet has $walletTime, need $remainingToDeduct)")
            
            // Consume from cached entries (already sorted by timestamp)
            for (entry in entries) {
                if (remainingToConsume <= 0) {
                    updatedEntries.add(entry)
                } else if (entry.minutes <= remainingToConsume) {
                    remainingToConsume -= entry.minutes
                    // Don't add this entry (fully consumed)
                } else {
                    val remaining = entry.minutes - remainingToConsume
                    if (remaining > 0) {
                        updatedEntries.add(RewardTimeEntry(entry.timestamp, remaining))
                    }
                    remainingToConsume = 0
                }
            }
            
            // Save updated entries (async, non-blocking)
            val saveStartTime = System.currentTimeMillis()
            saveRewardTimeEntries(updatedEntries)
            val saveDuration = System.currentTimeMillis() - saveStartTime
            Log.d("ResetDailyLimit", "saveRewardTimeEntries() took ${saveDuration}ms - ${updatedEntries.size} entries saved")
            remainingToDeduct -= walletDeducted
            Log.d("ResetDailyLimit", "After wallet deduction: remainingToDeduct = $remainingToDeduct")
        } else {
            // No wallet deduction (wallet is empty or 0), keep all entries
            updatedEntries.addAll(entries)
            Log.d("ResetDailyLimit", "Wallet is empty or 0 ($walletTime), skipping wallet deduction. remainingToDeduct = $remainingToDeduct")
        }
        val walletDeductionDuration = System.currentTimeMillis() - walletDeductionStartTime
        Log.d("ResetDailyLimit", "Wallet deduction logic took ${walletDeductionDuration}ms - walletDeducted: $walletDeducted, remainingToDeduct: $remainingToDeduct")
        
        // Step 2: If still need more (or wallet was empty/insufficient), deduct from applied time
        var newApplied = appliedTime // Initialize with current applied time
        if (remainingToDeduct > 0) {
            // Use the appliedTime we already calculated (handles midnight reset correctly)
            // This ensures consistency with the initial check
            if (appliedTime > 0) {
                // Deduct the remaining amount from applied time
                appliedTimeDeducted = appliedTime.coerceAtMost(remainingToDeduct)
                newApplied = (appliedTime - appliedTimeDeducted).coerceAtLeast(0)
                remainingToDeduct -= appliedTimeDeducted
                Log.d("ResetDailyLimit", "Deducting $appliedTimeDeducted minutes from applied time (had $appliedTime, remaining: $newApplied, still need: $remainingToDeduct)")
            } else {
                Log.d("ResetDailyLimit", "No applied time available to deduct (appliedTime: $appliedTime, remainingToDeduct: $remainingToDeduct)")
            }
        } else {
            Log.d("ResetDailyLimit", "No need to deduct from applied time (remainingToDeduct: $remainingToDeduct, appliedTime: $appliedTime)")
            // No deduction needed, keep current applied time (already set above)
        }
        
        // Verify we deducted exactly 60 minutes
        if (walletDeducted + appliedTimeDeducted != REQUIRED_MINUTES) {
            AppLogger.e("PreferencesManager: Reset deduction mismatch. Expected $REQUIRED_MINUTES, got ${walletDeducted + appliedTimeDeducted}")
            return ResetDailyLimitResult(
                success = false,
                walletDeducted = walletDeducted,
                appliedTimeDeducted = appliedTimeDeducted,
                message = "Reset failed due to calculation error."
            )
        }
        
        // Get current base limit to preserve it (don't force to 180)
        val currentBase = getBaseTimeLimitMinutes()
        val newEffectiveLimit = currentBase + newApplied
        val effectiveLimitCapped = newEffectiveLimit.coerceAtMost(180)
        
        Log.d("ResetDailyLimit", "Current base: $currentBase, New applied: $newApplied, New effective: $effectiveLimitCapped")
        
        // Batch all SharedPreferences writes - use apply() for async (non-blocking) save
        // Only wallet consumption needs commit() for immediate save
        val prefsWriteStartTime = System.currentTimeMillis()
        val editor = prefs.edit()
        // Always save applied time (even if 0) to ensure consistency
        editor.putInt(KEY_APPLIED_EARNED_TIME, newApplied)
        // Reset time used today - preserve current base limit (don't force to 180)
        // The effective limit will be: base + newApplied (after deduction), capped at 180
        editor.putLong(KEY_TIME_USED_TODAY, 0)
        editor.putLong(KEY_LAST_RESET_DATE, System.currentTimeMillis())
        // Keep current base limit - don't force to 180
        // If user wants max time, they can set it manually
        editor.apply() // Use apply() for async save - non-blocking, faster
        val prefsWriteDuration = System.currentTimeMillis() - prefsWriteStartTime
        Log.d("ResetDailyLimit", "SharedPreferences writes took ${prefsWriteDuration}ms")
        
        val totalDuration = System.currentTimeMillis() - functionStartTime
        Log.d("ResetDailyLimit", "Total resetDailyLimit() execution took ${totalDuration}ms")
        Log.d("ResetDailyLimit", "═══════════════════════════════════════════════════════════")
        
        // Build success message with deduction details
        val messageParts = mutableListOf<String>()
        messageParts.add("Daily limit reset. Effective limit: $effectiveLimitCapped minutes (Base: $currentBase + Applied: $newApplied).")
        if (walletDeducted > 0 && appliedTimeDeducted > 0) {
            messageParts.add("Deducted $walletDeducted min from wallet and $appliedTimeDeducted min from applied time.")
        } else if (walletDeducted > 0) {
            messageParts.add("Deducted $walletDeducted min from wallet.")
        } else if (appliedTimeDeducted > 0) {
            messageParts.add("Deducted $appliedTimeDeducted min from applied time.")
        }
        
        return ResetDailyLimitResult(
            success = true,
            walletDeducted = walletDeducted,
            appliedTimeDeducted = appliedTimeDeducted,
            message = messageParts.joinToString(" ")
        )
    }
    
    /**
     * Legacy method for backward compatibility - now calls resetDailyLimit()
     */
    fun resetTimeAndSetBonusToMax() {
        resetDailyLimit()
    }

    // Check if time limit exceeded
    fun isTimeLimitExceeded(): Boolean {
        if (!isTimeLimitEnabled()) return false
        val limitMs = getTimeLimitMinutes() * 60 * 1000L
        return getTimeUsedToday() >= limitMs
    }

    /**
     * Get total earned time in wallet (from rewarded ads).
     * Wallet resets at midnight (12:00am) daily.
     * Each 30-minute chunk expires 8 hours after being added (if not reset at midnight first).
     * Expired chunks are automatically removed.
     * This is the earned time wallet - does NOT auto-apply to daily limit.
     */
    fun getTotalEarnedTimeMinutes(): Int {
        // Check if wallet should be reset at midnight
        val lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, 0)
        if (isPastMidnight(lastResetDate) && lastResetDate > 0) {
            // Past midnight, wallet was already reset in getTimeUsedToday()
            // But if this is called before getTimeUsedToday(), reset it here
            val entries = getRewardTimeEntries()
            if (entries.isNotEmpty()) {
                resetWalletAtMidnight()
            }
            return 0
        }
        
        val entries = getRewardTimeEntries()
        val now = System.currentTimeMillis()
        val eightHoursMs = 8 * 60 * 60 * 1000L
        
        // Filter out expired entries (older than 8 hours)
        val validEntries = entries.filter { entry ->
            val age = now - entry.timestamp
            age < eightHoursMs
        }
        
        // Save back the valid entries (removing expired ones)
        if (validEntries.size != entries.size) {
            saveRewardTimeEntries(validEntries)
        }
        
        // Sum up all valid earned time minutes in wallet
        return validEntries.sumOf { it.minutes }
    }
    
    /**
     * Data class for reward time entries
     */
    /**
     * Result of reset daily limit operation
     */
    data class ResetDailyLimitResult(
        val success: Boolean,
        val walletDeducted: Int = 0, // Minutes deducted from wallet
        val appliedTimeDeducted: Int = 0, // Minutes deducted from applied time
        val message: String = ""
    )
    
    private data class RewardTimeEntry(
        val timestamp: Long, // When this bonus was added
        val minutes: Int     // Always 30 minutes per rewarded ad
    )
    
    /**
     * Get all reward time entries from storage
     * Optimized to avoid unnecessary operations
     */
    private fun getRewardTimeEntries(): List<RewardTimeEntry> {
        val jsonString = prefs.getString(KEY_TIME_BONUS_ENTRIES, null) ?: return emptyList()
        if (jsonString.isEmpty() || jsonString == "[]") return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val entries = ArrayList<RewardTimeEntry>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                entries.add(
                    RewardTimeEntry(
                        timestamp = obj.getLong("timestamp"),
                        minutes = obj.getInt("minutes")
                    )
                )
            }
            entries.sortedBy { it.timestamp } // Sort by oldest first
        } catch (e: Exception) {
            AppLogger.e("Error parsing reward time entries", e)
            emptyList()
        }
    }
    
    /**
     * Save reward time entries to storage (asynchronous)
     */
    private fun saveRewardTimeEntries(entries: List<RewardTimeEntry>) {
        try {
            val jsonArray = JSONArray()
            entries.forEach { entry ->
                val obj = JSONObject()
                obj.put("timestamp", entry.timestamp)
                obj.put("minutes", entry.minutes)
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_TIME_BONUS_ENTRIES, jsonArray.toString()).apply()
        } catch (e: Exception) {
            AppLogger.e("Error saving reward time entries", e)
        }
    }
    
    /**
     * Save reward time entries to storage (synchronous - for critical operations)
     * Uses commit() to ensure immediate persistence
     */
    private fun saveRewardTimeEntriesSync(entries: List<RewardTimeEntry>) {
        try {
            if (entries.isEmpty()) {
                val success = prefs.edit().putString(KEY_TIME_BONUS_ENTRIES, "[]").commit()
                if (!success) {
                    Log.e("PreferencesManager", "Failed to save empty reward time entries (commit returned false)")
                }
                return
            }
            
            // Build JSON string more efficiently
            val jsonString = buildString {
                append("[")
                entries.forEachIndexed { index, entry ->
                    if (index > 0) append(",")
                    append("{\"timestamp\":")
                    append(entry.timestamp)
                    append(",\"minutes\":")
                    append(entry.minutes)
                    append("}")
                }
                append("]")
            }
            val success = prefs.edit().putString(KEY_TIME_BONUS_ENTRIES, jsonString).commit()
            if (!success) {
                Log.e("PreferencesManager", "Failed to save reward time entries (commit returned false)")
            }
        } catch (e: Exception) {
            AppLogger.e("Error saving reward time entries", e)
        }
    }
    
    /**
     * Consume minutes from reward time pool (oldest chunks first)
     * @param minutesToConsume Number of minutes to remove from the pool
     */
    private fun consumeRewardTime(minutesToConsume: Int) {
        if (minutesToConsume <= 0) return
        
        val entries = getRewardTimeEntries()
        var remainingToConsume = minutesToConsume
        val updatedEntries = mutableListOf<RewardTimeEntry>()
        
        // Remove oldest chunks first until we've consumed enough
        // Entries are already sorted by timestamp from getRewardTimeEntries()
        for (entry in entries) {
            if (remainingToConsume <= 0) {
                // Keep this entry
                updatedEntries.add(entry)
            } else if (entry.minutes <= remainingToConsume) {
                // Consume this entire entry
                remainingToConsume -= entry.minutes
                // Don't add this entry (it's fully consumed)
            } else {
                // Partially consume this entry (shouldn't happen with 30-min chunks, but handle it)
                val remaining = entry.minutes - remainingToConsume
                if (remaining > 0) {
                    updatedEntries.add(RewardTimeEntry(entry.timestamp, remaining))
                }
                remainingToConsume = 0
            }
        }
        
        // Use commit() instead of apply() to ensure synchronous save
        saveRewardTimeEntriesSync(updatedEntries)
    }

    /**
     * Grant 30 minutes to earned time wallet via rewarded ad.
     * This earned time does NOT auto-apply - parent must manually add it.
     * Earned time expires 8 hours after being added.
     */
    fun addEarnedTimeToWallet() {
        val entries = getRewardTimeEntries().toMutableList()
        val now = System.currentTimeMillis()
        
        // NEW: Add new 15-minute entry with current timestamp to wallet (changed from 30 to 15 minutes)
        entries.add(RewardTimeEntry(timestamp = now, minutes = 15))
        
        saveRewardTimeEntries(entries)
    }
    
    /**
     * Consume (deduct) specific amount of minutes from earned time wallet.
     * Used when base time is increased beyond default (60 minutes).
     * @param minutes Number of minutes to consume
     * @return true if consumption was successful, false if insufficient wallet time
     */
    fun consumeMinutesFromWallet(minutes: Int): Boolean {
        if (minutes <= 0) return true
        val walletTime = getTotalEarnedTimeMinutes()
        if (walletTime < minutes) {
            return false
        }
        consumeRewardTime(minutes)
        return true
    }
    
    /**
     * Add specific amount of minutes to earned time wallet.
     * Used when user reduces time limit and remaining time should be added to wallet.
     * @param minutes Number of minutes to add to wallet
     */
    fun addMinutesToWallet(minutes: Int) {
        if (minutes <= 0) {
            Log.w("PreferencesManager", "addMinutesToWallet: Invalid minutes ($minutes), skipping")
            return
        }
        
        val entries = getRewardTimeEntries().toMutableList()
        val now = System.currentTimeMillis()
        val oldWalletTotal = entries.sumOf { it.minutes }
        
        // Add entry with specified minutes to wallet
        entries.add(RewardTimeEntry(timestamp = now, minutes = minutes))
        
        val newWalletTotal = entries.sumOf { it.minutes }
        Log.d("PreferencesManager", "addMinutesToWallet: Adding $minutes minutes. Old wallet: $oldWalletTotal, New wallet: $newWalletTotal")
        
        // Use synchronous save to ensure it's persisted immediately
        saveRewardTimeEntriesSync(entries)
        
        // Verify it was saved
        val verifyEntries = getRewardTimeEntries()
        val verifyTotal = verifyEntries.sumOf { it.minutes }
        Log.d("PreferencesManager", "addMinutesToWallet: Verification - Wallet total after save: $verifyTotal")
        
        if (verifyTotal != newWalletTotal) {
            Log.e("PreferencesManager", "addMinutesToWallet: WARNING - Wallet total mismatch! Expected: $newWalletTotal, Got: $verifyTotal")
        }
    }
    
    /**
     * Legacy method for backward compatibility - now calls addEarnedTimeToWallet()
     */
    fun addBonusMinutesForToday() {
        addEarnedTimeToWallet()
    }
    
    /**
     * Manually apply earned time from wallet to today's daily limit.
     * When time is applied, it reduces the used time so that remaining time = newly added time.
     * @param minutesToApply Number of minutes to apply (must be available in wallet)
     * @return true if applied successfully, false if insufficient earned time
     */
    fun applyEarnedTimeToDailyLimit(minutesToApply: Int): Boolean {
        if (minutesToApply <= 0) {
            Log.w("PreferencesManager", "applyEarnedTimeToDailyLimit: Invalid minutes ($minutesToApply), must be > 0")
            return false
        }
        
        // Get current state - read wallet FIRST to get most up-to-date value
        val totalEarned = getTotalEarnedTimeMinutes()
        val currentApplied = getAppliedEarnedTimeToday()
        val base = getBaseTimeLimitMinutes()
        
        Log.d("PreferencesManager", "applyEarnedTimeToDailyLimit: Requested=$minutesToApply, Wallet=$totalEarned, Applied=$currentApplied, Base=$base")
        
        // CRITICAL: Check wallet FIRST - cannot apply more than available
        if (minutesToApply > totalEarned) {
            Log.e("PreferencesManager", "applyEarnedTimeToDailyLimit: BLOCKED - Insufficient wallet. Requested: $minutesToApply, Available: $totalEarned")
            return false // Insufficient earned time in wallet
        }
        
        // Calculate how much can actually be applied (considering 180-minute cap)
        val maxCanApplyByCap = 180 - base - currentApplied
        if (maxCanApplyByCap <= 0) {
            Log.w("PreferencesManager", "applyEarnedTimeToDailyLimit: Already at maximum (180 minutes). Base: $base, Applied: $currentApplied")
            return false // Already at or over cap
        }
        
        // Apply only what fits within both constraints: wallet limit AND 180-minute cap
        // IMPORTANT: Use the minimum of what's requested, what's in wallet, and what fits in cap
        val actualApplied = minOf(minutesToApply, totalEarned, maxCanApplyByCap)
        
        if (actualApplied <= 0) {
            Log.e("PreferencesManager", "applyEarnedTimeToDailyLimit: BLOCKED - Cannot apply any time. Requested: $minutesToApply, Wallet: $totalEarned, Max by cap: $maxCanApplyByCap")
            return false
        }
        
        // DOUBLE-CHECK: Verify wallet still has enough before consuming
        val walletBeforeConsume = getTotalEarnedTimeMinutes()
        if (actualApplied > walletBeforeConsume) {
            Log.e("PreferencesManager", "applyEarnedTimeToDailyLimit: BLOCKED - Wallet changed. Need: $actualApplied, Have: $walletBeforeConsume")
            return false
        }
        
        if (actualApplied < minutesToApply) {
            Log.d("PreferencesManager", "applyEarnedTimeToDailyLimit: Applying $actualApplied minutes (requested $minutesToApply was reduced due to constraints)")
        }
        
        // Consume from wallet FIRST (before applying to daily limit)
        consumeRewardTime(actualApplied)
        
        // VERIFY consumption worked
        val walletAfterConsume = getTotalEarnedTimeMinutes()
        val expectedWalletAfter = walletBeforeConsume - actualApplied
        if (walletAfterConsume != expectedWalletAfter) {
            Log.e("PreferencesManager", "applyEarnedTimeToDailyLimit: ERROR - Wallet consumption mismatch. Expected: $expectedWalletAfter, Got: $walletAfterConsume")
            // Try to rollback - this shouldn't happen, but handle it
            return false
        }
        
        // Then apply to daily limit
        val newApplied = currentApplied + actualApplied
        prefs.edit().putInt(KEY_APPLIED_EARNED_TIME, newApplied).apply()
        
        // CRITICAL FIX: Reduce used time by the amount applied (but not below 0)
        // This ensures that when time is added, remaining time = newly added time
        val currentUsedMs = getTimeUsedToday()
        val appliedMs = actualApplied * 60 * 1000L
        val newUsedMs = (currentUsedMs - appliedMs).coerceAtLeast(0L)
        prefs.edit().putLong(KEY_TIME_USED_TODAY, newUsedMs).apply()
        
        Log.d("PreferencesManager", "applyEarnedTimeToDailyLimit: SUCCESS - Applied $actualApplied minutes. Base: $base, Applied: $currentApplied -> $newApplied, Effective: ${base + newApplied}, Wallet: $walletBeforeConsume -> $walletAfterConsume")
        
        return true
    }

    // Security Questions Management
    fun setSecurityQuestion(questionNumber: Int, question: String, answer: String) {
        when (questionNumber) {
            1 -> {
                prefs.edit()
                    .putString(KEY_SECURITY_QUESTION_1, question)
                    .putString(KEY_SECURITY_ANSWER_1, answer.lowercase().trim())
                    .apply()
            }
            2 -> {
                prefs.edit()
                    .putString(KEY_SECURITY_QUESTION_2, question)
                    .putString(KEY_SECURITY_ANSWER_2, answer.lowercase().trim())
                    .apply()
            }
            3 -> {
                prefs.edit()
                    .putString(KEY_SECURITY_QUESTION_3, question)
                    .putString(KEY_SECURITY_ANSWER_3, answer.lowercase().trim())
                    .apply()
            }
        }
    }
    
    fun getSecurityQuestion(questionNumber: Int): String? {
        return when (questionNumber) {
            1 -> prefs.getString(KEY_SECURITY_QUESTION_1, null)
            2 -> prefs.getString(KEY_SECURITY_QUESTION_2, null)
            3 -> prefs.getString(KEY_SECURITY_QUESTION_3, null)
            else -> null
        }
    }
    
    fun verifySecurityAnswer(questionNumber: Int, answer: String): Boolean {
        val correctAnswer = when (questionNumber) {
            1 -> prefs.getString(KEY_SECURITY_ANSWER_1, null)
            2 -> prefs.getString(KEY_SECURITY_ANSWER_2, null)
            3 -> prefs.getString(KEY_SECURITY_ANSWER_3, null)
            else -> null
        }
        return correctAnswer != null && correctAnswer == answer.lowercase().trim()
    }
    
    fun hasSecurityQuestionsSet(): Boolean {
        // Check reviewer unlock first (session-only bypass)
        if (why.xee.kidsview.utils.ReviewerUnlockManager.isUnlocked()) {
            return true
        }
        return getSecurityQuestion(1) != null && 
               getSecurityQuestion(2) != null && 
               getSecurityQuestion(3) != null
    }
    
    fun verifySecurityQuestions(answers: List<String>): Boolean {
        if (answers.size != 3) return false
        return verifySecurityAnswer(1, answers[0]) &&
               verifySecurityAnswer(2, answers[1]) &&
               verifySecurityAnswer(3, answers[2])
    }

    // Rejection Message Read Status Management
    fun markRejectionAsRead(requestId: String) {
        prefs.edit().putBoolean("$KEY_REJECTION_READ_PREFIX$requestId", true).apply()
    }
    
    fun isRejectionRead(requestId: String): Boolean {
        return prefs.getBoolean("$KEY_REJECTION_READ_PREFIX$requestId", false)
    }
    
    // Instructions Dialog Management
    /**
     * Check if instructions have been shown for the current app version
     * @param currentVersionCode The current version code (e.g., BuildConfig.VERSION_CODE)
     * @return true if instructions have been shown for this version, false otherwise
     */
    fun hasInstructionsBeenShownForVersion(currentVersionCode: Int): Boolean {
        val shownVersion = prefs.getInt(KEY_INSTRUCTIONS_SHOWN_VERSION, 0)
        return shownVersion == currentVersionCode
    }
    
    /**
     * Mark instructions as shown for the current app version
     * @param currentVersionCode The current version code (e.g., BuildConfig.VERSION_CODE)
     */
    fun markInstructionsShownForVersion(currentVersionCode: Int) {
        prefs.edit().putInt(KEY_INSTRUCTIONS_SHOWN_VERSION, currentVersionCode).apply()
    }
    
    // Helper methods for ReviewManager
    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }
    
    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }
    
    fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }
    
    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }
    
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    
    // New Year Splash Management
    fun isNewYearSplashDisabled(): Boolean {
        return prefs.getBoolean(KEY_NEW_YEAR_SPLASH_DISABLED, false)
    }
    
    fun setNewYearSplashDisabled(disabled: Boolean) {
        prefs.edit().putBoolean(KEY_NEW_YEAR_SPLASH_DISABLED, disabled).apply()
    }
    
    // Legacy methods for backward compatibility
}

