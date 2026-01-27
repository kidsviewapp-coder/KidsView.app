package why.xee.kidsview.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Manages in-app updates using Google Play In-App Update API.
 * Supports both flexible and immediate update flows.
 */
object UpdateManager {
    
    // Use FLEXIBLE update by default (allows background download)
    // Change to AppUpdateType.IMMEDIATE for critical updates
    private const val UPDATE_TYPE = AppUpdateType.FLEXIBLE
    private const val UPDATE_REQUEST_CODE = 1001
    
    private var appUpdateManager: AppUpdateManager? = null
    private var installStateListener: InstallStateUpdatedListener? = null
    private var onUpdateDownloadedCallback: (() -> Unit)? = null
    
    /**
     * Initialize the update manager for an activity.
     * Should be called in onCreate().
     */
    fun initialize(activity: ComponentActivity) {
        try {
            appUpdateManager = AppUpdateManagerFactory.create(activity)
            
            // Register listener for flexible update download progress
            installStateListener = InstallStateUpdatedListener { state ->
                try {
                    when (state.installStatus()) {
                        InstallStatus.DOWNLOADED -> {
                            AppLogger.i("UpdateManager: Update downloaded successfully - automatically installing...")
                            // Automatically install the update when downloaded
                            try {
                                if (activity is Activity) {
                                    completeUpdate(activity)
                                    AppLogger.i("UpdateManager: Update installation triggered automatically")
                                }
                            } catch (e: Exception) {
                                AppLogger.e("UpdateManager: Failed to auto-install update, opening Play Store", e)
                                openPlayStoreForUpdate(activity)
                            }
                            onUpdateDownloadedCallback?.invoke()
                        }
                        InstallStatus.DOWNLOADING -> {
                            val bytesDownloaded = state.bytesDownloaded()
                            val totalBytesToDownload = state.totalBytesToDownload()
                            if (totalBytesToDownload > 0) {
                                val progress = (bytesDownloaded * 100 / totalBytesToDownload).toInt()
                                AppLogger.i("UpdateManager: Download in progress: $progress% ($bytesDownloaded / $totalBytesToDownload bytes)")
                            } else {
                                AppLogger.i("UpdateManager: Download started - calculating size...")
                            }
                        }
                        InstallStatus.PENDING -> {
                            AppLogger.i("UpdateManager: Update pending - waiting to start download")
                        }
                        InstallStatus.INSTALLED -> {
                            AppLogger.i("UpdateManager: Update installed successfully")
                            unregisterInstallStateListener()
                        }
                        InstallStatus.FAILED -> {
                            AppLogger.e("UpdateManager: Update installation failed - opening Play Store as fallback")
                            // If download/installation fails, open Play Store
                            if (activity is Activity) {
                                openPlayStoreForUpdate(activity)
                            }
                            unregisterInstallStateListener()
                        }
                        InstallStatus.CANCELED -> {
                            AppLogger.w("UpdateManager: Update installation canceled - opening Play Store as fallback")
                            // If user cancels, offer Play Store option
                            if (activity is Activity) {
                                openPlayStoreForUpdate(activity)
                            }
                            unregisterInstallStateListener()
                        }
                        else -> {
                            AppLogger.d("UpdateManager: Install status: ${state.installStatus()}")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("UpdateManager: Error in install state listener: ${e.message}", e)
                }
            }
            
            appUpdateManager?.registerListener(installStateListener!!)
        } catch (e: Exception) {
            // Handle initialization errors gracefully
            val errorMessage = e.message ?: ""
            val stackTrace = e.stackTraceToString()
            
            val isPlayStoreNotFound = errorMessage.contains("Play Store", ignoreCase = true) || 
                                     stackTrace.contains("ERROR_PLAY_STORE_NOT_FOUND") ||
                                     stackTrace.contains("Install Error(-9)")
            
            if (isPlayStoreNotFound) {
                AppLogger.d("UpdateManager: Play Store not available - update manager initialization skipped. This is normal in emulators or devices without official Play Store.")
                // Don't set appUpdateManager to null, but mark it as unavailable
            } else {
                AppLogger.w("UpdateManager: Failed to initialize update manager: ${e.message}", e)
            }
        }
    }
    
    /**
     * Set callback to be invoked when update download completes.
     */
    fun setOnUpdateDownloadedCallback(callback: (() -> Unit)?) {
        onUpdateDownloadedCallback = callback
    }
    
    /**
     * Unregister the install state listener.
     */
    private fun unregisterInstallStateListener() {
        installStateListener?.let { listener ->
            appUpdateManager?.unregisterListener(listener)
        }
        installStateListener = null
    }
    
    /**
     * Check for available updates and prompt user if update is available.
     * Should be called after initialize() and when appropriate (e.g., on app start or resume).
     */
    fun checkForUpdate(activity: Activity) {
        val manager = appUpdateManager ?: run {
            AppLogger.e("UpdateManager: UpdateManager not initialized. Call initialize() first.")
            return
        }
        
        try {
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                try {
                    when (appUpdateInfo.updateAvailability()) {
                        UpdateAvailability.UPDATE_AVAILABLE -> {
                            // Check if update is allowed (not already in progress)
                            if (appUpdateInfo.isUpdateTypeAllowed(UPDATE_TYPE)) {
                                AppLogger.i("UpdateManager: Update available. Starting update flow...")
                                AppLogger.i("UpdateManager: For FLEXIBLE updates, download will start automatically in background when user accepts the dialog.")
                                startUpdateFlow(activity, appUpdateInfo)
                            } else {
                                AppLogger.w("UpdateManager: Update available but type not allowed or already in progress. Opening Play Store as fallback.")
                                openPlayStoreForUpdate(activity)
                            }
                        }
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                            // Resume update if it was already started
                            AppLogger.i("UpdateManager: Resuming update in progress...")
                            startUpdateFlow(activity, appUpdateInfo)
                        }
                        UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                            AppLogger.d("UpdateManager: No update available")
                        }
                        UpdateAvailability.UNKNOWN -> {
                            AppLogger.w("UpdateManager: Update availability unknown")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("UpdateManager: Error processing update info: ${e.message}", e)
                }
            }.addOnFailureListener { exception ->
                // Handle Play Store not found error gracefully (common in emulators/devices without Play Store)
                handleUpdateError(exception, "checkForUpdate")
            }
        } catch (e: Exception) {
            // Catch any synchronous exceptions that might occur when calling appUpdateInfo
            handleUpdateError(e, "checkForUpdate (synchronous)")
        }
    }
    
    /**
     * Helper function to handle update errors gracefully
     */
    private fun handleUpdateError(exception: Throwable, context: String) {
        val errorMessage = exception.message ?: ""
        val stackTrace = exception.stackTraceToString()
        
        val isPlayStoreNotFound = errorMessage.contains("Play Store", ignoreCase = true) && 
                                 (errorMessage.contains("not installed", ignoreCase = true) || 
                                  errorMessage.contains("not the official version", ignoreCase = true) ||
                                  errorMessage.contains("ERROR_PLAY_STORE_NOT_FOUND", ignoreCase = true) ||
                                  errorMessage.contains("-9") ||
                                  stackTrace.contains("ERROR_PLAY_STORE_NOT_FOUND") ||
                                  stackTrace.contains("Install Error(-9)"))
        
        if (isPlayStoreNotFound) {
            // Play Store not available - this is expected in development/testing environments
            AppLogger.d("UpdateManager: Play Store not available - update check skipped ($context). This is normal in emulators or devices without official Play Store.")
        } else {
            // Log other errors as warnings (not critical)
            AppLogger.w("UpdateManager: Failed to check for updates ($context): ${exception.message}", exception)
        }
    }
    
    /**
     * Start the update flow.
     * For FLEXIBLE updates, this will show a dialog and start downloading in the background.
     * If the update flow fails, it will fallback to opening Play Store.
     */
    private fun startUpdateFlow(activity: Activity, appUpdateInfo: AppUpdateInfo) {
        val manager = appUpdateManager ?: run {
            AppLogger.e("UpdateManager: Update manager not initialized")
            openPlayStoreForUpdate(activity)
            return
        }
        
        try {
            if (activity is ComponentActivity) {
                manager.startUpdateFlowForResult(
                    appUpdateInfo,
                    UPDATE_TYPE,
                    activity,
                    UPDATE_REQUEST_CODE
                )
                AppLogger.i("UpdateManager: Update flow started successfully. For FLEXIBLE updates, download will start in background when user accepts.")
            } else {
                AppLogger.e("UpdateManager: Activity is not a ComponentActivity")
                openPlayStoreForUpdate(activity)
            }
        } catch (e: Exception) {
            AppLogger.e("UpdateManager: Failed to start update flow, opening Play Store as fallback", e)
            openPlayStoreForUpdate(activity)
        }
    }
    
    /**
     * Open Play Store for manual app update.
     * This is used as a fallback when in-app update fails.
     */
    fun openPlayStoreForUpdate(activity: Activity) {
        try {
            val packageName = activity.packageName
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending") // Ensure it opens in Play Store app
            }
            
            // Try to open Play Store app first
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
                AppLogger.i("UpdateManager: Opened Play Store app for manual update")
            } else {
                // Fallback to web browser if Play Store app is not available
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                }
                if (webIntent.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(webIntent)
                    AppLogger.i("UpdateManager: Opened Play Store website for manual update")
                } else {
                    AppLogger.e("UpdateManager: Cannot open Play Store - no suitable app found")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("UpdateManager: Failed to open Play Store for update", e)
        }
    }
    
