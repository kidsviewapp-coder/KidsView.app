package why.xee.kidsview.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;
import com.google.firebase.auth.FirebaseAuth;

import why.xee.kidsview.BuildConfig;
import why.xee.kidsview.utils.ReviewerUnlockManager;

/**
 * Unified AdManager for KidsView Android App
 * 
 * ============================================================================
 * AD ENABLING/DISABLING GUIDE
 * ============================================================================
 * 
 * All ads are controlled by BuildConfig.ADS_ENABLED flag:
 * 
 * TO DISABLE ADS (for alpha/testing builds):
 * - Edit app/build.gradle.kts
 * - Find the build type (debug/alpha/release)
 * - Set: buildConfigField("boolean", "ADS_ENABLED", "false")
 * - Rebuild the app
 * 
 * TO ENABLE ADS (for production release):
 * - Edit app/build.gradle.kts
 * - Find the "release" build type
 * - Set: buildConfigField("boolean", "ADS_ENABLED", "true")
 * - Rebuild the app
 * 
 * When ADS_ENABLED = false:
 * - Ad UI is automatically hidden
 * - Ad loading is prevented (no network calls)
 * - Rewarded ads grant fake rewards for testing
 * - No AdMob SDK initialization occurs
 * 
 * When ADS_ENABLED = true:
 * - All ads function normally
 * - Production AdMob IDs are used
 * - Real ads are displayed
 * 
 * ============================================================================
 * FEATURES
 * ============================================================================
 * 
 * - Banner Ads: Displayed at bottom of Parent Mode screens
 * - Interstitial Ads: Shown between screens with 60-second cooldown
 * - Rewarded Ads: Grant bonus time when watched
 * - Automatic retry logic for transient errors
 * - Exponential backoff for failed ad loads
 * - COPPA-compliant ad configuration (child-directed, G-rated)
 * - Server-side verification for rewarded ads
 * 
 * ============================================================================
 * USAGE
 * ============================================================================
 * 
 * 1. Initialize in MainActivity.onCreate():
 *    AdManager.init(context);
 * 
 * 2. Load ads when entering Parent Mode screens:
 *    AdManager.getInstance().loadInterstitialAd(context, isParentMode);
 *    AdManager.getInstance().loadRewardedAd(context, isParentMode);
 * 
 * 3. Show ads:
 *    AdManager.getInstance().showInterstitialAd(context, isParentMode, false, () -> {
 *        // Ad dismissed, continue app flow
 *    });
 *    
 *    AdManager.getInstance().showRewardedAd(context, isParentMode, () -> {
 *        // Reward granted (30 minutes bonus time)
 *    }, () -> {
 *        // Ad dismissed
 *    });
 * 
 * 4. Create banner ad view (for XML layouts):
 *    AdView bannerAd = AdManager.getInstance().createBannerAd(context, isParentMode);
 *    if (bannerAd != null) {
 *        containerLayout.addView(bannerAd);
 *    }
 * 
 */
public class AdManager {
    private static final String TAG = "AdManager";
    
    // Singleton instance
    private static AdManager instance;
    
    // Ad objects
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private AdView bannerAd;
    
    // Loading state tracking
    private boolean isInterstitialAdLoading = false;
    private boolean isRewardedAdLoading = false;
    private boolean isMobileAdsInitialized = false;
    
    // Cooldown mechanism (60 seconds between interstitial ads - AdMob policy)
    private static final long AD_COOLDOWN_MS = 60_000L; // 60 seconds
    private static final String PREFS_NAME = "admob_cooldown_prefs";
    private static final String KEY_LAST_AD_SHOW_TIME = "last_interstitial_ad_time";
    private static final String KEY_LAST_REWARDED_AD_SHOW_TIME = "last_rewarded_ad_time";
    
    // Retry delay for errors (exponential backoff)
    private long interstitialRetryDelay = 2000L; // Start with 2 seconds
    private long rewardedRetryDelay = 2000L; // Start with 2 seconds
    private static final long MAX_RETRY_DELAY = 30_000L; // Max 30 seconds
    private static final long MIN_INIT_DELAY = 2000L; // Wait 2 seconds after MobileAds init
    
