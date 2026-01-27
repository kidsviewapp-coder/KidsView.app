package why.xee.kidsview.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.widget.FrameLayout
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.utils.AppLogger
import why.xee.kidsview.utils.Constants
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.firebase.auth.FirebaseAuth
import why.xee.kidsview.BuildConfig
import why.xee.kidsview.utils.ReviewerUnlockManager

/**
 * Banner Ad Component
 * Place this at the bottom of screens
 */
@Composable
fun BannerAd(
    adUnitId: String = Constants.ADMOB_BANNER_AD_ID,
    modifier: Modifier = Modifier,
    isParentMode: Boolean
) {
    // REVIEWER MODE: If reviewer mode is active, skip banner ad
    if (ReviewerUnlockManager.isUnlocked()) {
        AppLogger.d("REVIEWER MODE: Banner ad skipped - Reviewer Mode active")
        return
    }
    
    // Ads are disabled for alpha/testing builds - check BuildConfig.ADS_ENABLED
    if (!BuildConfig.ADS_ENABLED) {
        return
    }
    
    // Validate adUnitId is not empty
    if (adUnitId.isBlank()) {
        AppLogger.w("BannerAd: Ad Unit ID is blank - ad will not load")
        return
    }

    val context = LocalContext.current
    var retryDelay by remember { mutableStateOf(2000L) }
    var retryCount by remember { mutableStateOf(0) }
    val maxRetries = 3
    
    // Log when banner ad composable is created/recomposed
    LaunchedEffect(Unit) {
        AppLogger.w("📱 BannerAd composable initialized")
        AppLogger.w("📱 ADS_ENABLED: ${BuildConfig.ADS_ENABLED}")
        AppLogger.w("📱 Ad Unit ID: $adUnitId")
        AppLogger.w("📱 Is Parent Mode: $isParentMode")
        AppLogger.w("📱 MobileAds Initialized: ${why.xee.kidsview.utils.AdManager.getInstance().isMobileAdsInitialized()}")
        if (adUnitId.contains("3940256099942544")) {
            AppLogger.w("✅ Using TEST Banner Ad ID")
        } else {
            AppLogger.w("⚠️ Using PRODUCTION Banner Ad ID")
        }
    }

    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        AppLogger.w("✅ Banner ad loaded successfully (unit: $adUnitId)")
                        // Log if this is a test ad (AdMob will show "Test Ad" label)
                        AppLogger.w("✅ Banner ad loaded (check if 'Test Ad' label is visible)")
                        retryCount = 0
                        retryDelay = 2000L
                    }
                    
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        super.onAdFailedToLoad(error)
                        val errorMessage = error.message ?: ""
                        AppLogger.e("❌ Banner ad failed to load: $errorMessage (code: ${error.code}, unit: $adUnitId)")
                        
                        // AdMob logs the test device ID when ad fails to load
                        // This helps identify the device ID needed for test ads
                        AppLogger.w("💡 Tip: Check Logcat for test device ID when ad loads")
                        
                        // Handle JavascriptEngine error with retry backoff
                        val isJavascriptEngineError = errorMessage.contains("JavascriptEngine", ignoreCase = true) || 
                            errorMessage.contains("Unable to obtain", ignoreCase = true) ||
                            errorMessage.contains("Javascript", ignoreCase = true)
                        
                        AppLogger.w("🔍 Banner ad error check: isJavascriptEngineError=$isJavascriptEngineError, retryCount=$retryCount, maxRetries=$maxRetries")
                        
                        if (isJavascriptEngineError && retryCount < maxRetries) {
                            val currentRetry = retryCount + 1
                            AppLogger.w("⚠️ Banner ad JavascriptEngine error detected, will retry after ${retryDelay}ms (attempt $currentRetry/$maxRetries)")
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                AppLogger.w("🔄 Retrying banner ad load after ${retryDelay}ms delay (attempt $currentRetry/$maxRetries)")
                                // Increment retry count before retrying
                                retryCount = currentRetry
                                loadAd(AdRequest.Builder().build())
                            }, retryDelay)
                            // Increase delay for next retry (exponential backoff, max 30s)
                            retryDelay = (retryDelay * 2).coerceAtMost(30000L)
                        } else {
                            if (retryCount >= maxRetries) {
                                AppLogger.w("⚠️ Banner ad max retries ($maxRetries) reached, giving up")
                            }
                            // Reset for next error cycle
                            retryCount = 0
                            retryDelay = 2000L
                        }
                    }
                }
                // Wait for MobileAds initialization before loading
                if (why.xee.kidsview.utils.AdManager.getInstance().isMobileAdsInitialized()) {
                    // Build ad request with test device configuration for debug builds
                    val adRequest = if (BuildConfig.DEBUG) {
                        AdRequest.Builder().build() // Test device IDs are configured globally in AdManager.init()
                    } else {
                        AdRequest.Builder().build()
                    }
                    AppLogger.w("🔄 Loading banner ad with unit ID: $adUnitId")
                    if (adUnitId.contains("3940256099942544")) {
                        AppLogger.w("✅ Using TEST Banner Ad ID")
                    } else {
                        AppLogger.w("⚠️ Using PRODUCTION Banner Ad ID")
                    }
                    loadAd(adRequest)
                } else {
                    AppLogger.w("⚠️ Banner ad waiting for MobileAds initialization...")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (why.xee.kidsview.utils.AdManager.getInstance().isMobileAdsInitialized()) {
                            val adRequest = AdRequest.Builder().build()
                            AppLogger.w("🔄 Loading banner ad after initialization delay (unit: $adUnitId)")
                            loadAd(adRequest)
                        }
                    }, 2000L)
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        update = { view ->
            // Reload ad if adUnitId changes
            if (view.adUnitId != adUnitId) {
                view.adUnitId = adUnitId
                if (why.xee.kidsview.utils.AdManager.getInstance().isMobileAdsInitialized()) {
                    val adRequest = AdRequest.Builder().build()
                    AppLogger.w("🔄 Reloading banner ad with new unit ID: $adUnitId")
                    view.loadAd(adRequest)
                }
            }
        }
    )
}

