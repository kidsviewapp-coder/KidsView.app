package why.xee.kidsview.ui.example

import android.content.Context
import why.xee.kidsview.data.preferences.WatchTimeManager

/**
 * Example usage of WatchTimeManager
 * 
 * This file demonstrates how to integrate WatchTimeManager into your Android app
 */

class WatchTimeManagerUsageExample(private val context: Context) {
    
    private val watchTimeManager = WatchTimeManager.getInstance(context)
    
    // ==================== Example 1: Watch Ad for Wallet ====================
    
    fun exampleWatchAdForWallet() {
        // User watches a rewarded ad
        val success = watchTimeManager.watchAdForWallet()
        
        if (success) {
            val walletTime = watchTimeManager.getWalletTime()
            println("✅ Added 30 minutes to wallet. Current wallet: $walletTime minutes")
        } else {
            println("❌ Cannot add to wallet: Would exceed maximum limit")
        }
    }
    
    // ==================== Example 2: Apply Wallet Time ====================
    
    fun exampleApplyWalletTime(minutes: Int) {
        // User wants to apply 30 minutes from wallet
        val success = watchTimeManager.applyWalletTime(minutes)
        
        if (success) {
            val effectiveLimit = watchTimeManager.getEffectiveTimeLimit()
            val walletTime = watchTimeManager.getWalletTime()
            println("✅ Applied $minutes minutes. Effective limit: $effectiveLimit minutes, Wallet: $walletTime minutes")
        } else {
            println("❌ Cannot apply: Insufficient wallet or would exceed maximum")
        }
    }
    
    // ==================== Example 3: Reduce Watch Time ====================
    
    fun exampleReduceWatchTime(newEffectiveTime: Int) {
        // User reduces effective time from 120 to 90 minutes
        val success = watchTimeManager.reduceWatchTime(newEffectiveTime)
        
        if (success) {
            val effectiveLimit = watchTimeManager.getEffectiveTimeLimit()
            val walletTime = watchTimeManager.getWalletTime()
            println("✅ Reduced to $effectiveLimit minutes. Wallet: $walletTime minutes")
        } else {
            println("❌ Cannot reduce: Invalid time or no time to return")
        }
    }
    
    // ==================== Example 4: Reset Watch Time (3 Ads) ====================
    
