package why.xee.kidsview.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import why.xee.kidsview.BuildConfig

/**
 * Centralized logging utility that:
 * - Only logs in debug builds (removes logs in release)
 * - Sends errors to Firebase Crashlytics in release builds
 * - Sanitizes sensitive information
 */
object AppLogger {
    private const val TAG = "KidsView"
    
    /**
     * Log debug messages (only in debug builds)
     */
    fun d(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.d(TAG, sanitizeMessage(message), throwable)
            } else {
                Log.d(TAG, sanitizeMessage(message))
            }
        }
    }
    
    /**
     * Log info messages (only in debug builds)
     */
    fun i(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.i(TAG, sanitizeMessage(message), throwable)
            } else {
                Log.i(TAG, sanitizeMessage(message))
            }
        }
    }
    
    /**
     * Log warning messages (only in debug builds)
     */
    fun w(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.w(TAG, sanitizeMessage(message), throwable)
            } else {
                Log.w(TAG, sanitizeMessage(message))
            }
        }
    }
    
    /**
     * Log error messages
     * - In debug: Logs to Logcat
     * - In release: Sends to Firebase Crashlytics
     */
    fun e(message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitizeMessage(message)
        
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(TAG, sanitizedMessage, throwable)
            } else {
                Log.e(TAG, sanitizedMessage)
            }
        } else {
            // In release, send to Crashlytics
            try {
                FirebaseCrashlytics.getInstance().log(sanitizedMessage)
                throwable?.let { 
                    FirebaseCrashlytics.getInstance().recordException(it)
                }
            } catch (e: Exception) {
                // If Crashlytics fails, silently fail (don't crash the app)
            }
        }
    }
    
    /**
     * Sanitize log messages to remove sensitive information
     */
    private fun sanitizeMessage(message: String): String {
        var sanitized = message
        
        // Remove API keys (basic pattern matching)
        sanitized = sanitized.replace(Regex("AIza[0-9A-Za-z_-]{35}"), "API_KEY_REDACTED")
        
        // Remove PINs/passwords (6+ digit sequences)
        sanitized = sanitized.replace(Regex("\\b\\d{6,}\\b"), "PIN_REDACTED")
        
        // Remove email addresses
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "EMAIL_REDACTED")
        
        return sanitized
    }
}