/**
 * Interstitial Ad Manager
 * Call showInterstitialAd() before navigating to video player
 * Implements cooldown mechanism to comply with AdMob policies (minimum 60 seconds between ads)
 */
object AdManager {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInterstitialAdLoading: Boolean = false // Track loading state to prevent multiple loads
    private var isRewardedAdLoading: Boolean = false // Track loading state to prevent multiple loads
    private var preferencesManager: PreferencesManager? = null
    private var isMobileAdsInitialized: Boolean = false
    
    // Cooldown period: 60 seconds (AdMob policy requires reasonable spacing between ad impressions)
    private const val AD_COOLDOWN_MS = 60_000L // 60 seconds
    private const val PREFS_NAME = "admob_cooldown_prefs"
    private const val KEY_LAST_AD_SHOW_TIME = "last_interstitial_ad_time"
    
    // Retry delay for errors (exponential backoff)
    private var interstitialRetryDelay: Long = 2000L // Start with 2 seconds
    private var rewardedRetryDelay: Long = 2000L // Start with 2 seconds
    private const val MAX_RETRY_DELAY = 10000L // Max 10 seconds
    private const val MIN_INIT_DELAY = 2000L // Wait 2 seconds after MobileAds init
    
    /**
     * Initialize AdManager with PreferencesManager (fixes memory leak)
     * Should be called from MainActivity.onCreate()
     */
    fun init(preferencesManager: PreferencesManager) {
        this.preferencesManager = preferencesManager
    }
    
    /**
     * Mark MobileAds as initialized
     * Should be called after MobileAds.initialize() completes with delay
     */
    fun setMobileAdsInitialized() {
        isMobileAdsInitialized = true
        AppLogger.w("✅ AdManager: MobileAds marked as initialized - ads can now be loaded")
    }
    
    /**
     * Check if MobileAds is initialized
     */
    fun isMobileAdsInitialized(): Boolean {
        return isMobileAdsInitialized
    }
    
    /**
     * Get SharedPreferences for ad cooldown tracking
     */
    private fun getAdCooldownPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun canShowAd(context: Context): Boolean {
        val prefs = getAdCooldownPrefs(context)
        val lastAdTime = prefs.getLong(KEY_LAST_AD_SHOW_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastAdTime) >= AD_COOLDOWN_MS
    }
    
    /**
     * Record that an ad was shown (update last show time)
     */
    private fun recordAdShown(context: Context) {
        val prefs = getAdCooldownPrefs(context)
        prefs.edit().putLong(KEY_LAST_AD_SHOW_TIME, System.currentTimeMillis()).apply()
    }
    
    /**
     * Check if an interstitial ad is currently loaded and ready to show
     */
    fun isInterstitialAdReady(): Boolean {
        return interstitialAd != null
    }
    
    /**
     * Check if a rewarded ad is currently loaded and ready to show
     */
    fun isRewardedAdReady(): Boolean {
        return rewardedAd != null
    }
    
