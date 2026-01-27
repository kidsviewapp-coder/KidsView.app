package why.xee.kidsview

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import why.xee.kidsview.utils.ReviewerUnlockManager

/**
 * Application class for Hilt dependency injection
 */
@HiltAndroidApp
class KidsViewApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Reset reviewer unlock flag on app start (ensures clean state)
        ReviewerUnlockManager.reset()
    }
}

