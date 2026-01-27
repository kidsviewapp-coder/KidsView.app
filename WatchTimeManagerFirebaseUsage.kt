package why.xee.kidsview.ui.example

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import why.xee.kidsview.data.preferences.WatchTimeManagerFirebase

/**
 * Example usage of WatchTimeManagerFirebase
 * 
 * This file demonstrates how to integrate WatchTimeManagerFirebase into your Android app
 * with backward compatibility for existing users.
 */

class WatchTimeManagerFirebaseUsage(private val context: Context) {
    
    private val watchTimeManager = WatchTimeManagerFirebase.getInstance(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    // ==================== Example 1: Check Migration Status ====================
    
    fun exampleCheckMigrationStatus() {
        coroutineScope.launch {
            val isMigrated = watchTimeManager.isMigratedToFirebase()
            if (isMigrated) {
                println("✅ User is using Firebase system (cloud sync enabled)")
            } else {
                println("📱 User is using local system (backward compatibility)")
            }
        }
    }
    
    // ==================== Example 2: Migrate User to Firebase ====================
    
    fun exampleMigrateToFirebase() {
        coroutineScope.launch {
            val success = watchTimeManager.migrateToFirebase()
            if (success) {
                println("✅ Migration successful! User data synced to Firebase")
            } else {
                println("❌ Migration failed. User will continue with local system")
            }
        }
    }
    
    // ==================== Example 3: Watch Ad for Wallet ====================
    
    fun exampleWatchAdForWallet() {
        coroutineScope.launch {
            // User watches a rewarded ad
            val success = watchTimeManager.watchAdForWallet()
            
            if (success) {
                val walletTime = watchTimeManager.getWalletTime()
                val isMigrated = watchTimeManager.isMigratedToFirebase()
                println("✅ Added 30 minutes to wallet. Current wallet: $walletTime minutes (${if (isMigrated) "Firebase" else "Local"})")
            } else {
                println("❌ Cannot add to wallet: Would exceed maximum limit")
            }
        }
    }
    
    // ==================== Example 4: Apply Wallet Time ====================
    
    fun exampleApplyWalletTime(minutes: Int) {
        coroutineScope.launch {
            // User wants to apply 30 minutes from wallet
            val success = watchTimeManager.applyWalletTime(minutes)
            
            if (success) {
                val effectiveLimit = watchTimeManager.getEffectiveTimeLimit()
                val walletTime = watchTimeManager.getWalletTime()
                val isMigrated = watchTimeManager.isMigratedToFirebase()
                println("✅ Applied $minutes minutes. Effective limit: $effectiveLimit minutes, Wallet: $walletTime minutes (${if (isMigrated) "Firebase" else "Local"})")
            } else {
                println("❌ Cannot apply: Insufficient wallet or would exceed maximum")
            }
        }
    }
    
    // ==================== Example 5: Reduce Watch Time ====================
    
    fun exampleReduceWatchTime(newEffectiveTime: Int) {
        coroutineScope.launch {
            // User reduces effective time from 120 to 90 minutes
            val success = watchTimeManager.reduceWatchTime(newEffectiveTime)
            
            if (success) {
                val effectiveLimit = watchTimeManager.getEffectiveTimeLimit()
                val walletTime = watchTimeManager.getWalletTime()
                val isMigrated = watchTimeManager.isMigratedToFirebase()
                println("✅ Reduced to $effectiveLimit minutes. Wallet: $walletTime minutes (${if (isMigrated) "Firebase" else "Local"})")
            } else {
                println("❌ Cannot reduce: Invalid time or no time to return")
            }
        }
    }
    
    // ==================== Example 6: Reset Watch Time (3 Ads) ====================
    
    fun exampleResetWatchTime() {
        coroutineScope.launch {
            // User watches first ad
            var result = watchTimeManager.watchAdForReset()
            println("Ad 1: ${result.message}")
            
            if (!result.isComplete) {
                // User watches second ad
                result = watchTimeManager.watchAdForReset()
                println("Ad 2: ${result.message}")
                
                if (!result.isComplete) {
                    // User watches third ad
                    result = watchTimeManager.watchAdForReset()
                    println("Ad 3: ${result.message}")
                    
                    if (result.isComplete) {
                        val isMigrated = watchTimeManager.isMigratedToFirebase()
                        println("🎉 Reset complete! Daily time is now at maximum (3 hours) (${if (isMigrated) "Firebase" else "Local"})")
                    }
                }
            }
        }
    }
    
    // ==================== Example 7: Timer During Video Playback ====================
    
    private var videoStartTime: Long = 0
    
    fun exampleStartVideo() {
        coroutineScope.launch {
            // Check if time limit is exceeded
            val isExceeded = watchTimeManager.isTimeLimitExceeded()
            if (isExceeded) {
                println("Cannot play: Time limit exceeded")
                return@launch
            }
            
            // Video starts playing
            videoStartTime = watchTimeManager.startTimer()
            println("▶️ Video started. Timer running...")
        }
    }
    
    fun exampleStopVideo() {
        coroutineScope.launch {
            // Video stops playing
            if (videoStartTime > 0) {
                watchTimeManager.stopTimer(videoStartTime)
                val display = watchTimeManager.getTimeDisplayString()
                println("⏹️ Video stopped. $display")
                videoStartTime = 0
            }
        }
    }
    
    fun exampleUpdateTimerDuringPlayback() {
        coroutineScope.launch {
            // Call this periodically (e.g., every 10 seconds) while video is playing
            if (videoStartTime > 0) {
                watchTimeManager.addTimeUsed(10000) // Add 10 seconds
                
                val remaining = watchTimeManager.getRemainingTime()
                if (remaining <= 0) {
                    println("⏰ Time limit reached! Video should pause.")
                    exampleStopVideo()
                }
            }
        }
    }
    
    // ==================== Example 8: Display Time Information ====================
    
    fun exampleDisplayTimeInfo() {
        coroutineScope.launch {
            val display = watchTimeManager.getTimeDisplayString()
            val remaining = watchTimeManager.getRemainingTime()
            val wallet = watchTimeManager.getWalletTime()
            val effective = watchTimeManager.getEffectiveTimeLimit()
            val isMigrated = watchTimeManager.isMigratedToFirebase()
            
            println("📊 Time Information (${if (isMigrated) "Firebase" else "Local"}):")
            println("   $display")
            println("   Remaining: $remaining minutes")
            println("   Wallet: $wallet minutes")
            println("   Effective Limit: $effective minutes")
        }
    }
    
    // ==================== Example 9: Integration with Video Player ====================
    
    /**
     * Example integration with a video player
     */
    class VideoPlayerExample(private val context: Context) {
        private val watchTimeManager = WatchTimeManagerFirebase.getInstance(context)
        private var videoStartTime: Long = 0
        private var isPlaying = false
        private val coroutineScope = CoroutineScope(Dispatchers.Main)
        
        suspend fun onVideoPlay() {
            // Check if time limit is exceeded
            val isExceeded = watchTimeManager.isTimeLimitExceeded()
            if (isExceeded) {
                println("Cannot play: Time limit exceeded")
                return
            }
            
            // Start timer
            videoStartTime = watchTimeManager.startTimer()
            isPlaying = true
            
            // Start periodic updates (every 10 seconds)
            coroutineScope.launch {
                while (isPlaying) {
                    kotlinx.coroutines.delay(10000) // Every 10 seconds
                    watchTimeManager.addTimeUsed(10000)
                    
                    if (watchTimeManager.isTimeLimitExceeded()) {
                        onVideoPause()
                        // Show message to user
                        break
                    }
                }
            }
        }
        
        suspend fun onVideoPause() {
            if (isPlaying && videoStartTime > 0) {
                watchTimeManager.stopTimer(videoStartTime)
                isPlaying = false
                videoStartTime = 0
            }
        }
        
        suspend fun onVideoStop() {
            onVideoPause()
        }
        
        suspend fun getTimeDisplay(): String {
            return watchTimeManager.getTimeDisplayString()
        }
        
        suspend fun getRemainingTime(): Int {
            return watchTimeManager.getRemainingTime()
        }
    }
    
    // ==================== Example 10: Real-time Firebase Sync ====================
    
    /**
     * Example: Listen to Firebase changes in real-time
     * This allows syncing across multiple devices
     */
    fun exampleListenToFirebaseChanges() {
        coroutineScope.launch {
            val isMigrated = watchTimeManager.isMigratedToFirebase()
            if (!isMigrated) {
                println("User not migrated to Firebase. Real-time sync not available.")
                return@launch
            }
            
            // Get Firebase user ID
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                println("User not logged in. Cannot listen to Firebase changes.")
                return@launch
            }
            
            // Listen to user document changes
            com.google.firebase.firestore.ktx.firestore
                .collection("users")
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("Error listening to Firebase: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        val walletTime = snapshot.getLong("walletTime")?.toInt() ?: 0
                        val appliedTime = snapshot.getLong("appliedExtraTime")?.toInt() ?: 0
                        val timeUsed = snapshot.getLong("timeUsedToday") ?: 0L
                        
                        println("📡 Firebase update received:")
                        println("   Wallet: $walletTime minutes")
                        println("   Applied: $appliedTime minutes")
                        println("   Time Used: ${timeUsed / 1000 / 60} minutes")
                        
                        // Update UI with new values
                        // updateUI(walletTime, appliedTime, timeUsed)
                    }
                }
        }
    }
    
    // ==================== Example 11: Complete Flow with Migration ====================
    
    fun exampleCompleteFlowWithMigration() {
        coroutineScope.launch {
            println("=== Complete Watch Time Flow with Firebase ===\n")
            
            // 1. Check migration status
            println("1. Checking migration status...")
            val isMigrated = watchTimeManager.isMigratedToFirebase()
            println("   Status: ${if (isMigrated) "Firebase" else "Local"}\n")
            
            // 2. If not migrated, migrate user
            if (!isMigrated) {
                println("2. Migrating to Firebase...")
                val success = watchTimeManager.migrateToFirebase()
                if (success) {
                    println("   ✅ Migration successful!\n")
                } else {
                    println("   ❌ Migration failed. Continuing with local system.\n")
                }
            }
            
            // 3. Check current state
            println("3. Initial State:")
            exampleDisplayTimeInfo()
            println()
            
            // 4. Watch ad to earn wallet time
            println("4. Watching ad for wallet...")
            exampleWatchAdForWallet()
            kotlinx.coroutines.delay(500) // Wait for async operation
            exampleDisplayTimeInfo()
            println()
            
            // 5. Apply wallet time
            println("5. Applying 30 minutes from wallet...")
            exampleApplyWalletTime(30)
            kotlinx.coroutines.delay(500)
            exampleDisplayTimeInfo()
            println()
            
            // 6. Start watching video
            println("6. Starting video...")
            exampleStartVideo()
            kotlinx.coroutines.delay(500)
            
            // Simulate watching for 15 minutes
            kotlinx.coroutines.delay(100) // In real app, this would be actual video playback
            watchTimeManager.addTimeUsed(15 * 60 * 1000L)
            exampleDisplayTimeInfo()
            println()
            
            // 7. Stop video
            println("7. Stopping video...")
            exampleStopVideo()
            kotlinx.coroutines.delay(500)
            println()
            
            // 8. Reduce watch time
            println("8. Reducing watch time to 60 minutes...")
            exampleReduceWatchTime(60)
            kotlinx.coroutines.delay(500)
            exampleDisplayTimeInfo()
            println()
            
            // 9. Reset watch time (3 ads)
            println("9. Resetting watch time (requires 3 ads)...")
            exampleResetWatchTime()
            kotlinx.coroutines.delay(500)
            exampleDisplayTimeInfo()
        }
    }
}
