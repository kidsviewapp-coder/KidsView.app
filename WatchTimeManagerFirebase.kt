package why.xee.kidsview.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * WatchTimeManagerFirebase - Manages daily watch time with Firebase backend
 * 
 * Features:
 * - Backward compatibility with existing local ad-based system
 * - Firebase Firestore integration for cloud sync
 * - Default base watch time: 60 minutes (1 hour) per day
 * - Wallet system: Earn 30 minutes per rewarded ad
 * - Maximum daily watch time: 180 minutes (3 hours)
 * - Automatic midnight reset (12:00 AM)
 * - Reset to max requires 3 consecutive rewarded ads
 * - Timer only counts during video playback
 * 
 * Migration Strategy:
 * - Existing users: Continue using local SharedPreferences (isMigratedToFirebase = false)
 * - New/Migrated users: Use Firebase Firestore (isMigratedToFirebase = true)
 * 
 * IMPORTANT: For backward compatibility, this integrates with existing PreferencesManager
 * when not migrated. Pass PreferencesManager instance for full integration.
 */
class WatchTimeManagerFirebase private constructor(
    context: Context,
    private val preferencesManager: PreferencesManager? = null
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val PREFS_NAME = "watchtime_prefs"
        
        // SharedPreferences keys (for backward compatibility)
        private const val KEY_BASE_TIME = "base_time_minutes"
        private const val KEY_APPLIED_EXTRA_TIME = "applied_extra_time"
        private const val KEY_WALLET_TIME = "wallet_time_minutes"
        private const val KEY_TIME_USED_TODAY = "time_used_today_ms"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_RESET_AD_COUNT = "reset_ad_count"
        private const val KEY_RESET_AD_START_TIME = "reset_ad_start_time"
        private const val KEY_IS_MIGRATED_TO_FIREBASE = "is_migrated_to_firebase" // Migration flag
        
        // Firebase Firestore paths
        private const val COLLECTION_USERS = "users"
        private const val FIELD_BASE_TIME = "baseTime"
        private const val FIELD_APPLIED_EXTRA_TIME = "appliedExtraTime"
        private const val FIELD_WALLET_TIME = "walletTime"
        private const val FIELD_TIME_USED_TODAY = "timeUsedToday"
        private const val FIELD_LAST_RESET_DATE = "lastResetDate"
        private const val FIELD_RESET_AD_COUNT = "resetAdCount"
        private const val FIELD_RESET_AD_START_TIME = "resetAdStartTime"
        private const val FIELD_IS_MIGRATED = "isMigratedToFirebase"
        
        // Constants
        private const val DEFAULT_BASE_TIME = 60        // 1 hour default
        private const val MAX_DAILY_TIME = 180           // 3 hours maximum
        private const val WALLET_EARN_PER_AD = 30       // 30 minutes per ad
        private const val RESET_ADS_REQUIRED = 3        // 3 consecutive ads for reset
        private const val RESET_AD_TIMEOUT_MS = 300000L // 5 minutes timeout between ads
        
        @Volatile
        private var INSTANCE: WatchTimeManagerFirebase? = null
        
        fun getInstance(context: Context, preferencesManager: PreferencesManager? = null): WatchTimeManagerFirebase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchTimeManagerFirebase(context.applicationContext, preferencesManager).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Initialize: Check for midnight reset on app start
     */
    init {
        checkMidnightReset()
    }
    
    // ==================== Migration & Backward Compatibility ====================
    
    /**
     * Check if user has migrated to Firebase system
     * @return true if using Firebase, false if using local system (backward compatibility)
     */
    fun isMigratedToFirebase(): Boolean {
        return prefs.getBoolean(KEY_IS_MIGRATED_TO_FIREBASE, false)
    }
    
    /**
     * Migrate user from local system to Firebase
     * This should be called once when user logs in or opts to migrate
     * @return true if migration successful, false otherwise
     */
    suspend fun migrateToFirebase(): Boolean {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("WatchTimeManager", "Cannot migrate: User not logged in")
            return false
        }
        
        try {
            // Read current local values
            val baseTime = getBaseTimeLocal()
            val appliedExtraTime = getAppliedExtraTimeLocal()
            val walletTime = getWalletTimeLocal()
            val timeUsedToday = getTimeUsedTodayLocal()
            val lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, 0)
            val resetAdCount = prefs.getInt(KEY_RESET_AD_COUNT, 0)
            val resetAdStartTime = prefs.getLong(KEY_RESET_AD_START_TIME, 0)
            
            // Write to Firebase
            val userDoc = firestore.collection(COLLECTION_USERS).document(userId)
            userDoc.set(mapOf(
                FIELD_BASE_TIME to baseTime,
                FIELD_APPLIED_EXTRA_TIME to appliedExtraTime,
                FIELD_WALLET_TIME to walletTime,
                FIELD_TIME_USED_TODAY to timeUsedToday,
                FIELD_LAST_RESET_DATE to lastResetDate,
                FIELD_RESET_AD_COUNT to resetAdCount,
                FIELD_RESET_AD_START_TIME to resetAdStartTime,
                FIELD_IS_MIGRATED to true
            )).await()
            
            // Mark as migrated in local prefs
            prefs.edit().putBoolean(KEY_IS_MIGRATED_TO_FIREBASE, true).apply()
            
            Log.d("WatchTimeManager", "Successfully migrated user $userId to Firebase")
            return true
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Migration failed", e)
            return false
        }
    }
    
    /**
     * Get user document reference in Firestore
     */
    private fun getUserDocRef() = firestore.collection(COLLECTION_USERS).document(
        auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    )
    
    // ==================== Core Time Management ====================
    
    /**
     * Get base watch time (default: 60 minutes / 1 hour)
     * Uses Firebase if migrated, otherwise local storage (backward compatibility)
     */
    suspend fun getBaseTime(): Int {
        return if (isMigratedToFirebase()) {
            getBaseTimeFirebase()
        } else {
            getBaseTimeLocal()
        }
    }
    
    /**
     * Get base time from local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private fun getBaseTimeLocal(): Int {
        return preferencesManager?.getBaseTimeLimitMinutes() 
            ?: prefs.getInt(KEY_BASE_TIME, DEFAULT_BASE_TIME).coerceIn(1, MAX_DAILY_TIME)
    }
    
    /**
     * Get base time from Firebase
     */
    private suspend fun getBaseTimeFirebase(): Int {
        return try {
            val snapshot = getUserDocRef().get().await()
            (snapshot.getLong(FIELD_BASE_TIME)?.toInt() ?: DEFAULT_BASE_TIME).coerceIn(1, MAX_DAILY_TIME)
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error reading base time from Firebase", e)
            DEFAULT_BASE_TIME // Fallback to default
        }
    }
    
    /**
     * Set base watch time (1-180 minutes)
     * Updates Firebase if migrated, otherwise local storage
     */
    suspend fun setBaseTime(minutes: Int) {
        val clamped = minutes.coerceIn(1, MAX_DAILY_TIME)
        
        if (isMigratedToFirebase()) {
            setBaseTimeFirebase(clamped)
        } else {
            setBaseTimeLocal(clamped)
        }
    }
    
    /**
     * Set base time in local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private fun setBaseTimeLocal(minutes: Int) {
        if (preferencesManager != null) {
            preferencesManager.setTimeLimitMinutes(minutes)
            Log.d("WatchTimeManager", "Base time set to $minutes minutes via PreferencesManager (local)")
        } else {
            prefs.edit().putInt(KEY_BASE_TIME, minutes).apply()
            Log.d("WatchTimeManager", "Base time set to $minutes minutes (local)")
        }
    }
    
    /**
     * Set base time in Firebase (atomic update)
     */
    private suspend fun setBaseTimeFirebase(minutes: Int) {
        try {
            getUserDocRef().update(FIELD_BASE_TIME, minutes).await()
            Log.d("WatchTimeManager", "Base time set to $minutes minutes (Firebase)")
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error setting base time in Firebase", e)
            throw e
        }
    }
    
    /**
     * Get applied extra time from wallet (increases daily limit)
     * Uses Firebase if migrated, otherwise local storage
     */
    suspend fun getAppliedExtraTime(): Int {
        checkMidnightReset() // Check reset before returning
        return if (isMigratedToFirebase()) {
            getAppliedExtraTimeFirebase()
        } else {
            getAppliedExtraTimeLocal()
        }
    }
    
    /**
     * Get applied extra time from local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private fun getAppliedExtraTimeLocal(): Int {
        return preferencesManager?.getAppliedEarnedTimeToday() 
            ?: run {
                val lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, 0)
                if (isPastMidnight(lastResetDate)) {
                    prefs.edit().putInt(KEY_APPLIED_EXTRA_TIME, 0).apply()
                    return@run 0
                }
                prefs.getInt(KEY_APPLIED_EXTRA_TIME, 0)
            }
    }
    
    /**
     * Get applied extra time from Firebase
     */
    private suspend fun getAppliedExtraTimeFirebase(): Int {
        return try {
            val snapshot = getUserDocRef().get().await()
            snapshot.getLong(FIELD_APPLIED_EXTRA_TIME)?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error reading applied extra time from Firebase", e)
            0 // Fallback
        }
    }
    
    /**
     * Get wallet time (earned but not yet applied)
     * Uses Firebase if migrated, otherwise local storage
     */
    suspend fun getWalletTime(): Int {
        checkMidnightReset() // Check reset before returning
        return if (isMigratedToFirebase()) {
            getWalletTimeFirebase()
        } else {
            getWalletTimeLocal()
        }
    }
    
    /**
     * Get wallet time from local storage (backward compatibility)
     * Integrates with existing PreferencesManager wallet system
     */
    private fun getWalletTimeLocal(): Int {
        // Use existing PreferencesManager if available (for backward compatibility)
        return preferencesManager?.getTotalEarnedTimeMinutes() 
            ?: prefs.getInt(KEY_WALLET_TIME, 0)
    }
    
    /**
     * Get wallet time from Firebase
     */
    private suspend fun getWalletTimeFirebase(): Int {
        return try {
            val snapshot = getUserDocRef().get().await()
            snapshot.getLong(FIELD_WALLET_TIME)?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error reading wallet time from Firebase", e)
            0 // Fallback
        }
    }
    
    /**
     * Get effective daily watch time limit (base + applied)
     * Maximum: 180 minutes (3 hours)
     */
    suspend fun getEffectiveTimeLimit(): Int {
        val base = getBaseTime()
        val applied = getAppliedExtraTime()
        val total = base + applied
        return total.coerceAtMost(MAX_DAILY_TIME)
    }
    
    /**
     * Get time used today in milliseconds
     * Uses Firebase if migrated, otherwise local storage
     */
    suspend fun getTimeUsedToday(): Long {
        checkMidnightReset() // Check reset before returning
        return if (isMigratedToFirebase()) {
            getTimeUsedTodayFirebase()
        } else {
            getTimeUsedTodayLocal()
        }
    }
    
    /**
     * Get time used today from local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private fun getTimeUsedTodayLocal(): Long {
        return preferencesManager?.getTimeUsedToday() 
            ?: run {
                val lastResetDate = prefs.getLong(KEY_LAST_RESET_DATE, 0)
                if (isPastMidnight(lastResetDate)) {
                    resetTimeUsedTodayLocal()
                    return@run 0L
                }
                prefs.getLong(KEY_TIME_USED_TODAY, 0)
            }
    }
    
    /**
     * Get time used today from Firebase
     */
    private suspend fun getTimeUsedTodayFirebase(): Long {
        return try {
            val snapshot = getUserDocRef().get().await()
            snapshot.getLong(FIELD_TIME_USED_TODAY) ?: 0L
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error reading time used today from Firebase", e)
            0L // Fallback
        }
    }
    
    /**
     * Get time used today in minutes
     */
    suspend fun getTimeUsedTodayMinutes(): Int {
        return (getTimeUsedToday() / (60 * 1000)).toInt()
    }
    
    /**
     * Get remaining watch time in minutes
     */
    suspend fun getRemainingTime(): Int {
        val effectiveLimit = getEffectiveTimeLimit()
        val usedMinutes = getTimeUsedTodayMinutes()
        return (effectiveLimit - usedMinutes).coerceAtLeast(0)
    }
    
    // ==================== Wallet System ====================
    
    /**
     * Watch rewarded ad to earn 30 minutes to wallet
     * Updates Firebase if migrated, otherwise local storage
     * @return true if successfully added, false if wallet would exceed limit
     */
    suspend fun watchAdForWallet(): Boolean {
        checkMidnightReset()
        
        val currentWallet = getWalletTime()
        val currentApplied = getAppliedExtraTime()
        val base = getBaseTime()
        
        // Check if adding 30 minutes would exceed maximum possible wallet
        val maxPossibleWallet = MAX_DAILY_TIME - base - currentApplied
        val newWallet = currentWallet + WALLET_EARN_PER_AD
        
        if (newWallet > maxPossibleWallet) {
            Log.w("WatchTimeManager", "Cannot add to wallet: Would exceed maximum (current: $currentWallet, max: $maxPossibleWallet)")
            return false
        }
        
        if (isMigratedToFirebase()) {
            return watchAdForWalletFirebase(newWallet)
        } else {
            return watchAdForWalletLocal(newWallet)
        }
    }
    
    /**
     * Add to wallet in local storage (backward compatibility)
     * Integrates with existing PreferencesManager wallet system
     */
    private fun watchAdForWalletLocal(newWallet: Int): Boolean {
        // Use existing PreferencesManager if available (for backward compatibility)
        if (preferencesManager != null) {
            // Calculate how many 30-minute chunks to add
            val currentWallet = preferencesManager.getTotalEarnedTimeMinutes()
            val chunksToAdd = (newWallet - currentWallet) / WALLET_EARN_PER_AD
            repeat(chunksToAdd) {
                preferencesManager.addEarnedTimeToWallet()
            }
            Log.d("WatchTimeManager", "Added ${WALLET_EARN_PER_AD * chunksToAdd} minutes to wallet via PreferencesManager. New wallet: ${preferencesManager.getTotalEarnedTimeMinutes()} minutes (local)")
        } else {
            // Fallback to direct SharedPreferences
            prefs.edit().putInt(KEY_WALLET_TIME, newWallet).apply()
            Log.d("WatchTimeManager", "Added ${WALLET_EARN_PER_AD} minutes to wallet. New wallet: $newWallet minutes (local)")
        }
        return true
    }
    
    /**
     * Add to wallet in Firebase (atomic update)
     */
    private suspend fun watchAdForWalletFirebase(newWallet: Int): Boolean {
        return try {
            getUserDocRef().update(FIELD_WALLET_TIME, newWallet).await()
            Log.d("WatchTimeManager", "Added ${WALLET_EARN_PER_AD} minutes to wallet. New wallet: $newWallet minutes (Firebase)")
            true
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error adding to wallet in Firebase", e)
            false
        }
    }
    
    /**
     * Apply time from wallet to increase daily watch time
     * Updates Firebase if migrated, otherwise local storage
     * @param minutes Number of minutes to apply (must be available in wallet)
     * @return true if successfully applied, false if insufficient wallet or would exceed max
     */
    suspend fun applyWalletTime(minutes: Int): Boolean {
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
            val finalApplied = currentApplied + actualApplied
            
            if (isMigratedToFirebase()) {
                return applyWalletTimeFirebase(newWallet, finalApplied, actualApplied)
            } else {
                return applyWalletTimeLocal(newWallet, finalApplied, actualApplied)
            }
        }
        
        // Apply full amount
        val newWallet = currentWallet - minutes
        
        if (isMigratedToFirebase()) {
            return applyWalletTimeFirebase(newWallet, newApplied, minutes)
        } else {
            return applyWalletTimeLocal(newWallet, newApplied, minutes)
        }
    }
    
    /**
     * Apply wallet time in local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private fun applyWalletTimeLocal(newWallet: Int, newApplied: Int, actualApplied: Int): Boolean {
        if (preferencesManager != null) {
            // Use existing PreferencesManager
            val success = preferencesManager.applyEarnedTimeToDailyLimit(actualApplied)
            if (success) {
                Log.d("WatchTimeManager", "Applied $actualApplied minutes from wallet via PreferencesManager. New applied: $newApplied, New wallet: ${preferencesManager.getTotalEarnedTimeMinutes()} (local)")
                return true
            } else {
                Log.w("WatchTimeManager", "Failed to apply wallet time via PreferencesManager")
                return false
            }
        } else {
            // Fallback to direct SharedPreferences
            prefs.edit()
                .putInt(KEY_WALLET_TIME, newWallet)
                .putInt(KEY_APPLIED_EXTRA_TIME, newApplied)
                .apply()
            
            Log.d("WatchTimeManager", "Applied $actualApplied minutes from wallet. New applied: $newApplied, New wallet: $newWallet (local)")
            return true
        }
    }
    
    /**
     * Apply wallet time in Firebase (atomic transaction)
     */
    private suspend fun applyWalletTimeFirebase(newWallet: Int, newApplied: Int, actualApplied: Int): Boolean {
        return try {
            // Use transaction for atomic update
            firestore.runTransaction { transaction ->
                val userDocRef = getUserDocRef()
                val snapshot = transaction.get(userDocRef)
                
                // Double-check wallet has enough (prevent race conditions)
                val currentWallet = snapshot.getLong(FIELD_WALLET_TIME)?.toInt() ?: 0
                if (currentWallet < actualApplied) {
                    throw IllegalStateException("Insufficient wallet during transaction")
                }
                
                // Update both wallet and applied time atomically
                transaction.update(userDocRef, FIELD_WALLET_TIME, newWallet)
                transaction.update(userDocRef, FIELD_APPLIED_EXTRA_TIME, newApplied)
            }.await()
            
            Log.d("WatchTimeManager", "Applied $actualApplied minutes from wallet. New applied: $newApplied, New wallet: $newWallet (Firebase)")
            true
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error applying wallet time in Firebase", e)
            false
        }
    }
    
    // ==================== Reducing Watch Time ====================
    
    /**
     * Reduce daily watch time (reduces applied time, returns to wallet)
     * Updates Firebase if migrated, otherwise local storage
     * @param newEffectiveTime New effective time limit (base + applied)
     * @return true if successfully reduced, false if invalid
     */
    suspend fun reduceWatchTime(newEffectiveTime: Int): Boolean {
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
        
        if (isMigratedToFirebase()) {
            return reduceWatchTimeFirebase(newApplied, newWallet, timeToReturn)
        } else {
            return reduceWatchTimeLocal(newApplied, newWallet, timeToReturn)
        }
    }
    
    /**
     * Reduce watch time in local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private fun reduceWatchTimeLocal(newApplied: Int, newWallet: Int, timeToReturn: Int): Boolean {
        if (preferencesManager != null) {
            // Use existing PreferencesManager
            // First, reduce applied time
            preferencesManager.setAppliedEarnedTime(newApplied)
            // Then, add returned time to wallet
            preferencesManager.addMinutesToWallet(timeToReturn)
            
            Log.d("WatchTimeManager", "Reduced watch time via PreferencesManager: Returned $timeToReturn to wallet. New applied: $newApplied, New wallet: ${preferencesManager.getTotalEarnedTimeMinutes()} (local)")
            return true
        } else {
            // Fallback to direct SharedPreferences
            prefs.edit()
                .putInt(KEY_APPLIED_EXTRA_TIME, newApplied)
                .putInt(KEY_WALLET_TIME, newWallet)
                .apply()
            
            Log.d("WatchTimeManager", "Reduced watch time: Returned $timeToReturn to wallet. New applied: $newApplied, New wallet: $newWallet (local)")
            return true
        }
    }
    
    /**
     * Reduce watch time in Firebase (atomic transaction)
     */
    private suspend fun reduceWatchTimeFirebase(newApplied: Int, newWallet: Int, timeToReturn: Int): Boolean {
        return try {
            // Use transaction for atomic update
            firestore.runTransaction { transaction ->
                val userDocRef = getUserDocRef()
                
                // Update both applied time and wallet atomically
                transaction.update(userDocRef, FIELD_APPLIED_EXTRA_TIME, newApplied)
                transaction.update(userDocRef, FIELD_WALLET_TIME, newWallet)
            }.await()
            
            Log.d("WatchTimeManager", "Reduced watch time: Returned $timeToReturn to wallet. New applied: $newApplied, New wallet: $newWallet (Firebase)")
            true
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error reducing watch time in Firebase", e)
            false
        }
    }
    
    // ==================== Reset Watch Time (3 Consecutive Ads) ====================
    
    /**
     * Get current reset ad count (0-3)
     */
    suspend fun getResetAdCount(): Int {
        checkMidnightReset()
        return if (isMigratedToFirebase()) {
            getResetAdCountFirebase()
        } else {
            getResetAdCountLocal()
        }
    }
    
    /**
     * Get reset ad count from local storage (backward compatibility)
     */
    private fun getResetAdCountLocal(): Int {
        return prefs.getInt(KEY_RESET_AD_COUNT, 0)
    }
    
    /**
     * Get reset ad count from Firebase
     */
    private suspend fun getResetAdCountFirebase(): Int {
        return try {
            val snapshot = getUserDocRef().get().await()
            snapshot.getLong(FIELD_RESET_AD_COUNT)?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error reading reset ad count from Firebase", e)
            0 // Fallback
        }
    }
    
    /**
     * Get reset progress message (e.g., "Ads watched: 1/3")
     */
    suspend fun getResetProgressMessage(): String {
        val count = getResetAdCount()
        return "Ads watched: $count/$RESET_ADS_REQUIRED"
    }
    
    /**
     * Watch an ad for reset (must watch 3 consecutive ads)
     * Updates Firebase if migrated, otherwise local storage
     * @return ResetResult with success status and progress
     */
    suspend fun watchAdForReset(): ResetResult {
        checkMidnightReset()
        
        val currentCount = getResetAdCount()
        val startTime = if (isMigratedToFirebase()) {
            getResetAdStartTimeFirebase()
        } else {
            prefs.getLong(KEY_RESET_AD_START_TIME, 0)
        }
        val now = System.currentTimeMillis()
        
        // Check if reset sequence has timed out (more than 5 minutes between ads)
        if (currentCount > 0 && startTime > 0) {
            val timeSinceLastAd = now - startTime
            if (timeSinceLastAd > RESET_AD_TIMEOUT_MS) {
                // Timeout: Reset the count
                if (isMigratedToFirebase()) {
                    resetAdCountFirebase(0, 0)
                } else {
                    prefs.edit().putInt(KEY_RESET_AD_COUNT, 0).apply()
                }
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
            if (isMigratedToFirebase()) {
                resetAdCountFirebase(newCount, now)
            } else {
                prefs.edit()
                    .putInt(KEY_RESET_AD_COUNT, newCount)
                    .putLong(KEY_RESET_AD_START_TIME, now)
                    .apply()
            }
        } else {
            if (isMigratedToFirebase()) {
                resetAdCountFirebase(newCount, startTime)
            } else {
                prefs.edit().putInt(KEY_RESET_AD_COUNT, newCount).apply()
            }
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
     * Get reset ad start time from Firebase
     */
    private suspend fun getResetAdStartTimeFirebase(): Long {
        return try {
            val snapshot = getUserDocRef().get().await()
            snapshot.getLong(FIELD_RESET_AD_START_TIME) ?: 0L
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error reading reset ad start time from Firebase", e)
            0L // Fallback
        }
    }
    
    /**
     * Update reset ad count in Firebase
     */
    private suspend fun resetAdCountFirebase(count: Int, startTime: Long) {
        try {
            getUserDocRef().update(
                FIELD_RESET_AD_COUNT to count,
                FIELD_RESET_AD_START_TIME to startTime
            ).await()
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error updating reset ad count in Firebase", e)
        }
    }
    
    /**
     * Reset daily watch time to maximum (3 hours)
     * Called after 3 consecutive ads are watched
     * Updates Firebase if migrated, otherwise local storage
     * @return true if successfully reset
     */
    private suspend fun resetWatchTime(): Boolean {
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
            if (isMigratedToFirebase()) {
                resetWatchTimeFirebase(currentWallet, currentApplied, 0)
            } else {
                resetWatchTimeLocal(currentWallet, currentApplied, 0)
            }
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
        }
        
        // Successfully reset to maximum (or as close as possible)
        if (isMigratedToFirebase()) {
            return resetWatchTimeFirebase(newWallet, newApplied, remainingNeeded)
        } else {
            return resetWatchTimeLocal(newWallet, newApplied, remainingNeeded)
        }
    }
    
    /**
     * Reset watch time in local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private suspend fun resetWatchTimeLocal(newWallet: Int, newApplied: Int, remainingNeeded: Int): Boolean {
        if (preferencesManager != null) {
            // Use existing PreferencesManager reset logic
            val result = preferencesManager.resetDailyLimit()
            if (result.success) {
                // Update applied time if needed
                if (newApplied != preferencesManager.getAppliedEarnedTimeToday()) {
                    preferencesManager.setAppliedEarnedTime(newApplied)
                }
                // Wallet is already updated by resetDailyLimit()
                prefs.edit()
                    .putInt(KEY_RESET_AD_COUNT, 0)
                    .putLong(KEY_RESET_AD_START_TIME, 0)
                    .apply()
                
                Log.d("WatchTimeManager", "Reset complete via PreferencesManager: Effective time = ${getBaseTime() + newApplied} minutes (local)")
                return true
            } else {
                Log.w("WatchTimeManager", "Reset failed via PreferencesManager: ${result.message}")
                return false
            }
        } else {
            // Fallback to direct SharedPreferences
            prefs.edit()
                .putInt(KEY_WALLET_TIME, newWallet)
                .putInt(KEY_APPLIED_EXTRA_TIME, newApplied)
                .putLong(KEY_TIME_USED_TODAY, 0)
                .putInt(KEY_RESET_AD_COUNT, 0)
                .putLong(KEY_RESET_AD_START_TIME, 0)
                .apply()
            
            Log.d("WatchTimeManager", "Reset complete: Effective time = ${getBaseTime() + newApplied} minutes (local)")
            return true
        }
    }
    
    /**
     * Reset watch time in Firebase (atomic transaction)
     */
    private suspend fun resetWatchTimeFirebase(newWallet: Int, newApplied: Int, remainingNeeded: Int): Boolean {
        return try {
            // Use transaction for atomic update
            firestore.runTransaction { transaction ->
                val userDocRef = getUserDocRef()
                
                // Update all fields atomically
                transaction.update(userDocRef, FIELD_WALLET_TIME, newWallet)
                transaction.update(userDocRef, FIELD_APPLIED_EXTRA_TIME, newApplied)
                transaction.update(userDocRef, FIELD_TIME_USED_TODAY, 0L)
                transaction.update(userDocRef, FIELD_RESET_AD_COUNT, 0)
                transaction.update(userDocRef, FIELD_RESET_AD_START_TIME, 0L)
            }.await()
            
            Log.d("WatchTimeManager", "Reset complete: Effective time = ${getBaseTime() + newApplied} minutes (Firebase)")
            true
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error resetting watch time in Firebase", e)
            false
        }
    }
    
    /**
     * Cancel reset sequence (if user abandons reset)
     */
    suspend fun cancelReset(): Boolean {
        val currentCount = getResetAdCount()
        if (currentCount == 0) {
            return false // Nothing to cancel
        }
        
        if (isMigratedToFirebase()) {
            resetAdCountFirebase(0, 0)
        } else {
            prefs.edit()
                .putInt(KEY_RESET_AD_COUNT, 0)
                .putLong(KEY_RESET_AD_START_TIME, 0)
                .apply()
        }
        
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
     * Updates Firebase if migrated, otherwise local storage
     * @param startTime Start time from startTimer()
     */
    suspend fun stopTimer(startTime: Long) {
        checkMidnightReset()
        
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed <= 0) return
        
        val currentUsed = getTimeUsedToday()
        val newUsed = currentUsed + elapsed
        
        // Don't allow exceeding effective limit
        val effectiveLimitMs = getEffectiveTimeLimit() * 60 * 1000L
        val clampedUsed = newUsed.coerceAtMost(effectiveLimitMs)
        
        if (isMigratedToFirebase()) {
            addTimeUsedFirebase(clampedUsed)
        } else {
            addTimeUsedLocal(clampedUsed)
        }
        
        Log.d("WatchTimeManager", "Added ${elapsed / 1000}s to watch time. Total used: ${clampedUsed / 1000}s / ${effectiveLimitMs / 1000}s")
    }
    
    /**
     * Add time used in local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private fun addTimeUsedLocal(milliseconds: Long) {
        if (preferencesManager != null) {
            preferencesManager.addTimeUsed(milliseconds)
        } else {
            prefs.edit().putLong(KEY_TIME_USED_TODAY, milliseconds).apply()
        }
    }
    
    /**
     * Add time used in Firebase
     */
    private suspend fun addTimeUsedFirebase(milliseconds: Long) {
        try {
            getUserDocRef().update(FIELD_TIME_USED_TODAY, milliseconds).await()
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error updating time used in Firebase", e)
        }
    }
    
    /**
     * Add time directly (for incremental updates during playback)
     * Updates Firebase periodically if migrated (batched for performance)
     * @param milliseconds Time to add in milliseconds
     */
    suspend fun addTimeUsed(milliseconds: Long) {
        checkMidnightReset()
        
        if (milliseconds <= 0) return
        
        val currentUsed = getTimeUsedToday()
        val newUsed = currentUsed + milliseconds
        
        // Don't allow exceeding effective limit
        val effectiveLimitMs = getEffectiveTimeLimit() * 60 * 1000L
        val clampedUsed = newUsed.coerceAtMost(effectiveLimitMs)
        
        if (isMigratedToFirebase()) {
            addTimeUsedFirebase(clampedUsed)
        } else {
            addTimeUsedLocal(clampedUsed)
        }
    }
    
    /**
     * Check if watch time limit is exceeded
     */
    suspend fun isTimeLimitExceeded(): Boolean {
        val effectiveLimitMs = getEffectiveTimeLimit() * 60 * 1000L
        return getTimeUsedToday() >= effectiveLimitMs
    }
    
    /**
     * Get formatted time display: "Used today: HH:MM / HH:MM"
     */
    suspend fun getTimeDisplayString(): String {
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
     * Check if it's past midnight (12:00am) since last reset
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
     * Updates Firebase if migrated, otherwise local storage
     */
    suspend fun checkMidnightReset() {
        val lastResetDate = if (isMigratedToFirebase()) {
            getLastResetDateFirebase()
        } else {
            prefs.getLong(KEY_LAST_RESET_DATE, 0)
        }
        
        if (isPastMidnight(lastResetDate)) {
            // Reset all daily values
            if (isMigratedToFirebase()) {
                resetMidnightFirebase()
            } else {
                resetMidnightLocal()
            }
            
            Log.d("WatchTimeManager", "Midnight reset: All daily values reset to defaults")
        }
    }
    
    /**
     * Get last reset date from Firebase
     */
    private suspend fun getLastResetDateFirebase(): Long {
        return try {
            val snapshot = getUserDocRef().get().await()
            snapshot.getLong(FIELD_LAST_RESET_DATE) ?: 0L
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error reading last reset date from Firebase", e)
            0L // Fallback - will trigger reset
        }
    }
    
    /**
     * Reset at midnight in local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private fun resetMidnightLocal() {
        if (preferencesManager != null) {
            // Use existing PreferencesManager reset logic
            preferencesManager.resetTimeUsedToday()
            preferencesManager.clearAppliedEarnedTime()
            // Note: Wallet reset is handled by PreferencesManager's getTotalEarnedTimeMinutes()
            // which filters expired entries. Base time reset handled separately if needed.
            prefs.edit()
                .putInt(KEY_RESET_AD_COUNT, 0)
                .putLong(KEY_RESET_AD_START_TIME, 0)
                .putLong(KEY_LAST_RESET_DATE, System.currentTimeMillis())
                .apply()
        } else {
            // Fallback to direct SharedPreferences
            prefs.edit()
                .putLong(KEY_TIME_USED_TODAY, 0)
                .putInt(KEY_APPLIED_EXTRA_TIME, 0)
                .putInt(KEY_WALLET_TIME, 0)
                .putInt(KEY_BASE_TIME, DEFAULT_BASE_TIME) // Reset to default 60 minutes
                .putInt(KEY_RESET_AD_COUNT, 0)
                .putLong(KEY_RESET_AD_START_TIME, 0)
                .putLong(KEY_LAST_RESET_DATE, System.currentTimeMillis())
                .apply()
        }
    }
    
    /**
     * Reset at midnight in Firebase (atomic transaction)
     */
    private suspend fun resetMidnightFirebase() {
        try {
            // Use transaction for atomic update
            firestore.runTransaction { transaction ->
                val userDocRef = getUserDocRef()
                
                // Reset all daily values atomically
                transaction.update(userDocRef, FIELD_TIME_USED_TODAY, 0L)
                transaction.update(userDocRef, FIELD_APPLIED_EXTRA_TIME, 0)
                transaction.update(userDocRef, FIELD_WALLET_TIME, 0)
                transaction.update(userDocRef, FIELD_BASE_TIME, DEFAULT_BASE_TIME)
                transaction.update(userDocRef, FIELD_RESET_AD_COUNT, 0)
                transaction.update(userDocRef, FIELD_RESET_AD_START_TIME, 0L)
                transaction.update(userDocRef, FIELD_LAST_RESET_DATE, System.currentTimeMillis())
            }.await()
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error resetting at midnight in Firebase", e)
        }
    }
    
    /**
     * Reset time used today in local storage (backward compatibility)
     * Integrates with existing PreferencesManager
     */
    private fun resetTimeUsedTodayLocal() {
        if (preferencesManager != null) {
            preferencesManager.resetTimeUsedToday()
        } else {
            prefs.edit()
                .putLong(KEY_TIME_USED_TODAY, 0)
                .putLong(KEY_LAST_RESET_DATE, System.currentTimeMillis())
                .putInt(KEY_APPLIED_EXTRA_TIME, 0)
                .apply()
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
    suspend fun getStateString(): String {
        val base = getBaseTime()
        val applied = getAppliedExtraTime()
        val wallet = getWalletTime()
        val effective = getEffectiveTimeLimit()
        val used = getTimeUsedTodayMinutes()
        val remaining = getRemainingTime()
        val resetCount = getResetAdCount()
        val display = getTimeDisplayString()
        val isMigrated = isMigratedToFirebase()
        
        return """
            WatchTimeManager State (${if (isMigrated) "Firebase" else "Local"}):
            - Base Time: $base minutes
            - Applied Extra Time: $applied minutes
            - Wallet Time: $wallet minutes
            - Effective Limit: $effective minutes
            - Time Used: $used minutes
            - Remaining: $remaining minutes
            - Reset Ad Count: $resetCount/$RESET_ADS_REQUIRED
            - Display: $display
        """.trimIndent()
    }
    
    /**
     * Force reset (for testing/debugging only)
     */
    suspend fun forceReset() {
        if (isMigratedToFirebase()) {
            resetMidnightFirebase()
        } else {
            resetMidnightLocal()
        }
        Log.d("WatchTimeManager", "Force reset completed")
    }
}
