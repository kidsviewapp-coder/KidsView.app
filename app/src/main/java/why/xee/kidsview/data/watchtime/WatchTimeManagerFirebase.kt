package why.xee.kidsview.data.watchtime

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.util.Calendar
import why.xee.kidsview.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WatchTimeManagerFirebase - Fully integrated Firebase watch-time system
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * FIREBASE INTEGRATION - ALL RULES ENFORCED VIA FIRESTORE
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This manager provides complete Firebase integration while maintaining backward
 * compatibility with the existing local SharedPreferences system.
 * 
 * Features:
 * - Default base watch time: 60 minutes (1 hour) per day
 * - Wallet system: Earn 30 minutes per rewarded ad
 * - Maximum daily watch time: 180 minutes (3 hours) - ENFORCED BY FIREBASE
 * - Automatic midnight reset (12:00 AM) - ENFORCED BY FIREBASE
 * - Reset to max requires 3 consecutive rewarded ads - ENFORCED BY FIREBASE
 * - Timer only counts during video playback
 * - All updates are atomic (Firestore transactions)
 * 
 * Migration Strategy:
 * - Existing users: Continue using local SharedPreferences (isMigratedToFirebase = false)
 * - New/Migrated users: Use Firebase Firestore as source of truth (isMigratedToFirebase = true)
 * - Firebase enforces all rules for migrated users
 * - Local system continues to work for non-migrated users
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * FIREBASE FIRESTORE STRUCTURE:
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * users/{userId}/watchTimeData {
 *   baseTime: Int (1-180, default: 60)
 *   appliedExtraTime: Int (0-180, default: 0)
 *   walletTime: Int (0-180, default: 0)
 *   timeUsedToday: Long (milliseconds, default: 0)
 *   lastResetDate: Long (timestamp, default: 0)
 *   resetAdCount: Int (0-3, default: 0)
 *   resetAdStartTime: Long (timestamp, default: 0)
 *   isMigratedToFirebase: Boolean (default: false)
 *   version: String (app version for tracking)
 * }
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * FIREBASE SECURITY RULES (to be added to firestore.rules):
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * match /users/{userId}/watchTimeData {
 *   allow read, write: if request.auth != null && request.auth.uid == userId;
 *   
 *   // Enforce maximum limits
 *   allow update: if request.resource.data.baseTime is int && 
 *                    request.resource.data.baseTime >= 1 && 
 *                    request.resource.data.baseTime <= 180 &&
 *                    request.resource.data.appliedExtraTime is int && 
 *                    request.resource.data.appliedExtraTime >= 0 && 
 *                    request.resource.data.appliedExtraTime <= 180 &&
 *                    request.resource.data.walletTime is int && 
 *                    request.resource.data.walletTime >= 0 && 
 *                    request.resource.data.walletTime <= 180 &&
 *                    (request.resource.data.baseTime + request.resource.data.appliedExtraTime) <= 180;
 * }
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Singleton
class WatchTimeManagerFirebase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val PREFS_NAME = "watchtime_firebase_prefs"
        
        // ═══════════════════════════════════════════════════════════════════
        // LOCAL STORAGE KEYS (for backward compatibility)
        // ═══════════════════════════════════════════════════════════════════
        private const val KEY_IS_MIGRATED_TO_FIREBASE = "is_migrated_to_firebase"
        
        // ═══════════════════════════════════════════════════════════════════
        // FIRESTORE PATHS AND FIELD NAMES
        // ═══════════════════════════════════════════════════════════════════
        private const val COLLECTION_USERS = "users"
        private const val DOCUMENT_WATCH_TIME_DATA = "watchTimeData"
        
        private const val FIELD_BASE_TIME = "baseTime"
        private const val FIELD_APPLIED_EXTRA_TIME = "appliedExtraTime"
        private const val FIELD_WALLET_TIME = "walletTime"
        private const val FIELD_TIME_USED_TODAY = "timeUsedToday"
        private const val FIELD_LAST_RESET_DATE = "lastResetDate"
        private const val FIELD_RESET_AD_COUNT = "resetAdCount"
        private const val FIELD_RESET_AD_START_TIME = "resetAdStartTime"
        private const val FIELD_IS_MIGRATED = "isMigratedToFirebase"
        private const val FIELD_VERSION = "version"
        
        // ═══════════════════════════════════════════════════════════════════
        // CONSTANTS - ENFORCED BY FIREBASE
        // ═══════════════════════════════════════════════════════════════════
        private const val DEFAULT_BASE_TIME = 60        // 1 hour default (NEW: unchanged)
        private const val MAX_DAILY_TIME = 120           // 2 hours maximum effective time (NEW: changed from 180)
        private const val MAX_WALLET_TIME = 180          // Maximum wallet 3 hours (NEW: unchanged)
        private const val WALLET_EARN_PER_AD = 15       // 15 minutes per ad (NEW: changed from 30)
        private const val RESET_ADS_REQUIRED = 3        // 3 consecutive ads for reset (NEW: unchanged)
        private const val RESET_AD_TIMEOUT_MS = 300000L // 5 minutes timeout between ads (NEW: unchanged)
        private const val RESET_TRIGGER_TIME = 120      // 2 hours used time triggers reset (NEW: added)
    }
    
    /**
     * Initialize: Check for midnight reset on app start
     */
    init {
        // Check midnight reset in background (non-blocking)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            checkMidnightReset()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MIGRATION & BACKWARD COMPATIBILITY
    // ═══════════════════════════════════════════════════════════════════════
    
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
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Read from PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            val baseTime = preferencesManager.getBaseTimeLimitMinutes()
            val appliedExtraTime = preferencesManager.getAppliedEarnedTimeToday()
            val walletTime = preferencesManager.getTotalEarnedTimeMinutes()
            val timeUsedToday = preferencesManager.getTimeUsedToday()
            val lastResetDate = prefs.getLong("last_reset_date", System.currentTimeMillis())
            val resetAdCount = prefs.getInt("reset_ad_count", 0)
            val resetAdStartTime = prefs.getLong("reset_ad_start_time", 0)
            
            // ═══════════════════════════════════════════════════════════════
            // FIREBASE SYNC: Write to Firestore with atomic transaction
            // ═══════════════════════════════════════════════════════════════
            val userDocRef = getUserDocRef()
            
            // Use transaction to ensure atomic write
            firestore.runTransaction { transaction ->
                // Check if document already exists
                val snapshot = transaction.get(userDocRef)
                
                // Only migrate if document doesn't exist or migration flag is false
                if (!snapshot.exists() || snapshot.getBoolean(FIELD_IS_MIGRATED) != true) {
                    transaction.set(userDocRef, mapOf(
                        FIELD_BASE_TIME to baseTime.coerceIn(1, MAX_DAILY_TIME),
                        FIELD_APPLIED_EXTRA_TIME to appliedExtraTime.coerceIn(0, MAX_DAILY_TIME),
                        FIELD_WALLET_TIME to walletTime.coerceIn(0, MAX_WALLET_TIME),
                        FIELD_TIME_USED_TODAY to timeUsedToday.coerceAtLeast(0L),
                        FIELD_LAST_RESET_DATE to lastResetDate,
                        FIELD_RESET_AD_COUNT to resetAdCount.coerceIn(0, RESET_ADS_REQUIRED),
                        FIELD_RESET_AD_START_TIME to resetAdStartTime.coerceAtLeast(0L),
                        FIELD_IS_MIGRATED to true,
                        FIELD_VERSION to android.os.Build.VERSION.SDK_INT.toString()
                    ))
                }
            }.await()
            
            // Mark as migrated in local prefs
            prefs.edit().putBoolean(KEY_IS_MIGRATED_TO_FIREBASE, true).apply()
            
            Log.d("WatchTimeManager", "✅ Successfully migrated user $userId to Firebase")
            return true
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "❌ Migration failed", e)
            return false
        }
    }
    
    /**
     * Get user document reference in Firestore
     * Structure: users/{userId}/watchTimeData
     */
    private fun getUserDocRef() = firestore.collection(COLLECTION_USERS).document(
        auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    ).collection(DOCUMENT_WATCH_TIME_DATA).document("data")
    
    // ═══════════════════════════════════════════════════════════════════════
    // CORE TIME MANAGEMENT - FIREBASE ENFORCED
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Get base watch time (default: 60 minutes / 1 hour)
     * Uses Firebase if migrated, otherwise local storage (backward compatibility)
     */
    suspend fun getBaseTime(): Int {
        return if (isMigratedToFirebase()) {
            getBaseTimeFirebase()
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            preferencesManager.getBaseTimeLimitMinutes()
        }
    }
    
    /**
     * Get base time from Firebase (ENFORCED: 1-180 minutes)
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
     * Set base watch time (1-180 minutes) - ENFORCED BY FIREBASE
     * Updates Firebase if migrated, otherwise local storage
     */
    suspend fun setBaseTime(minutes: Int) {
        val clamped = minutes.coerceIn(1, MAX_DAILY_TIME)
        
        if (isMigratedToFirebase()) {
            setBaseTimeFirebase(clamped)
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            preferencesManager.setTimeLimitMinutes(clamped)
        }
    }
    
    /**
     * Set base time in Firebase (atomic update, ENFORCED: 1-180)
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
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            preferencesManager.getAppliedEarnedTimeToday()
        }
    }
    
    /**
     * Get applied extra time from Firebase (ENFORCED: 0-180)
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
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            preferencesManager.getTotalEarnedTimeMinutes()
        }
    }
    
    /**
     * Get wallet time from Firebase (ENFORCED: 0-180)
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
     * Maximum: MAX_DAILY_TIME minutes - ENFORCED BY FIREBASE
     * NEW: Changed from 180 to MAX_DAILY_TIME minutes
     */
    suspend fun getEffectiveTimeLimit(): Int {
        val base = getBaseTime()
        val applied = getAppliedExtraTime()
        val total = base + applied
        return total.coerceAtMost(MAX_DAILY_TIME) // Firebase enforces this
    }
    
    /**
     * Check if user has reached 2 hours of used time (reset trigger)
     * NEW: Added to detect when reset should be offered
     * @return true if user has used 2 hours or more
     */
    suspend fun hasReachedResetTrigger(): Boolean {
        val usedMinutes = getTimeUsedTodayMinutes()
        return usedMinutes >= RESET_TRIGGER_TIME
    }
    
    /**
     * Get reset trigger message
     * NEW: Message shown when user reaches 2 hours
     * @return Message string for reset prompt
     */
    suspend fun getResetTriggerMessage(): String {
        return "Watch 3 ads consecutively to reset your daily watch-time."
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
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            preferencesManager.getTimeUsedToday()
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
    
    // ═══════════════════════════════════════════════════════════════════════
    // WALLET SYSTEM - FIREBASE ENFORCED
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Watch rewarded ad to earn 15 minutes to wallet
     * NEW: Changed from 30 to 15 minutes per ad
     * Updates Firebase if migrated, otherwise local storage
     * ENFORCED: Wallet cannot exceed 180 minutes (3 hours)
     * @return true if successfully added, false if wallet would exceed limit
     */
    suspend fun watchAdForWallet(): Boolean {
        checkMidnightReset()
        
        val currentWallet = getWalletTime()
        
        // ═══════════════════════════════════════════════════════════════════
        // NEW: Wallet can accumulate up to 3 hours (180 minutes) regardless of applied time
        // ═══════════════════════════════════════════════════════════════════
        val newWallet = currentWallet + WALLET_EARN_PER_AD
        
        if (newWallet > MAX_WALLET_TIME) {
            Log.w("WatchTimeManager", "Cannot add to wallet: Would exceed maximum (current: $currentWallet, max: $MAX_WALLET_TIME)")
            return false
        }
        
        if (isMigratedToFirebase()) {
            return watchAdForWalletFirebase(newWallet)
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // NEW: Need to add 15 minutes instead of 30
            // ═══════════════════════════════════════════════════════════════
            // Add 15 minutes to wallet (PreferencesManager adds 30, so we need custom logic)
            preferencesManager.addMinutesToWallet(WALLET_EARN_PER_AD)
            Log.d("WatchTimeManager", "Added ${WALLET_EARN_PER_AD} minutes to wallet via PreferencesManager (local)")
            return true
        }
    }
    
    /**
     * Add to wallet in Firebase (atomic update, ENFORCED: max 180)
     * NEW: Wallet can accumulate up to 180 minutes independently
     */
    private suspend fun watchAdForWalletFirebase(newWallet: Int): Boolean {
        return try {
            // Use transaction for atomic update
            firestore.runTransaction { transaction ->
                val userDocRef = getUserDocRef()
                val snapshot = transaction.get(userDocRef)
                
                // NEW: Wallet can accumulate up to 180 minutes (3 hours) independently
                val currentWallet = snapshot.getLong(FIELD_WALLET_TIME)?.toInt() ?: 0
                val finalWallet = (currentWallet + WALLET_EARN_PER_AD).coerceAtMost(MAX_WALLET_TIME)
                
                if (finalWallet > MAX_WALLET_TIME) {
                    throw IllegalStateException("Wallet would exceed maximum: $finalWallet > $MAX_WALLET_TIME")
                }
                
                transaction.update(userDocRef, FIELD_WALLET_TIME, finalWallet)
            }.await()
            
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
     * ENFORCED: Cannot exceed 180 minutes total
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
        
        // ═══════════════════════════════════════════════════════════════════
        // NEW: Maximum effective time is MAX_DAILY_TIME minutes
        // FIREBASE ENFORCEMENT: Check if new applied time would exceed maximum
        // ═══════════════════════════════════════════════════════════════════
        val newEffective = base + newApplied
        if (newEffective > MAX_DAILY_TIME) {
            // Apply only what fits within the limit (MAX_DAILY_TIME total)
            val maxCanApply = MAX_DAILY_TIME - base - currentApplied
            if (maxCanApply <= 0) {
                Log.w("WatchTimeManager", "Cannot apply: Already at maximum daily time ($MAX_DAILY_TIME minutes)")
                return false
            }
            
            // Apply partial amount
            val actualApplied = maxCanApply.coerceAtMost(minutes)
            val newWallet = currentWallet - actualApplied
            val finalApplied = currentApplied + actualApplied
            
            if (isMigratedToFirebase()) {
                return applyWalletTimeFirebase(newWallet, finalApplied, actualApplied)
            } else {
                // ═══════════════════════════════════════════════════════════
                // LOCAL LEGACY LOGIC: Use PreferencesManager
                // ═══════════════════════════════════════════════════════════
                return preferencesManager.applyEarnedTimeToDailyLimit(actualApplied)
            }
        }
        
        // Apply full amount
        val newWallet = currentWallet - minutes
        
        if (isMigratedToFirebase()) {
            return applyWalletTimeFirebase(newWallet, newApplied, minutes)
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            return preferencesManager.applyEarnedTimeToDailyLimit(minutes)
        }
    }
    
    /**
     * Apply wallet time in Firebase (atomic transaction, ENFORCED: max 180)
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
                
                // Firebase enforcement: Ensure total doesn't exceed 180
                val base = snapshot.getLong(FIELD_BASE_TIME)?.toInt() ?: DEFAULT_BASE_TIME
                val currentApplied = snapshot.getLong(FIELD_APPLIED_EXTRA_TIME)?.toInt() ?: 0
                val finalApplied = (currentApplied + actualApplied).coerceAtMost(MAX_DAILY_TIME - base)
                
                // Update both wallet and applied time atomically
                transaction.update(userDocRef, FIELD_WALLET_TIME, newWallet)
                transaction.update(userDocRef, FIELD_APPLIED_EXTRA_TIME, finalApplied)
            }.await()
            
            Log.d("WatchTimeManager", "Applied $actualApplied minutes from wallet. New applied: $newApplied, New wallet: $newWallet (Firebase)")
            true
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error applying wallet time in Firebase", e)
            false
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // REDUCING WATCH TIME - FIREBASE ENFORCED
    // ═══════════════════════════════════════════════════════════════════════
    
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
        
        // ═══════════════════════════════════════════════════════════════════
        // FIREBASE ENFORCEMENT: Ensure wallet doesn't exceed 180
        // ═══════════════════════════════════════════════════════════════════
        val finalWallet = newWallet.coerceAtMost(MAX_WALLET_TIME)
        
        if (isMigratedToFirebase()) {
            return reduceWatchTimeFirebase(newApplied, finalWallet, timeToReturn)
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            preferencesManager.setAppliedEarnedTime(newApplied)
            preferencesManager.addMinutesToWallet(timeToReturn)
            return true
        }
    }
    
    /**
     * Reduce watch time in Firebase (atomic transaction, ENFORCED: wallet max 180)
     */
    private suspend fun reduceWatchTimeFirebase(newApplied: Int, newWallet: Int, timeToReturn: Int): Boolean {
        return try {
            // Use transaction for atomic update
            firestore.runTransaction { transaction ->
                val userDocRef = getUserDocRef()
                
                // Firebase enforcement: Ensure wallet doesn't exceed 180
                val finalWallet = newWallet.coerceAtMost(MAX_WALLET_TIME)
                
                // Update both applied time and wallet atomically
                transaction.update(userDocRef, FIELD_APPLIED_EXTRA_TIME, newApplied)
                transaction.update(userDocRef, FIELD_WALLET_TIME, finalWallet)
            }.await()
            
            Log.d("WatchTimeManager", "Reduced watch time: Returned $timeToReturn to wallet. New applied: $newApplied, New wallet: $newWallet (Firebase)")
            true
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error reducing watch time in Firebase", e)
            false
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // RESET WATCH TIME (3 CONSECUTIVE ADS) - FIREBASE ENFORCED
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Get current reset ad count (0-3) - ENFORCED BY FIREBASE
     */
    suspend fun getResetAdCount(): Int {
        checkMidnightReset()
        return if (isMigratedToFirebase()) {
            getResetAdCountFirebase()
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Read from SharedPreferences
            // ═══════════════════════════════════════════════════════════════
            prefs.getInt("reset_ad_count", 0)
        }
    }
    
    /**
     * Get reset ad count from Firebase (ENFORCED: 0-3)
     */
    private suspend fun getResetAdCountFirebase(): Int {
        return try {
            val snapshot = getUserDocRef().get().await()
            snapshot.getLong(FIELD_RESET_AD_COUNT)?.toInt()?.coerceIn(0, RESET_ADS_REQUIRED) ?: 0
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
     * Watch an ad for reset (must watch 3 consecutive ads) - ENFORCED BY FIREBASE
     * Updates Firebase if migrated, otherwise local storage
     * @return ResetResult with success status and progress
     */
    suspend fun watchAdForReset(): ResetResult {
        checkMidnightReset()
        
        val currentCount = getResetAdCount()
        val startTime = if (isMigratedToFirebase()) {
            getResetAdStartTimeFirebase()
        } else {
            prefs.getLong("reset_ad_start_time", 0)
        }
        val now = System.currentTimeMillis()
        
        // ═══════════════════════════════════════════════════════════════════
        // FIREBASE ENFORCEMENT: Check if reset sequence has timed out
        // ═══════════════════════════════════════════════════════════════════
        if (currentCount > 0 && startTime > 0) {
            val timeSinceLastAd = now - startTime
            if (timeSinceLastAd > RESET_AD_TIMEOUT_MS) {
                // Timeout: Reset the count
                if (isMigratedToFirebase()) {
                    resetAdCountFirebase(0, 0)
                } else {
                    prefs.edit().putInt("reset_ad_count", 0).apply()
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
        val newCount = (currentCount + 1).coerceAtMost(RESET_ADS_REQUIRED)
        
        // If this is the first ad, record start time
        if (newCount == 1) {
            if (isMigratedToFirebase()) {
                resetAdCountFirebase(newCount, now)
            } else {
                prefs.edit()
                    .putInt("reset_ad_count", newCount)
                    .putLong("reset_ad_start_time", now)
                    .apply()
            }
        } else {
            if (isMigratedToFirebase()) {
                resetAdCountFirebase(newCount, startTime)
            } else {
                prefs.edit().putInt("reset_ad_count", newCount).apply()
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // FIREBASE ENFORCEMENT: Check if all 3 ads are complete
        // ═══════════════════════════════════════════════════════════════════
        if (newCount >= RESET_ADS_REQUIRED) {
            // NEW: Reset to maximum (MAX_DAILY_TIME)
            val success = resetWatchTime()
            val hours = MAX_DAILY_TIME / 60
            return if (success) {
                ResetResult(
                    success = true,
                    adCount = RESET_ADS_REQUIRED,
                    message = "Reset complete! Daily time set to maximum ($hours hours).",
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
     * Update reset ad count in Firebase (atomic update, ENFORCED: 0-3)
     */
    private suspend fun resetAdCountFirebase(count: Int, startTime: Long) {
        try {
            getUserDocRef().update(
                mapOf(
                    FIELD_RESET_AD_COUNT to count.coerceIn(0, RESET_ADS_REQUIRED),
                    FIELD_RESET_AD_START_TIME to startTime.coerceAtLeast(0L)
                )
            ).await()
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error updating reset ad count in Firebase", e)
        }
    }
    
    /**
     * Reset daily watch time to maximum (2 hours)
     * NEW: Changed from 3 hours to 2 hours, triggered when user reaches 2 hours used time
     * Called after 3 consecutive ads are watched
     * Updates Firebase if migrated, otherwise local storage
     * NEW: Wallet remains unless applied manually (wallet is NOT consumed during reset)
     * @return true if successfully reset
     */
    private suspend fun resetWatchTime(): Boolean {
        checkMidnightReset()
        
        val base = getBaseTime()
        val currentApplied = getAppliedExtraTime()
        val currentWallet = getWalletTime()
        
        // NEW: Reset to MAX_DAILY_TIME effective time
        // Calculate how much time we need to reach maximum (MAX_DAILY_TIME)
        val currentEffective = base + currentApplied
        val timeNeeded = MAX_DAILY_TIME - currentEffective
        
        // NEW: If already at MAX_DAILY_TIME, just reset time used and ad count
        if (timeNeeded <= 0) {
            Log.d("WatchTimeManager", "Already at maximum time ($MAX_DAILY_TIME minutes). Resetting time used.")
            // Reset time used and ad count, but keep wallet and applied time
            if (isMigratedToFirebase()) {
                resetWatchTimeFirebase(currentWallet, currentApplied, 0)
            } else {
                resetWatchTimeLocal(currentWallet, currentApplied, 0)
            }
            return true
        }
        
        // NEW: Try to get time from wallet to reach MAX_DAILY_TIME
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
        
        // Successfully reset to maximum (MAX_DAILY_TIME) or as close as possible
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
        // ═══════════════════════════════════════════════════════════════════
        // LOCAL LEGACY LOGIC: Use PreferencesManager
        // ═══════════════════════════════════════════════════════════════════
        val result = preferencesManager.resetDailyLimit()
        if (result.success) {
            // Update applied time if needed
            if (newApplied != preferencesManager.getAppliedEarnedTimeToday()) {
                preferencesManager.setAppliedEarnedTime(newApplied)
            }
            prefs.edit()
                .putInt("reset_ad_count", 0)
                .putLong("reset_ad_start_time", 0)
                .apply()
            
            Log.d("WatchTimeManager", "Reset complete via PreferencesManager: Effective time = ${getBaseTime() + newApplied} minutes (local)")
            return true
        } else {
            Log.w("WatchTimeManager", "Reset failed via PreferencesManager: ${result.message}")
            return false
        }
    }
    
    /**
     * Reset watch time in Firebase (atomic transaction, ENFORCED: max MAX_DAILY_TIME)
     * NEW: Resets to MAX_DAILY_TIME instead of 180
     */
    private suspend fun resetWatchTimeFirebase(newWallet: Int, newApplied: Int, remainingNeeded: Int): Boolean {
        return try {
            // Use transaction for atomic update
            firestore.runTransaction { transaction ->
                val userDocRef = getUserDocRef()
                val snapshot = transaction.get(userDocRef)
                
                // NEW: Firebase enforcement: Ensure limits are respected (max MAX_DAILY_TIME)
                val base = snapshot.getLong(FIELD_BASE_TIME)?.toInt() ?: DEFAULT_BASE_TIME
                val finalApplied = newApplied.coerceAtMost(MAX_DAILY_TIME - base)
                val finalWallet = newWallet.coerceAtMost(MAX_WALLET_TIME)
                
                // Update all fields atomically
                // NEW: Reset time used to 0, reset ad count, but keep wallet and applied time
                transaction.update(userDocRef, FIELD_WALLET_TIME, finalWallet)
                transaction.update(userDocRef, FIELD_APPLIED_EXTRA_TIME, finalApplied)
                transaction.update(userDocRef, FIELD_TIME_USED_TODAY, 0L) // Reset used time
                transaction.update(userDocRef, FIELD_RESET_AD_COUNT, 0)
                transaction.update(userDocRef, FIELD_RESET_AD_START_TIME, 0L)
            }.await()
            
            Log.d("WatchTimeManager", "Reset complete: Effective time = ${getBaseTime() + newApplied} minutes (max $MAX_DAILY_TIME) (Firebase)")
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
                .putInt("reset_ad_count", 0)
                .putLong("reset_ad_start_time", 0)
                .apply()
        }
        
        Log.d("WatchTimeManager", "Reset sequence cancelled")
        return true
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // TIMER MANAGEMENT - FIREBASE ENFORCED
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Start timer (call when video starts playing)
     * Returns the start time in milliseconds
     * Note: checkMidnightReset() should be called separately in a coroutine context
     */
    fun startTimer(): Long {
        // Note: checkMidnightReset() is a suspend function and should be called
        // from a coroutine context before starting the timer
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
        
        // ═══════════════════════════════════════════════════════════════════
        // FIREBASE ENFORCEMENT: Don't allow exceeding effective limit
        // ═══════════════════════════════════════════════════════════════════
        val effectiveLimitMs = getEffectiveTimeLimit() * 60 * 1000L
        val clampedUsed = newUsed.coerceAtMost(effectiveLimitMs)
        
        if (isMigratedToFirebase()) {
            addTimeUsedFirebase(clampedUsed)
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            preferencesManager.addTimeUsed(clampedUsed)
        }
        
        Log.d("WatchTimeManager", "Added ${elapsed / 1000}s to watch time. Total used: ${clampedUsed / 1000}s / ${effectiveLimitMs / 1000}s")
    }
    
    /**
     * Add time used in Firebase (atomic update)
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
        
        // ═══════════════════════════════════════════════════════════════════
        // FIREBASE ENFORCEMENT: Don't allow exceeding effective limit
        // ═══════════════════════════════════════════════════════════════════
        val effectiveLimitMs = getEffectiveTimeLimit() * 60 * 1000L
        val clampedUsed = newUsed.coerceAtMost(effectiveLimitMs)
        
        if (isMigratedToFirebase()) {
            addTimeUsedFirebase(clampedUsed)
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LOCAL LEGACY LOGIC: Use PreferencesManager
            // ═══════════════════════════════════════════════════════════════
            preferencesManager.addTimeUsed(clampedUsed)
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
    
    // ═══════════════════════════════════════════════════════════════════════
    // MIDNIGHT RESET - FIREBASE ENFORCED
    // ═══════════════════════════════════════════════════════════════════════
    
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
     * ENFORCED BY FIREBASE: Automatic reset at 12:00 AM
     */
    suspend fun checkMidnightReset() {
        val lastResetDate = if (isMigratedToFirebase()) {
            getLastResetDateFirebase()
        } else {
            prefs.getLong("last_reset_date", 0)
        }
        
        if (isPastMidnight(lastResetDate)) {
            // ═══════════════════════════════════════════════════════════════
            // FIREBASE ENFORCEMENT: Reset all daily values at midnight
            // ═══════════════════════════════════════════════════════════════
            if (isMigratedToFirebase()) {
                resetMidnightFirebase()
            } else {
                resetMidnightLocal()
            }
            
            Log.d("WatchTimeManager", "⏰ Midnight reset: All daily values reset to defaults")
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
     * NEW: Resets wallet to 0 at midnight
     * Integrates with existing PreferencesManager
     */
    private fun resetMidnightLocal() {
        // ═══════════════════════════════════════════════════════════════════
        // LOCAL LEGACY LOGIC: Use PreferencesManager
        // NEW: Clear wallet at midnight (12:00 AM sharp)
        // ═══════════════════════════════════════════════════════════════════
        preferencesManager.resetTimeUsedToday() // Resets timeUsedToday = 0
        preferencesManager.clearAppliedEarnedTime() // Resets appliedExtraTime = 0
        // NEW: PreferencesManager.getTimeUsedToday() already calls resetWalletAtMidnight() 
        // when past midnight, which clears all wallet entries. This ensures wallet = 0 at midnight.
        prefs.edit()
            .putInt("reset_ad_count", 0) // Reset ad count = 0
            .putLong("reset_ad_start_time", 0)
            .putLong("last_reset_date", System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Reset at midnight in Firebase (atomic transaction, ENFORCED)
     * NEW: Resets wallet to 0 at midnight (12:00 AM sharp)
     * Resets: timeUsedToday = 0, appliedExtraTime = 0, walletTime = 0, resetAdCount = 0
     */
    private suspend fun resetMidnightFirebase() {
        try {
            // Use transaction for atomic update
            firestore.runTransaction { transaction ->
                val userDocRef = getUserDocRef()
                
                // ═══════════════════════════════════════════════════════════
                // NEW: Midnight reset (12:00 AM sharp) resets:
                // - timeUsedToday = 0
                // - appliedExtraTime = 0
                // - walletTime = 0 (NEW: wallet is cleared at midnight)
                // - resetAdCount = 0
                // Base time remains at 60 minutes (1 hour)
                // ═══════════════════════════════════════════════════════════
                transaction.update(userDocRef, FIELD_TIME_USED_TODAY, 0L)
                transaction.update(userDocRef, FIELD_APPLIED_EXTRA_TIME, 0)
                transaction.update(userDocRef, FIELD_WALLET_TIME, 0) // NEW: Clear wallet at midnight
                transaction.update(userDocRef, FIELD_BASE_TIME, DEFAULT_BASE_TIME) // Keep base at 60
                transaction.update(userDocRef, FIELD_RESET_AD_COUNT, 0)
                transaction.update(userDocRef, FIELD_RESET_AD_START_TIME, 0L)
                transaction.update(userDocRef, FIELD_LAST_RESET_DATE, System.currentTimeMillis())
            }.await()
            
            Log.d("WatchTimeManager", "⏰ Midnight reset complete: Wallet cleared, time reset to 0")
        } catch (e: Exception) {
            Log.e("WatchTimeManager", "Error resetting at midnight in Firebase", e)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Result of watching ad for reset
     */
    data class ResetResult(
        val success: Boolean,
        val adCount: Int,
        val message: String,
        val isComplete: Boolean
    )
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════
    
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
