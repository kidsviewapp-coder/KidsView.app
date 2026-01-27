package why.xee.kidsview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import why.xee.kidsview.BuildConfig
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.navigation.NavGraph
import why.xee.kidsview.ui.theme.KidsViewTheme
import why.xee.kidsview.utils.AppLogger
import why.xee.kidsview.utils.UpdateManager
import why.xee.kidsview.utils.ReviewManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var watchTimeManager: why.xee.kidsview.data.watchtime.WatchTimeManagerFirebase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        if (BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setUserId("debug-user")
        }
        
        Firebase.auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    user?.uid?.let { uid ->
                        FirebaseCrashlytics.getInstance().setUserId(uid)
                        
                        // ═══════════════════════════════════════════════════════════════
                        // FIREBASE WATCH-TIME MIGRATION
                        // ═══════════════════════════════════════════════════════════════
                        // Migrate watch-time data to Firebase if not already migrated
                        lifecycleScope.launch {
                            try {
                                // Check if already migrated
                                if (!watchTimeManager.isMigratedToFirebase()) {
                                    AppLogger.d("MainActivity: Starting watch-time migration for user $uid")
                                    val migrationSuccess = watchTimeManager.migrateToFirebase()
                                    if (migrationSuccess) {
                                        AppLogger.d("MainActivity: Watch-time migration successful")
                                    } else {
                                        AppLogger.w("MainActivity: Watch-time migration failed, will retry on next launch")
                                    }
                                } else {
                                    AppLogger.d("MainActivity: Watch-time already migrated for user $uid")
                                }
                            } catch (e: Exception) {
                                AppLogger.e("MainActivity: Watch-time migration error", e)
                                // Migration will retry on next app launch
                            }
                        }
                        
                        // Migration disabled to prevent duplicate data on new installations
                        // If you need to migrate existing data, uncomment the code below
                        // and run it once, then comment it back out
                        /*
                        // Trigger migration in background (Phase 2)
                        // Migration runs automatically when user authenticates
                        lifecycleScope.launch {
                            try {
                                // Check if migration is already complete
                                val isComplete = why.xee.kidsview.utils.MigrationHelper.isMigrationComplete()
                                
                                if (!isComplete) {
                                    AppLogger.d("MainActivity: Starting migration for user $uid")
                                    
                                    // Migrate all data (videos + categories)
                                    val migrationResult = why.xee.kidsview.utils.MigrationHelper.migrateAllForCurrentUser()
                                    
                                    migrationResult.onSuccess { (videoCount, categoryCount) ->
                                        AppLogger.d("MainActivity: Migration complete - Videos: $videoCount, Categories: $categoryCount")
                                    }.onFailure { error ->
                                        AppLogger.e("MainActivity: Migration failed", error)
                                        // Migration will retry on next app launch
                                    }
                                } else {
                                    AppLogger.d("MainActivity: Migration already complete for user $uid")
                                }
                            } catch (e: Exception) {
                                AppLogger.e("MainActivity: Migration error", e)
                                // Migration will retry on next app launch
                            }
                        }
                        */
                    }
                } else {
                    val exception = task.exception
                    AppLogger.e("Firebase anonymous authentication failed", exception)
                    exception?.let {
                        FirebaseCrashlytics.getInstance().recordException(it)
                    }
                }
            }
        
        // Initialize Java AdManager (handles ads based on BuildConfig.ADS_ENABLED)
        // Wrapped in try-catch to prevent crashes in alpha builds
        try {
            why.xee.kidsview.utils.AdManager.init(this)
        } catch (e: Exception) {
            AppLogger.e("Failed to initialize AdManager", e)
            // Continue app startup even if AdManager init fails
        }
        
        // Also initialize Kotlin AdManager for backward compatibility (if still used)
        try {
            why.xee.kidsview.ui.components.AdManager.init(preferencesManager)
        } catch (e: Exception) {
            AppLogger.e("Failed to initialize Kotlin AdManager", e)
            // Continue app startup even if AdManager init fails
        }
        
        // Initialize in-app update manager
        UpdateManager.initialize(this)
        
        // Enable edge-to-edge display (required for Android 15+ / SDK 35+)
        // This ensures backward compatibility and proper edge-to-edge support for all Android versions
        // All Scaffolds in the app use paddingValues parameter to handle window insets correctly
        // Screens without Scaffold use navigationBarsPadding() or systemBarsPadding() modifiers
        enableEdgeToEdge()
        
        // Configure window layout to avoid deprecated display cutout mode
        // This prevents Coil and other libraries from using deprecated LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val layoutParams = window.attributes
            // Use default cutout mode (not SHORT_EDGES which is deprecated in Android 15)
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            window.attributes = layoutParams
        }
        
        // Hide navigation bar when app opens (immersive mode)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let { controller ->
            // Hide navigation bar
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            // Make navigation bar stay hidden even when user interacts
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            var themeName by remember { mutableStateOf(preferencesManager.getSelectedTheme()) }
            val lifecycleOwner = LocalLifecycleOwner.current
            // Session-based unlock state - persists until app is closed or explicitly locked
            var isUnlockedInSession by remember { mutableStateOf(false) }
            var hasCheckedInitialLock by remember { mutableStateOf(false) }
            val snackbarHostState = remember { SnackbarHostState() }
            var showUpdateReadySnackbar by remember { mutableStateOf(false) }
            
            // Set callback for when update is downloaded
            LaunchedEffect(Unit) {
                UpdateManager.setOnUpdateDownloadedCallback {
                    showUpdateReadySnackbar = true
                }
            }
            
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            themeName = preferencesManager.getSelectedTheme()
                            
                            // Check for midnight reset (this will trigger reset if past midnight)
                            // This ensures time limits reset automatically even if app wasn't opened at midnight
                            preferencesManager.getTimeUsedToday()
                            
                            // Hide navigation bar on start/resume
                            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                            windowInsetsController?.let { controller ->
                                controller.hide(WindowInsetsCompat.Type.navigationBars())
                                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            }
                            
                            // Only check lock state on first start (app launch), not on every resume
                            if (!hasCheckedInitialLock) {
                                val persistentLockState = preferencesManager.isAppLocked()
                                isUnlockedInSession = !persistentLockState
                                hasCheckedInitialLock = true
                                
                                // Check for app updates on app start (only once per session)
                                // Delay by 3 seconds to avoid interfering with app initialization
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    if (!isFinishing) {
                                        UpdateManager.checkForUpdate(this@MainActivity)
                                    }
                                }, 3000L)
                                
                                // Note: Review tracking is now done in parent mode screens only
                                // This ensures reviews only appear in parent mode, not kids mode
                            } else {
                                // On subsequent resumes, check if app was explicitly locked
                                // If it was locked, reset session state
                                if (preferencesManager.isAppLocked() && isUnlockedInSession) {
                                    isUnlockedInSession = false
                                }
                            }
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            themeName = preferencesManager.getSelectedTheme()
                            
                            // Hide navigation bar on resume
                            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                            windowInsetsController?.let { controller ->
                                controller.hide(WindowInsetsCompat.Type.navigationBars())
                                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            }
                            
                            // On resume, check if app was explicitly locked while in background
                            if (preferencesManager.isAppLocked() && isUnlockedInSession) {
                                isUnlockedInSession = false
                            }
                            
                            // Check for updates when app resumes (background check)
                            // Delay by 2 seconds to avoid interfering with app resume
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (!isFinishing) {
                                    UpdateManager.checkForUpdate(this@MainActivity)
                                }
                            }, 2000L)
                            
                            // Check for updates on resume (but only if app wasn't just started)
                            // This handles cases where user returns to app after update was available
                            if (hasCheckedInitialLock) {
                                UpdateManager.checkForUpdate(this@MainActivity)
                            }
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            // App is locked if persistent lock is true AND not unlocked in this session
            val isAppLocked = preferencesManager.isAppLocked() && !isUnlockedInSession
            
            BackHandler(enabled = isAppLocked) { }
            
            KidsViewTheme(themeName = themeName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavGraph(
                            navController = navController,
                            onThemeChange = { newThemeName ->
                                themeName = newThemeName
                                preferencesManager.setSelectedTheme(newThemeName)
                            },
                            onPasswordVerified = {
                                // When password is verified, unlock the session
                                isUnlockedInSession = true
                            }
                        )
                        
                        // Snackbar for update ready notification
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                    
                    // Show snackbar when update is downloaded (but update will auto-install)
                    LaunchedEffect(showUpdateReadySnackbar) {
                        if (showUpdateReadySnackbar) {
                            // Update will automatically install, but show info to user
                            val result = snackbarHostState.showSnackbar(
                                message = "Update downloaded! Installing automatically...",
                                actionLabel = "Restart Now",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                // User clicked "Restart Now" - complete the update immediately
                                UpdateManager.completeUpdate(this@MainActivity)
                            }
                            // Auto-install will happen in background via UpdateManager
                            showUpdateReadySnackbar = false
                        }
                    }
                }
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle in-app update result
        if (requestCode == 1001) { // UPDATE_REQUEST_CODE from UpdateManager
            when (resultCode) {
                RESULT_OK -> {
                    AppLogger.i("UpdateManager: Update flow completed successfully")
                    // For FLEXIBLE updates, download starts automatically in background
                    // User can continue using the app while update downloads
                }
                RESULT_CANCELED -> {
                    AppLogger.w("UpdateManager: Update flow was cancelled by user")
                    // If user cancelled, offer to open Play Store for manual update
                    UpdateManager.openPlayStoreForUpdate(this)
                }
                else -> {
                    AppLogger.w("UpdateManager: Update flow failed (result code: $resultCode)")
                    // If update failed, open Play Store as fallback
                    UpdateManager.openPlayStoreForUpdate(this)
                }
            }
        }
    }
}