    /**
     * Check if an update is currently in progress.
     * Useful for showing update progress UI.
     */
    fun isUpdateInProgress(callback: (Boolean) -> Unit) {
        val manager = appUpdateManager ?: run {
            callback(false)
            return
        }
        
        manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            val inProgress = appUpdateInfo.updateAvailability() == 
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            callback(inProgress)
        }.addOnFailureListener {
            callback(false)
        }
    }
    
    /**
     * Complete the flexible update installation.
     * Should be called when flexible update is downloaded and ready to install.
     * This will trigger the app restart to install the update.
     * If installation fails, opens Play Store as fallback.
     */
    fun completeUpdate(activity: Activity) {
        val manager = appUpdateManager ?: run {
            AppLogger.e("UpdateManager: Update manager not initialized - opening Play Store")
            openPlayStoreForUpdate(activity)
            return
        }
        
        try {
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                try {
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        manager.completeUpdate()
                        AppLogger.i("UpdateManager: Completing update - app will restart")
                    } else {
                        AppLogger.w("UpdateManager: Update not downloaded yet. Status: ${appUpdateInfo.installStatus()} - opening Play Store")
                        // If update is not ready, open Play Store
                        openPlayStoreForUpdate(activity)
                    }
                } catch (e: Exception) {
                    AppLogger.e("UpdateManager: Error completing update: ${e.message} - opening Play Store", e)
                    openPlayStoreForUpdate(activity)
                }
            }.addOnFailureListener { exception ->
                AppLogger.e("UpdateManager: Failed to get update info for completion - opening Play Store", exception)
                openPlayStoreForUpdate(activity)
            }
        } catch (e: Exception) {
            AppLogger.e("UpdateManager: Exception completing update - opening Play Store", e)
            openPlayStoreForUpdate(activity)
        }
    }
    
    /**
     * Get the AppUpdateManager instance (for checking update availability)
     */
    fun getAppUpdateManager(): AppUpdateManager? {
        return appUpdateManager
    }
    
    /**
     * Check if an update is available without automatically starting the flow.
     * Useful for UI components that want to show update button.
     */
    fun checkUpdateAvailability(callback: (Boolean) -> Unit) {
        val manager = appUpdateManager ?: run {
            callback(false)
            return
        }
        
        try {
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                try {
                    val available = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                            appUpdateInfo.isUpdateTypeAllowed(UPDATE_TYPE)
                    callback(available)
                } catch (e: Exception) {
                    AppLogger.w("UpdateManager: Error checking update availability: ${e.message}", e)
                    callback(false)
                }
            }.addOnFailureListener {
                callback(false)
            }
        } catch (e: Exception) {
            handleUpdateError(e, "checkUpdateAvailability (synchronous)")
            callback(false)
        }
    }
    
    /**
     * Manually start the update flow.
     * Should be called when user explicitly requests update (e.g., clicks update button).
     */
    fun startUpdateManually(activity: Activity) {
        val manager = appUpdateManager ?: run {
            AppLogger.e("UpdateManager: UpdateManager not initialized. Call initialize() first.")
            return
        }
        
        try {
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                try {
                    when (appUpdateInfo.updateAvailability()) {
                        UpdateAvailability.UPDATE_AVAILABLE -> {
                            if (appUpdateInfo.isUpdateTypeAllowed(UPDATE_TYPE)) {
                                AppLogger.i("UpdateManager: Starting update flow manually...")
                                startUpdateFlow(activity, appUpdateInfo)
                            } else {
                                AppLogger.w("UpdateManager: Update available but type not allowed")
                            }
                        }
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                            AppLogger.i("UpdateManager: Resuming update in progress...")
                            startUpdateFlow(activity, appUpdateInfo)
                        }
                        else -> {
                            AppLogger.d("UpdateManager: No update available to start")
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("UpdateManager: Error processing manual update: ${e.message}", e)
                }
            }.addOnFailureListener { exception ->
                handleUpdateError(exception, "startUpdateManually")
            }
        } catch (e: Exception) {
            handleUpdateError(e, "startUpdateManually (synchronous)")
        }
    }
}