    fun exampleResetWatchTime() {
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
                    println("🎉 Reset complete! Daily time is now at maximum (3 hours)")
                }
            }
        }
    }
    
    // ==================== Example 5: Timer During Video Playback ====================
    
    private var videoStartTime: Long = 0
    
    fun exampleStartVideo() {
        // Video starts playing
        videoStartTime = watchTimeManager.startTimer()
        println("▶️ Video started. Timer running...")
    }
    
    fun exampleStopVideo() {
        // Video stops playing
        if (videoStartTime > 0) {
            watchTimeManager.stopTimer(videoStartTime)
            val display = watchTimeManager.getTimeDisplayString()
            println("⏹️ Video stopped. $display")
            videoStartTime = 0
        }
    }
    
    fun exampleUpdateTimerDuringPlayback() {
        // Call this periodically (e.g., every 10 seconds) while video is playing
        if (videoStartTime > 0) {
            val elapsed = System.currentTimeMillis() - videoStartTime
            watchTimeManager.addTimeUsed(10000) // Add 10 seconds
            
            val remaining = watchTimeManager.getRemainingTime()
            if (remaining <= 0) {
                println("⏰ Time limit reached! Video should pause.")
                exampleStopVideo()
            }
        }
    }
    
    // ==================== Example 6: Display Time Information ====================
    
    fun exampleDisplayTimeInfo() {
        val display = watchTimeManager.getTimeDisplayString()
        val remaining = watchTimeManager.getRemainingTime()
        val wallet = watchTimeManager.getWalletTime()
        val effective = watchTimeManager.getEffectiveTimeLimit()
        
        println("📊 Time Information:")
        println("   $display")
        println("   Remaining: $remaining minutes")
        println("   Wallet: $wallet minutes")
        println("   Effective Limit: $effective minutes")
    }
    
    // ==================== Example 7: Check Time Limit ====================
    
    fun exampleCheckTimeLimit(): Boolean {
        val isExceeded = watchTimeManager.isTimeLimitExceeded()
        
        if (isExceeded) {
            println("⛔ Time limit exceeded! Cannot watch more videos today.")
            return false
        } else {
            val remaining = watchTimeManager.getRemainingTime()
            println("✅ Time available: $remaining minutes remaining")
            return true
        }
    }
    
    // ==================== Example 8: Integration with Video Player ====================
    
    /**
     * Example integration with a video player
     */
    class VideoPlayerExample(private val context: Context) {
        private val watchTimeManager = WatchTimeManager.getInstance(context)
        private var videoStartTime: Long = 0
        private var isPlaying = false
        
        fun onVideoPlay() {
            // Check if time limit is exceeded
            if (watchTimeManager.isTimeLimitExceeded()) {
                println("Cannot play: Time limit exceeded")
                return
            }
            
            // Start timer
            videoStartTime = watchTimeManager.startTimer()
            isPlaying = true
            
            // Start periodic updates (every 10 seconds)
            // In real app, use a coroutine or handler
            startPeriodicUpdates()
        }
        
        fun onVideoPause() {
            if (isPlaying && videoStartTime > 0) {
                watchTimeManager.stopTimer(videoStartTime)
                isPlaying = false
                videoStartTime = 0
            }
        }
        
        fun onVideoStop() {
            onVideoPause()
        }
        
        private fun startPeriodicUpdates() {
            // In real app, use:
            // - Coroutine with delay(10000)
            // - Handler.postDelayed()
            // - Timer.schedule()
            
            // Example with coroutine (requires coroutine scope):
            /*
            viewModelScope.launch {
                while (isPlaying) {
                    delay(10000) // Every 10 seconds
                    watchTimeManager.addTimeUsed(10000)
                    
                    if (watchTimeManager.isTimeLimitExceeded()) {
                        onVideoPause()
                        // Show message to user
                        break
                    }
                }
            }
            */
        }
        
        fun getTimeDisplay(): String {
            return watchTimeManager.getTimeDisplayString()
        }
        
        fun getRemainingTime(): Int {
            return watchTimeManager.getRemainingTime()
        }
    }
    
    // ==================== Example 9: Complete Flow ====================
    
    fun exampleCompleteFlow() {
        println("=== Complete Watch Time Flow ===\n")
        
        // 1. Check current state
        println("1. Initial State:")
        exampleDisplayTimeInfo()
        println()
        
        // 2. Watch ad to earn wallet time
        println("2. Watching ad for wallet...")
        exampleWatchAdForWallet()
        exampleDisplayTimeInfo()
        println()
        
        // 3. Apply wallet time
        println("3. Applying 30 minutes from wallet...")
        exampleApplyWalletTime(30)
        exampleDisplayTimeInfo()
        println()
        
        // 4. Start watching video
        println("4. Starting video...")
        exampleStartVideo()
        
        // Simulate watching for 15 minutes
        Thread.sleep(100) // In real app, this would be actual video playback
        watchTimeManager.addTimeUsed(15 * 60 * 1000L)
        exampleDisplayTimeInfo()
        println()
        
        // 5. Stop video
        println("5. Stopping video...")
        exampleStopVideo()
        println()
        
        // 6. Reduce watch time
        println("6. Reducing watch time to 60 minutes...")
        exampleReduceWatchTime(60)
        exampleDisplayTimeInfo()
        println()
        
        // 7. Reset watch time (3 ads)
        println("7. Resetting watch time (requires 3 ads)...")
        exampleResetWatchTime()
        exampleDisplayTimeInfo()
    }
}