    /**
     * Check if ads are enabled via BuildConfig
     * This is the main gate for all ad functionality
     */
    private static boolean areAdsEnabled() {
        return BuildConfig.ADS_ENABLED;
    }
    
    /**
     * Check if using test ad IDs (debug builds)
     * @return true if using test IDs, false otherwise
     */
    public static boolean isUsingTestIds() {
        return BuildConfig.USE_TEST_IDS;
    }
    
    /**
     * Check if this is a production build
     * @return true if production build, false otherwise
     */
    public static boolean isProduction() {
        return BuildConfig.IS_PRODUCTION;
    }
    
    /**
     * Private constructor for singleton pattern
     */
    private AdManager() {
    }
    
    /**
     * Get singleton instance
     * Use this to access AdManager methods
     */
    public static AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }
    
    /**
     * Initialize AdMob SDK
     * Should be called from MainActivity.onCreate()
     * 
     * This method:
     * - Checks BuildConfig.ADS_ENABLED before initializing
     * - Configures COPPA-compliant ad settings
     * - Sets up test device IDs (if uncommented)
     * - Initializes MobileAds SDK
     * 
     * @param context Application context
     */
    public static void init(@NonNull Context context) {
        try {
            // ALPHA MODE: Early return if ads are disabled - prevents ANY AdMob initialization
            // This ensures Alpha builds (Play Store testing) have ZERO ad SDK initialization
            // No ad requests, no ad loading, no ad SDK calls whatsoever
            if (!areAdsEnabled()) {
                Log.w(TAG, "═══════════════════════════════════════════════════════════");
                Log.w(TAG, "ALPHA BUILD: Ads are disabled (ADS_ENABLED = false)");
                Log.w(TAG, "ALPHA BUILD: AdMob SDK will NOT be initialized");
                Log.w(TAG, "ALPHA BUILD: No ad SDK initialization, no ad loading, no ad requests");
                Log.w(TAG, "ALPHA BUILD: Fake rewards will be granted immediately when 'Watch Ad' is triggered");
                Log.w(TAG, "═══════════════════════════════════════════════════════════");
                return;
            }
        } catch (Exception e) {
            // Safety: If BuildConfig access fails, don't initialize ads
            Log.e(TAG, "Error checking ADS_ENABLED - ads will be disabled for safety", e);
            return;
        }
        
        // Determine build type for logging
        String buildType = isUsingTestIds() ? "DEBUG (Test Ads)" : "RELEASE (Production Ads)";
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        Log.d(TAG, "Initializing AdMob SDK...");
        Log.d(TAG, "Build Type: " + buildType);
        Log.d(TAG, "App ID: " + BuildConfig.ADMOB_APP_ID);
        Log.d(TAG, "═══════════════════════════════════════════════════════════");
        
        // IMPORTANT: Set RequestConfiguration BEFORE MobileAds.initialize()
        // This ensures COPPA compliance for child-directed apps
        RequestConfiguration.Builder requestConfigBuilder = new RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G);
        
        // Test device configuration removed - no test ads functionality
        
        RequestConfiguration requestConfiguration = requestConfigBuilder.build();
        MobileAds.setRequestConfiguration(requestConfiguration);
        
        // Initialize MobileAds SDK
        MobileAds.initialize(context, initializationStatus -> {
            Log.d(TAG, "AdMob SDK initialized - Status: " + initializationStatus.getAdapterStatusMap().keySet());
            initializationStatus.getAdapterStatusMap().forEach((adapter, status) -> {
                Log.d(TAG, "Adapter: " + adapter + " - State: " + status.getInitializationState() + 
                      ", Description: " + status.getDescription());
            });
            
            // Wait 2 seconds after initialization before allowing ads to load
            // This ensures the SDK is fully ready
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                getInstance().isMobileAdsInitialized = true;
                Log.d(TAG, "MobileAds fully ready - ads can now be loaded");
            }, MIN_INIT_DELAY);
        });
    }
    
    /**
     * Check if MobileAds SDK is initialized and ready
     * @return true if initialized, false otherwise
     */
    public boolean isMobileAdsInitialized() {
        return isMobileAdsInitialized;
    }
    
    /**
     * Create and configure a Banner Ad View
     * Returns null if ads are disabled or not in parent mode
     * 
     * This method handles:
     * - BuildConfig.ADS_ENABLED check
     * - Parent mode check
     * - Ad unit ID validation
     * - Automatic ad loading after MobileAds initialization
     * 
     * @param context Context
     * @param isParentMode Whether app is in parent mode (ads only shown in parent mode)
     * @return AdView or null if ads disabled/not in parent mode
     */
    @Nullable
    public AdView createBannerAd(@NonNull Context context, boolean isParentMode) {
        // REVIEWER MODE: If reviewer mode is active, skip banner ad creation
        if (ReviewerUnlockManager.INSTANCE.isUnlocked()) {
            Log.d(TAG, "REVIEWER MODE: Banner ad creation skipped - Reviewer Mode active");
            return null;
        }
        
        // ALPHA MODE: Early return if ads are disabled - NO banner ads in Alpha mode
        if (!areAdsEnabled()) {
            Log.d(TAG, "ALPHA MODE: Banner ad creation skipped - ads are disabled (ADS_ENABLED = false)");
            Log.d(TAG, "ALPHA MODE: No banner ad created, no ad SDK calls");
            return null;
        }
        
        // Only show ads in parent mode (not in kids mode)
        if (!isParentMode) {
            Log.d(TAG, "Banner ad creation skipped - not in parent mode");
            return null;
        }
        
        // Validate ad unit ID
        String adUnitId = BuildConfig.ADMOB_BANNER_ID;
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            Log.w(TAG, "Banner ad unit ID is blank - ad will not load");
            Log.w(TAG, "Add admob.banner.id=YOUR_BANNER_ID to local.properties");
            return null;
        }
        Log.d(TAG, "Using Banner Ad ID: " + adUnitId);
        
        // Create and configure AdView
        AdView adView = new AdView(context);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(adUnitId);
        
        // Set up ad listener for loading callbacks
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                Log.d(TAG, "✅ Banner ad loaded successfully (unit: " + adUnitId + ")");
            }
            
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                super.onAdFailedToLoad(error);
                String errorMessage = error.getMessage() != null ? error.getMessage() : "";
                Log.e(TAG, "❌ Banner ad failed to load: " + errorMessage + " (code: " + error.getCode() + ")");
            }
        });
        
        // Wait for MobileAds initialization before loading
        if (isMobileAdsInitialized) {
            adView.loadAd(new AdRequest.Builder().build());
        } else {
            Log.w(TAG, "Banner ad waiting for MobileAds initialization...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isMobileAdsInitialized) {
                    adView.loadAd(new AdRequest.Builder().build());
                }
            }, MIN_INIT_DELAY);
        }
        
        bannerAd = adView;
        return adView;
    }
    
    /**
     * Load interstitial ad
     * Prevents multiple simultaneous loads
     * 
     * This method:
     * - Checks BuildConfig.ADS_ENABLED
     * - Validates parent mode
     * - Prevents duplicate loads
     * - Handles retry logic for transient errors
     * 
     * @param context Context
     * @param isParentMode Whether app is in parent mode
     */
    public void loadInterstitialAd(@NonNull Context context, boolean isParentMode) {
        // REVIEWER MODE: If reviewer mode is active, skip ad loading
        if (ReviewerUnlockManager.INSTANCE.isUnlocked()) {
            Log.d(TAG, "REVIEWER MODE: Interstitial ad load skipped - Reviewer Mode active");
            return;
        }
        
        // ALPHA MODE: Early return if ads are disabled - NO ad loading in Alpha mode
        if (!areAdsEnabled()) {
            Log.d(TAG, "ALPHA MODE: Interstitial ad load skipped - ads are disabled (ADS_ENABLED = false)");
            Log.d(TAG, "ALPHA MODE: No ad loading, no ad requests, no ad SDK calls");
            return;
        }
        
        // Only load ads in parent mode
        if (!isParentMode) {
            Log.d(TAG, "Interstitial ad load skipped - not in parent mode");
            return;
        }
        
        // Skip if already loaded
        if (interstitialAd != null) {
            Log.d(TAG, "Interstitial ad already loaded, skipping load");
            return;
        }
        
        // Skip if already loading (prevent duplicate loads)
        if (isInterstitialAdLoading) {
            Log.d(TAG, "Interstitial ad already loading, skipping duplicate load");
            return;
        }
        
        // Wait for MobileAds initialization
        if (!isMobileAdsInitialized) {
            Log.w(TAG, "MobileAds not initialized yet, waiting " + MIN_INIT_DELAY + "ms...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isParentMode && !isInterstitialAdLoading && interstitialAd == null) {
                    loadInterstitialAd(context, isParentMode);
                }
            }, MIN_INIT_DELAY);
            return;
        }
        
        // Validate ad unit ID
        String adUnitId = BuildConfig.ADMOB_INTERSTITIAL_ID;
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            Log.w(TAG, "Interstitial ad unit ID is blank - ad will not load");
            Log.w(TAG, "Add admob.interstitial.id=YOUR_INTERSTITIAL_ID to local.properties");
            return;
        }
        Log.d(TAG, "Using Interstitial Ad ID: " + adUnitId);
        
        Log.d(TAG, "🔄 Loading interstitial ad (unit: " + adUnitId + ")...");
        isInterstitialAdLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        
        InterstitialAd.load(context, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                isInterstitialAdLoading = false;
                interstitialAd = ad;
                interstitialRetryDelay = 2000L; // Reset retry delay on success
                Log.d(TAG, "✅ Interstitial ad loaded successfully");
            }
            
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                isInterstitialAdLoading = false;
                interstitialAd = null;
                String errorMessage = error.getMessage() != null ? error.getMessage() : "";
                Log.e(TAG, "❌ Interstitial ad failed to load: " + errorMessage + 
                      " (code: " + error.getCode() + ", domain: " + error.getDomain() + ")");
                
                // Handle JavascriptEngine error with retry backoff
                // This is a transient error that often resolves on retry
                boolean isJavascriptEngineError = errorMessage.contains("JavascriptEngine") || 
                        errorMessage.contains("Unable to obtain");
                
                if (isJavascriptEngineError) {
                    Log.w(TAG, "⚠️ JavascriptEngine error detected, will retry after " + interstitialRetryDelay + "ms");
                    // Schedule retry with exponential backoff
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d(TAG, "🔄 Retrying interstitial ad load after " + interstitialRetryDelay + "ms delay");
                        if (isParentMode && !isInterstitialAdLoading && interstitialAd == null) {
                            loadInterstitialAd(context, isParentMode);
                        } else {
                            Log.w(TAG, "Skipping retry - isParentMode=" + isParentMode + 
                                  ", isLoading=" + isInterstitialAdLoading + ", ad=" + (interstitialAd != null));
                        }
                    }, interstitialRetryDelay);
                    // Increase delay for next retry (exponential backoff, max 30s)
                    interstitialRetryDelay = Math.min(interstitialRetryDelay * 2, MAX_RETRY_DELAY);
                } else {
                    // Reset retry delay for non-JavascriptEngine errors
                    interstitialRetryDelay = 2000L;
                    Log.d(TAG, "Non-JavascriptEngine error, retry delay reset to 2000ms");
                }
            }
        });
    }
    
    /**
     * Show interstitial ad if loaded and cooldown period has passed
     * 
     * Cooldown: 60 seconds between ads (AdMob policy requirement)
     * 
     * @param context Context for accessing SharedPreferences
     * @param isParentMode Whether app is in parent mode
     * @param forceShow If true, bypass cooldown check (use sparingly)
     * @param onAdDismissed Callback when ad is dismissed or failed (can be null)
     */
    public void showInterstitialAd(@NonNull Context context, boolean isParentMode, 
                                   boolean forceShow, @Nullable Runnable onAdDismissed) {
        // REVIEWER MODE: If reviewer mode is active, skip ad and continue immediately
        if (ReviewerUnlockManager.INSTANCE.isUnlocked()) {
            Log.d(TAG, "REVIEWER MODE: Interstitial ad show skipped - Reviewer Mode active");
            Log.d(TAG, "REVIEWER MODE: No ad shown, continuing app flow immediately");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }
        
        // ALPHA MODE: Early return if ads are disabled - NO ads shown in Alpha mode
        if (!areAdsEnabled()) {
            Log.d(TAG, "ALPHA MODE: Interstitial ad show skipped - ads are disabled (ADS_ENABLED = false)");
            Log.d(TAG, "ALPHA MODE: No ad shown, continuing app flow immediately");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }
        
        // Only show ads in parent mode
        if (!isParentMode) {
            Log.d(TAG, "Interstitial ad show skipped - not in parent mode");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }
        
        // Check cooldown period (unless forced)
        if (!forceShow && !canShowAd(context)) {
            long cooldownRemaining = getCooldownRemainingSeconds(context);
            Log.w(TAG, "⏱️ Interstitial ad in cooldown: " + cooldownRemaining + "s remaining");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }
        
        // If ad not loaded, try loading it and continue app flow
        if (interstitialAd == null) {
            Log.w(TAG, "Interstitial ad not ready - loading now...");
            loadInterstitialAd(context, isParentMode);
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }
        
        Log.d(TAG, "✅ Showing interstitial ad");
        
        // Record that we're about to show an ad (for cooldown tracking)
        recordAdShown(context);
        
        // Set up full screen content callback
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                // Preload next ad
                loadInterstitialAd(context, isParentMode);
                if (onAdDismissed != null) {
                    onAdDismissed.run();
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                Log.e(TAG, "Interstitial ad failed to show: " + error.getMessage());
                interstitialAd = null;
                // Try loading next ad
                loadInterstitialAd(context, isParentMode);
                if (onAdDismissed != null) {
                    onAdDismissed.run();
                }
            }
        });
        
        // Show the ad
        Activity activity = findActivity(context);
        if (activity != null) {
            interstitialAd.show(activity);
        } else {
            Log.e(TAG, "Cannot find Activity to show ad");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
        }
    }
    
    /**
     * Load rewarded ad
     * Prevents multiple simultaneous loads
     * 
     * This method:
     * - Checks BuildConfig.ADS_ENABLED
     * - Validates parent mode
     * - Prevents duplicate loads
     * - Configures server-side verification (SSV)
     * - Handles retry logic for transient errors
     * 
     * @param context Context
     * @param isParentMode Whether app is in parent mode
     */
    public void loadRewardedAd(@NonNull Context context, boolean isParentMode) {
        // REVIEWER MODE: If reviewer mode is active, skip ad loading
        if (ReviewerUnlockManager.INSTANCE.isUnlocked()) {
            Log.d(TAG, "REVIEWER MODE: Rewarded ad load skipped - Reviewer Mode active");
            return;
        }
        
        // ALPHA MODE: Early return if ads are disabled - NO ad loading in Alpha mode
        if (!areAdsEnabled()) {
            Log.d(TAG, "ALPHA MODE: Rewarded ad load skipped - ads are disabled (ADS_ENABLED = false)");
            Log.d(TAG, "ALPHA MODE: No ad loading, no ad requests, no ad SDK calls");
            return;
        }
        
        // Only load ads in parent mode
        if (!isParentMode) {
            Log.d(TAG, "Rewarded ad load skipped - not in parent mode");
            return;
        }
        
        // Skip if already loaded
        if (rewardedAd != null) {
            Log.d(TAG, "Rewarded ad already loaded, skipping load");
            return;
        }
        
        // Skip if already loading (prevent duplicate loads)
        if (isRewardedAdLoading) {
            Log.d(TAG, "Rewarded ad already loading, skipping duplicate load");
            return;
        }
        
        // Wait for MobileAds initialization
        if (!isMobileAdsInitialized) {
            Log.w(TAG, "MobileAds not initialized yet, waiting " + MIN_INIT_DELAY + "ms...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isParentMode && !isRewardedAdLoading && rewardedAd == null) {
                    loadRewardedAd(context, isParentMode);
                }
            }, MIN_INIT_DELAY);
            return;
        }
        
        // Validate ad unit ID
        String adUnitId = BuildConfig.ADMOB_REWARDED_ID;
        if (adUnitId == null || adUnitId.trim().isEmpty()) {
            Log.w(TAG, "Rewarded ad unit ID is blank - ad will not load");
            Log.w(TAG, "Add admob.rewarded.id=YOUR_REWARDED_ID to local.properties");
            return;
        }
        Log.d(TAG, "Using Rewarded Ad ID: " + adUnitId);
        
        Log.d(TAG, "🔄 Loading rewarded ad (unit: " + adUnitId + ")...");
        isRewardedAdLoading = true;
        
        AdRequest adRequest = new AdRequest.Builder().build();
        
        RewardedAd.load(context, adUnitId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                isRewardedAdLoading = false;
                rewardedAd = ad;
                rewardedRetryDelay = 2000L; // Reset retry delay on success
                Log.d(TAG, "✅ Rewarded ad loaded successfully");
                
                // Configure Server-Side Verification (SSV)
                // Include Firebase Anonymous Auth UID as custom data for server verification
                // This helps prevent fraudulent reward claims
                String firebaseUid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
                ServerSideVerificationOptions ssvOptions = new ServerSideVerificationOptions.Builder()
                        .setCustomData(firebaseUid)
                        .build();
                ad.setServerSideVerificationOptions(ssvOptions);
            }
            
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                isRewardedAdLoading = false;
                rewardedAd = null;
                
                String errorMessage = error.getMessage() != null ? error.getMessage() : "";
                int errorCode = error.getCode();
                
                // Code 3 = "No fill" - This is NORMAL, not an error
                // It just means AdMob doesn't have an ad available at this moment
                if (errorCode == 3 || errorMessage.contains("No fill")) {
                    Log.d(TAG, "ℹ️ Rewarded ad: No fill (code: " + errorCode + 
                          ") - No ad available at this time. This is normal.");
                    // Reset retry delay for "No fill" - it's not an error, just no ad available
                    rewardedRetryDelay = 2000L;
                    return;
                }
                
                // For actual errors, log them
                Log.e(TAG, "❌ Rewarded ad failed to load: " + errorMessage + " (code: " + errorCode + ")");
                if (error.getCause() != null) {
                    Log.e(TAG, "Rewarded ad error cause: " + error.getCause());
                }
                
                // Handle JavascriptEngine error with retry backoff
                // This is a transient error that often resolves on retry
                boolean isJavascriptEngineError = errorMessage.contains("JavascriptEngine") || 
                        errorMessage.contains("Unable to obtain");
                
                if (isJavascriptEngineError) {
                    Log.w(TAG, "⚠️ Rewarded ad JavascriptEngine error detected, will retry after " + 
                          rewardedRetryDelay + "ms");
                    // Schedule retry with exponential backoff
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Log.d(TAG, "🔄 Retrying rewarded ad load after " + rewardedRetryDelay + "ms delay");
                        if (isParentMode && !isRewardedAdLoading && rewardedAd == null) {
                            loadRewardedAd(context, isParentMode);
                        } else {
                            Log.w(TAG, "Skipping retry - isParentMode=" + isParentMode + 
                                  ", isLoading=" + isRewardedAdLoading + ", ad=" + (rewardedAd != null));
                        }
                    }, rewardedRetryDelay);
                    // Increase delay for next retry (exponential backoff, max 30s)
                    rewardedRetryDelay = Math.min(rewardedRetryDelay * 2, MAX_RETRY_DELAY);
                } else {
                    // Reset retry delay for non-JavascriptEngine errors
                    rewardedRetryDelay = 2000L;
                    Log.d(TAG, "Non-JavascriptEngine error, retry delay reset to 2000ms");
                }
            }
        });
    }
    
    /**
     * Show rewarded ad if loaded
     * When ads are disabled, grants fake reward for testing
     * 
     * IMPORTANT: When ADS_ENABLED = false, this method grants a fake reward
     * immediately without showing an ad. This allows testers to fully explore
     * features without real ads during alpha/testing.
     * 
     * @param context Context
     * @param isParentMode Whether app is in parent mode
     * @param onRewarded Callback when reward is granted (can be null)
     * @param onAdDismissed Callback when ad is dismissed or failed (can be null)
     */
    public void showRewardedAd(@NonNull Context context, boolean isParentMode, 
                               @Nullable Runnable onRewarded, @Nullable Runnable onAdDismissed) {
        // REVIEWER MODE: If reviewer mode is active, skip ad and grant reward immediately
        if (ReviewerUnlockManager.INSTANCE.isUnlocked()) {
            Log.d(TAG, "REVIEWER MODE: Rewarded ad show skipped - Reviewer Mode active");
            Log.d(TAG, "REVIEWER MODE: Granting reward immediately (no ad shown)");
            if (onRewarded != null) {
                onRewarded.run();
            }
            // Don't call onAdDismissed for reviewer mode - reward was granted immediately
            return;
        }
        
        // ALPHA MODE: If ads are disabled, grant fake reward immediately
        // This is for Play Store alpha testing builds - NO real ads, immediate fake rewards
        if (!areAdsEnabled()) {
            Log.w(TAG, "ALPHA MODE: Rewarded ad show skipped - ads are disabled (ADS_ENABLED = false)");
            Log.w(TAG, "ALPHA MODE: Granting fake reward immediately (no ad shown, no ad SDK calls)");
            // Grant fake reward immediately - no ad shown, no ad SDK initialization
            if (onRewarded != null) {
                onRewarded.run();
            }
            // Don't call onAdDismissed for fake rewards - reward was granted immediately
            return;
        }
        
        // DEBUG MODE: Show test ad (will show "Test Ad" labels)
        if (isUsingTestIds()) {
            Log.d(TAG, "DEBUG MODE: Showing test rewarded ad (Test Ad labels will be visible)");
        } else {
            Log.d(TAG, "PRODUCTION MODE: Showing real rewarded ad");
        }
        
        // Only show ads in parent mode
        if (!isParentMode) {
            Log.d(TAG, "Rewarded ad show skipped - not in parent mode");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }
        
        // If ad not loaded, try loading it
        if (rewardedAd == null) {
            if (isRewardedAdCurrentlyLoading()) {
                Log.w(TAG, "Rewarded ad is still loading...");
                if (onAdDismissed != null) {
                    onAdDismissed.run();
                }
                return;
            }
            Log.w(TAG, "Rewarded ad not ready - loading now...");
            loadRewardedAd(context, isParentMode);
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }
        
        Log.d(TAG, "✅ Showing rewarded ad");
        
        // Set up full screen content callback
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                // Preload next ad
                loadRewardedAd(context, isParentMode);
                if (onAdDismissed != null) {
                    onAdDismissed.run();
                }
            }
            
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                Log.e(TAG, "Rewarded ad failed to show: " + error.getMessage() + " (code: " + error.getCode() + ")");
                rewardedAd = null;
                // Try loading next ad
                loadRewardedAd(context, isParentMode);
                if (onAdDismissed != null) {
                    onAdDismissed.run();
                }
            }
        });
        
        // Show the ad
        Activity activity = findActivity(context);
        if (activity == null) {
            Log.e(TAG, "showRewardedAd: Could not find Activity from Context");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }
        
        // Show ad and handle reward
        rewardedAd.show(activity, rewardItem -> {
            int rewardAmount = rewardItem.getAmount();
            String rewardType = rewardItem.getType();
            
            if (isUsingTestIds()) {
                Log.d(TAG, "✅ DEBUG MODE: Test ad completed - Reward granted: " + rewardAmount + " " + rewardType);
            } else {
                Log.d(TAG, "✅ PRODUCTION MODE: Real ad completed - Reward granted: " + rewardAmount + " " + rewardType);
            }
            
            if (onRewarded != null) {
                onRewarded.run();
            }
        });
    }
    
    // ==================== Status Check Methods ====================
    
    /**
     * Check if a rewarded ad is currently loaded and ready to show
     * @return true if loaded, false otherwise
     */
    public boolean isRewardedAdReady() {
        return rewardedAd != null;
    }
    
    /**
     * Check if a rewarded ad is currently loading
     * @return true if loading, false otherwise
     */
    public boolean isRewardedAdCurrentlyLoading() {
        return isRewardedAdLoading;
    }
    
    /**
     * Check if interstitial ad is ready to show
     * @return true if loaded, false otherwise
     */
    public boolean isInterstitialAdReady() {
        return interstitialAd != null;
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Get SharedPreferences for ad cooldown tracking
     */
    private SharedPreferences getAdCooldownPrefs(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if enough time has passed since last ad (cooldown period)
     * Cooldown: 60 seconds (AdMob policy requirement)
     */
    private boolean canShowAd(@NonNull Context context) {
        SharedPreferences prefs = getAdCooldownPrefs(context);
        long lastAdTime = prefs.getLong(KEY_LAST_AD_SHOW_TIME, 0L);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastAdTime;
        return elapsed >= AD_COOLDOWN_MS;
    }
    
    /**
     * Record that an ad was shown (for cooldown tracking)
     */
    private void recordAdShown(@NonNull Context context) {
        SharedPreferences prefs = getAdCooldownPrefs(context);
        prefs.edit().putLong(KEY_LAST_AD_SHOW_TIME, System.currentTimeMillis()).apply();
    }
    
    /**
     * Get time remaining in cooldown period (in seconds)
     * @param context Context
     * @return Seconds remaining in cooldown, or 0 if cooldown has passed
     */
    public long getCooldownRemainingSeconds(@NonNull Context context) {
        SharedPreferences prefs = getAdCooldownPrefs(context);
        long lastAdTime = prefs.getLong(KEY_LAST_AD_SHOW_TIME, 0L);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastAdTime;
        long remaining = AD_COOLDOWN_MS - elapsed;
        return remaining > 0 ? remaining / 1000 : 0;
    }
    
    /**
     * Check if enough time has passed since last rewarded ad (cooldown period)
     * Cooldown: 60 seconds (AdMob policy requirement)
     * @param context Context
     * @return true if cooldown has passed, false otherwise
     */
    public boolean canShowRewardedAd(@NonNull Context context) {
        SharedPreferences prefs = getAdCooldownPrefs(context);
        long lastAdTime = prefs.getLong(KEY_LAST_REWARDED_AD_SHOW_TIME, 0L);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastAdTime;
        return elapsed >= AD_COOLDOWN_MS;
    }
    
    /**
     * Record that a rewarded ad was shown (for cooldown tracking)
     * @param context Context
     */
    public void recordRewardedAdShown(@NonNull Context context) {
        SharedPreferences prefs = getAdCooldownPrefs(context);
        prefs.edit().putLong(KEY_LAST_REWARDED_AD_SHOW_TIME, System.currentTimeMillis()).apply();
    }
    
    /**
     * Get time remaining in rewarded ad cooldown period (in seconds)
     * @param context Context
     * @return Seconds remaining in cooldown, or 0 if cooldown has passed
     */
    public long getRewardedAdCooldownRemainingSeconds(@NonNull Context context) {
        SharedPreferences prefs = getAdCooldownPrefs(context);
        long lastAdTime = prefs.getLong(KEY_LAST_REWARDED_AD_SHOW_TIME, 0L);
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastAdTime;
        long remaining = AD_COOLDOWN_MS - elapsed;
        return remaining > 0 ? remaining / 1000 : 0;
    }
    
    /**
     * Find Activity from Context (recursive)
     * Used to get Activity for showing full-screen ads
     */
    private Activity findActivity(@NonNull Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return findActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }
    
    /**
     * Hide ad view if ads are disabled
     * Call this on banner ad views to hide them when ads are disabled
     * 
     * @param adView The ad view to hide (can be null)
     */
    public static void hideAdViewIfDisabled(@Nullable View adView) {
        if (adView != null && !areAdsEnabled()) {
            adView.setVisibility(View.GONE);
            if (adView.getParent() != null && adView.getParent() instanceof ViewGroup) {
                ((ViewGroup) adView.getParent()).removeView(adView);
            }
        }
    }
}
