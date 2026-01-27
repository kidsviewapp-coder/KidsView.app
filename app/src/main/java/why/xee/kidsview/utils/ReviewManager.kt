package why.xee.kidsview.utils

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import why.xee.kidsview.data.preferences.PreferencesManager

/**
 * Manages Google Play In-App Review requests.
 * 
 * Criteria for showing review:
 * - User has launched app at least 3 times
 * - At least 3 days have passed since install
 * - User has performed meaningful actions (watched videos, used features)
 * 
 * Note: Google controls when the review popup actually appears (quota system).
 * Even if criteria are met, the popup may not show if quota is exceeded.
 */
object ReviewManager {
    
    private const val PREF_LAUNCH_COUNT = "review_launch_count"
    private const val PREF_FIRST_LAUNCH_TIME = "review_first_launch_time"
    private const val PREF_LAST_REVIEW_REQUEST = "review_last_request_time"
    private const val PREF_HAS_REQUESTED_REVIEW = "review_has_requested"
    
    // Criteria thresholds
    private const val MIN_LAUNCHES = 3
    private const val MIN_DAYS_SINCE_INSTALL = 3
    private const val DAYS_BETWEEN_REQUESTS = 90 // Don't request again for 90 days
    
    /**
     * Track app launch and check if review should be requested.
     * Should be called when parent mode is accessed (not from MainActivity).
     * Only tracks launches and requests reviews in parent mode.
     * 
     * @param activity The activity context
     * @param preferencesManager Preferences manager instance
     * @param isParentMode Whether the app is currently in parent mode
     */
    fun trackLaunchAndRequestReviewIfNeeded(activity: Activity, preferencesManager: PreferencesManager, isParentMode: Boolean = false) {
        // Only track launches and request reviews in parent mode
        if (!isParentMode) {
            AppLogger.d("ReviewManager: Skipping review request - not in parent mode")
            return
        }
        // Increment launch count
        val launchCount = preferencesManager.getInt(PREF_LAUNCH_COUNT, 0) + 1
        preferencesManager.putInt(PREF_LAUNCH_COUNT, launchCount)
        
        // Store first launch time if not set
        val firstLaunchTime = preferencesManager.getLong(PREF_FIRST_LAUNCH_TIME, 0L)
        if (firstLaunchTime == 0L) {
            preferencesManager.putLong(PREF_FIRST_LAUNCH_TIME, System.currentTimeMillis())
            return // Don't request on first launch
        }
        
        // Check if we should request review
        if (shouldRequestReview(preferencesManager)) {
            requestReview(activity, preferencesManager)
        }
    }
    
    /**
     * Check if review criteria are met.
     */
    private fun shouldRequestReview(preferencesManager: PreferencesManager): Boolean {
        // Get last request time once (used in multiple checks)
        val lastRequestTime = preferencesManager.getLong(PREF_LAST_REVIEW_REQUEST, 0L)
        
        // Check if already requested recently
        if (lastRequestTime > 0L) {
            val daysSinceLastRequest = (System.currentTimeMillis() - lastRequestTime) / (24 * 60 * 60 * 1000)
            if (daysSinceLastRequest < DAYS_BETWEEN_REQUESTS) {
                AppLogger.d("ReviewManager: Review requested recently, skipping")
                return false
            }
        }
        
        // Check launch count
        val launchCount = preferencesManager.getInt(PREF_LAUNCH_COUNT, 0)
        if (launchCount < MIN_LAUNCHES) {
            AppLogger.d("ReviewManager: Launch count ($launchCount) below threshold ($MIN_LAUNCHES)")
            return false
        }
        
        // Check days since install
        val firstLaunchTime = preferencesManager.getLong(PREF_FIRST_LAUNCH_TIME, 0L)
        if (firstLaunchTime == 0L) {
            return false
        }
        
        val daysSinceInstall = (System.currentTimeMillis() - firstLaunchTime) / (24 * 60 * 60 * 1000)
        if (daysSinceInstall < MIN_DAYS_SINCE_INSTALL) {
            AppLogger.d("ReviewManager: Days since install ($daysSinceInstall) below threshold ($MIN_DAYS_SINCE_INSTALL)")
            return false
        }
        
        // Check if already requested (one-time check)
        val hasRequested = preferencesManager.getBoolean(PREF_HAS_REQUESTED_REVIEW, false)
        if (hasRequested && lastRequestTime > 0L) {
            // Only request again if enough time has passed
            val daysSinceLastRequest = (System.currentTimeMillis() - lastRequestTime) / (24 * 60 * 60 * 1000)
            if (daysSinceLastRequest < DAYS_BETWEEN_REQUESTS) {
                AppLogger.d("ReviewManager: Already requested review, waiting for cooldown period")
                return false
            }
        }
        
        AppLogger.i("ReviewManager: Criteria met - launchCount=$launchCount, daysSinceInstall=$daysSinceInstall")
        return true
    }
    
    /**
     * Request review from Google Play.
     * Note: Google controls when the popup actually appears (quota system).
     */
    private fun requestReview(activity: Activity, preferencesManager: PreferencesManager) {
        try {
            val manager: ReviewManager = ReviewManagerFactory.create(activity)
            
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    
                    // Launch the review flow
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener { reviewTask ->
                        // Mark that we've requested review
                        preferencesManager.putLong(PREF_LAST_REVIEW_REQUEST, System.currentTimeMillis())
                        preferencesManager.putBoolean(PREF_HAS_REQUESTED_REVIEW, true)
                        
                        if (reviewTask.isSuccessful) {
                            AppLogger.i("ReviewManager: Review flow completed successfully")
                        } else {
                            // Log the error - Google Play controls when review can be shown
                            val exception = reviewTask.exception
                            AppLogger.w("ReviewManager: Review flow failed: ${exception?.message}")
                            // Note: ReviewErrorCode is not directly accessible from exception
                            // Google controls quota, so failures are expected sometimes
                        }
                    }
                } else {
                    AppLogger.e("ReviewManager: Failed to request review flow", task.exception)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("ReviewManager: Exception while requesting review", e)
        }
    }
    
    /**
     * Manually trigger review request (e.g., from settings or feedback button).
     * Bypasses criteria checks.
     */
    fun requestReviewManually(activity: Activity, preferencesManager: PreferencesManager) {
        requestReview(activity, preferencesManager)
    }
}