    /**
     * Check if a rewarded ad is currently loading
     */
    fun isRewardedAdCurrentlyLoading(): Boolean {
        return isRewardedAdLoading
    }
    
    /**
     * Get time remaining in cooldown period (in seconds)
     */
    fun getCooldownRemainingSeconds(context: Context): Long {
        val prefs = getAdCooldownPrefs(context)
        val lastAdTime = prefs.getLong(KEY_LAST_AD_SHOW_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastAdTime
        val remaining = AD_COOLDOWN_MS - elapsed
        return if (remaining > 0) remaining / 1000 else 0
    }

    /**
     * Load interstitial ad
     * Prevents multiple simultaneous loads
     * 
     * ADS DISABLED FOR ALPHA TESTING - Uncomment code below to re-enable after alpha testing
     */
    fun loadInterstitialAd(context: Context, isParentMode: Boolean) {
        // Ads are disabled for alpha testing - production IDs are kept but ads are not loaded
        return
        
        /* UNCOMMENT BELOW TO RE-ENABLE ADS AFTER ALPHA TESTING
        // Do not load ads in Kid Mode
        if (!isParentMode) return
        
        // Skip if already loaded
        if (interstitialAd != null) {
            AppLogger.w("ℹ️ Interstitial ad already loaded, skipping load")
            return
        }
        
        // Skip if already loading
        if (isInterstitialAdLoading) {
            AppLogger.w("ℹ️ Interstitial ad already loading, skipping duplicate load")
            return
        }
        
        // Wait for MobileAds initialization
        if (!isMobileAdsInitialized) {
            AppLogger.w("⚠️ MobileAds not initialized yet, waiting ${MIN_INIT_DELAY}ms...")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isParentMode && !isInterstitialAdLoading && interstitialAd == null) {
                    loadInterstitialAd(context, isParentMode)
                }
            }, MIN_INIT_DELAY)
            return
        }
        
        val adUnitId = Constants.ADMOB_INTERSTITIAL_AD_ID
        if (adUnitId.isBlank()) {
            AppLogger.w("⚠️ loadInterstitialAd: Ad Unit ID is blank - ad will not load")
            return
        }
        
        AppLogger.w("🔄 Loading interstitial ad (unit: $adUnitId)...")
        isInterstitialAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isInterstitialAdLoading = false
                    interstitialAd = ad
                    interstitialRetryDelay = 2000L // Reset retry delay on success
                    AppLogger.w("✅ Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isInterstitialAdLoading = false
                    interstitialAd = null
                    AppLogger.e("❌ Interstitial ad failed to load: ${error.message} (code: ${error.code}, domain: ${error.domain})")
                    
                    // Handle JavascriptEngine error with retry backoff
                    val errorMessage = error.message ?: ""
                    AppLogger.w("🔍 Checking error message: '$errorMessage'")
                    val isJavascriptEngineError = errorMessage.contains("JavascriptEngine", ignoreCase = true) || 
                        errorMessage.contains("Unable to obtain", ignoreCase = true)
                    
                    if (isJavascriptEngineError) {
                        AppLogger.w("⚠️ JavascriptEngine error detected, will retry after ${interstitialRetryDelay}ms")
                        // Schedule retry with exponential backoff
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            AppLogger.w("🔄 Retrying interstitial ad load after ${interstitialRetryDelay}ms delay")
                            if (isParentMode && !isInterstitialAdLoading && interstitialAd == null) {
                                loadInterstitialAd(context, isParentMode)
                            } else {
                                AppLogger.w("⚠️ Skipping retry - isParentMode=$isParentMode, isLoading=$isInterstitialAdLoading, ad=${interstitialAd != null}")
                            }
                        }, interstitialRetryDelay)
                        // Increase delay for next retry (exponential backoff, max 30s)
                        interstitialRetryDelay = (interstitialRetryDelay * 2).coerceAtMost(MAX_RETRY_DELAY)
                    } else {
                        // Reset retry delay for non-JavascriptEngine errors
                        interstitialRetryDelay = 2000L
                        AppLogger.w("ℹ️ Non-JavascriptEngine error, retry delay reset to 2000ms")
                    }
                }
            }
        )
        */
    }

    /**
     * Show interstitial ad if loaded and cooldown period has passed
     * @param context Context for accessing SharedPreferences
     * @param forceShow If true, bypass cooldown check (use sparingly)
     * @param onAdDismissed Callback when ad is dismissed or failed
     * 
     * ADS DISABLED FOR ALPHA TESTING - Uncomment code below to re-enable after alpha testing
     */
    fun showInterstitialAd(
        context: Context,
        isParentMode: Boolean,
        forceShow: Boolean = false,
        onAdDismissed: () -> Unit = {}
    ) {
        // REVIEWER MODE: If reviewer mode is active, skip ad and continue immediately
        if (ReviewerUnlockManager.isUnlocked()) {
            AppLogger.d("REVIEWER MODE: Interstitial ad show skipped - Reviewer Mode active")
            onAdDismissed()
            return
        }
        
        // Ads are disabled for alpha testing - production IDs are kept but ads are not shown
        // Call callback immediately to continue app flow
        onAdDismissed()
        return
        
        /* UNCOMMENT BELOW TO RE-ENABLE ADS AFTER ALPHA TESTING
        // Do not show ads in Kid Mode
        if (!isParentMode) {
            onAdDismissed()
            return
        }
        if (!forceShow && !canShowAd(context)) {
            val cooldownRemaining = getCooldownRemainingSeconds(context)
            AppLogger.w("⏱️ Interstitial ad in cooldown: ${cooldownRemaining}s remaining")
            onAdDismissed()
            return
        }
        
        if (interstitialAd == null) {
            AppLogger.w("⚠️ Interstitial ad not ready - loading now...")
            loadInterstitialAd(context, isParentMode = true)
            // Don't call onAdDismissed() immediately - let the caller know ad isn't ready
            // The ad will be loaded and can be shown on next attempt
            onAdDismissed()
            return
        }
        
        AppLogger.w("✅ Showing interstitial ad")
        
        interstitialAd?.let { ad ->
            // Record that we're about to show an ad
            recordAdShown(context)
            
            ad.fullScreenContentCallback =
                object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        loadInterstitialAd(context, isParentMode = true)
                        onAdDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        AppLogger.e("Interstitial ad failed to show: ${error.message}")
                        interstitialAd = null
                        loadInterstitialAd(context, isParentMode = true)
                        onAdDismissed()
                    }
                }
            val activity = context.findActivity()
            if (activity != null) {
                ad.show(activity)
            } else {
                AppLogger.e("Cannot find Activity to show ad")
                onAdDismissed()
            }
        } ?: run {
            onAdDismissed()
        }
        */
    }

    /**
     * Load rewarded ad
     * Prevents multiple simultaneous loads
     * 
     * ADS DISABLED FOR ALPHA TESTING - Uncomment code below to re-enable after alpha testing
     */
    fun loadRewardedAd(context: Context, isParentMode: Boolean) {
        // Ads are disabled for alpha testing - production IDs are kept but ads are not loaded
        return
        
        /* UNCOMMENT BELOW TO RE-ENABLE ADS AFTER ALPHA TESTING
        if (!isParentMode) {
            return
        }
        
        if (rewardedAd != null) {
            AppLogger.w("ℹ️ Rewarded ad already loaded, skipping load")
            return
        }
        
        if (isRewardedAdLoading) {
            AppLogger.w("ℹ️ Rewarded ad already loading, skipping duplicate load")
            return
        }
        
        // Wait for MobileAds initialization
        if (!isMobileAdsInitialized) {
            AppLogger.w("⚠️ MobileAds not initialized yet, waiting ${MIN_INIT_DELAY}ms...")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isParentMode && !isRewardedAdLoading && rewardedAd == null) {
                    loadRewardedAd(context, isParentMode)
                }
            }, MIN_INIT_DELAY)
            return
        }
        
        val adUnitId = Constants.ADMOB_REWARDED_AD_ID
        if (adUnitId.isBlank()) {
            AppLogger.w("⚠️ loadRewardedAd: Ad Unit ID is blank - ad will not load")
            return
        }
        
        AppLogger.w("🔄 Loading rewarded ad (unit: $adUnitId)...")
        isRewardedAdLoading = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isRewardedAdLoading = false
                    rewardedAd = ad
                    rewardedRetryDelay = 2000L // Reset retry delay on success
                    AppLogger.w("✅ Rewarded ad loaded successfully")
                    
                    // Configure Server-Side Verification (SSV)
                    // Include Firebase Anonymous Auth UID as custom data for server verification
                    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                    val ssvOptions = ServerSideVerificationOptions.Builder()
                        .setCustomData(firebaseUid)
                        .build()
                    ad.setServerSideVerificationOptions(ssvOptions)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isRewardedAdLoading = false
                    rewardedAd = null
                    
                    val errorMessage = error.message ?: ""
                    val errorCode = error.code
                    
                    // Code 3 = "No fill" - This is NORMAL, not an error
                    // It just means AdMob doesn't have an ad available at this moment
                    if (errorCode == 3 || errorMessage.contains("No fill", ignoreCase = true)) {
                        AppLogger.w("ℹ️ Rewarded ad: No fill (code: $errorCode) - No ad available at this time. This is normal.")
                        // Reset retry delay for "No fill" - it's not an error, just no ad available
                        rewardedRetryDelay = 2000L
                        return
                    }
                    
                    // For actual errors, log them
                    AppLogger.e("❌ Rewarded ad failed to load: $errorMessage (code: $errorCode)")
                    // Only log cause if it's not null (avoid redundant "cause: null" logs)
                    if (error.cause != null) {
                        AppLogger.e("❌ Rewarded ad error cause: ${error.cause}")
                    }
                    
                    // Handle JavascriptEngine error with retry backoff
                    val isJavascriptEngineError = errorMessage.contains("JavascriptEngine", ignoreCase = true) || 
                        errorMessage.contains("Unable to obtain", ignoreCase = true)
                    
                    if (isJavascriptEngineError) {
                        AppLogger.w("⚠️ Rewarded ad JavascriptEngine error detected, will retry after ${rewardedRetryDelay}ms")
                        // Schedule retry with exponential backoff
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            AppLogger.w("🔄 Retrying rewarded ad load after ${rewardedRetryDelay}ms delay")
                            if (isParentMode && !isRewardedAdLoading && rewardedAd == null) {
                                loadRewardedAd(context, isParentMode)
                            } else {
                                AppLogger.w("⚠️ Skipping retry - isParentMode=$isParentMode, isLoading=$isRewardedAdLoading, ad=${rewardedAd != null}")
                            }
                        }, rewardedRetryDelay)
                        // Increase delay for next retry (exponential backoff, max 30s)
                        rewardedRetryDelay = (rewardedRetryDelay * 2).coerceAtMost(MAX_RETRY_DELAY)
                    } else {
                        // Reset retry delay for non-JavascriptEngine errors
                        rewardedRetryDelay = 2000L
                        AppLogger.w("ℹ️ Non-JavascriptEngine error, retry delay reset to 2000ms")
                    }
                }
            }
        )
        */
    }

    /**
     * Show rewarded ad if loaded
     * 
     * ADS DISABLED FOR ALPHA TESTING - Uncomment code below to re-enable after alpha testing
     */
    fun showRewardedAd(
        context: Context,
        isParentMode: Boolean,
        onRewarded: () -> Unit = {},
        onAdDismissed: () -> Unit = {}
    ) {
        // REVIEWER MODE: If reviewer mode is active, skip ad and grant reward immediately
        if (ReviewerUnlockManager.isUnlocked()) {
            AppLogger.d("REVIEWER MODE: Rewarded ad show skipped - Reviewer Mode active")
            AppLogger.d("REVIEWER MODE: Granting reward immediately (no ad shown)")
            onRewarded()
            return
        }
        
        // Ads are disabled for alpha testing - production IDs are kept but ads are not shown
        // Reward-based features are also disabled - do NOT call onRewarded() to prevent feature unlocks
        // Only call onAdDismissed() to continue app flow
        onAdDismissed()
        return
        
        /* UNCOMMENT BELOW TO RE-ENABLE ADS AFTER ALPHA TESTING
        if (!isParentMode) {
            onAdDismissed()
            return
        }
        
        if (rewardedAd == null) {
            if (isRewardedAdCurrentlyLoading()) {
                AppLogger.w("⚠️ Rewarded ad is still loading...")
                onAdDismissed()
                return
            }
            AppLogger.w("⚠️ Rewarded ad not ready - loading now...")
            loadRewardedAd(context, isParentMode = true)
            // Don't call onAdDismissed() immediately - let the caller know ad isn't ready
            onAdDismissed()
            return
        }
        
        AppLogger.w("✅ Showing rewarded ad")
        
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback =
                object : com.google.android.gms.ads.FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        rewardedAd = null
                        loadRewardedAd(context, isParentMode = true)
                        onAdDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        AppLogger.e("Rewarded ad failed to show: ${error.message} (code: ${error.code})")
                        rewardedAd = null
                        loadRewardedAd(context, isParentMode = true)
                        onAdDismissed()
                    }
                }

            val activity = context.findActivity()
            if (activity == null) {
                AppLogger.e("showRewardedAd: Could not find Activity from Context")
                onAdDismissed()
                return
            }
            
            ad.show(activity) { reward ->
                onRewarded()
            }
        } ?: run {
            AppLogger.w("showRewardedAd: Ad is null")
            onAdDismissed()
        }
        */
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}