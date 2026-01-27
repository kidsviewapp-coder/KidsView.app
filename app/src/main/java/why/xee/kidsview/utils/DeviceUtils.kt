package why.xee.kidsview.utils

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Utility object for device-specific configurations
 */
object DeviceUtils {
    
    /**
     * Determines the number of grid columns based on screen width
     * - Phones (< 600dp): 2 columns
     * - Tablets (600-840dp): 3 columns
     * - Large Tablets (> 840dp): 4 columns
     */
    @Composable
    fun getAdaptiveGridColumns(): GridCells {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        
        return when {
            screenWidthDp < 600 -> GridCells.Fixed(2)  // Phones
            screenWidthDp < 840 -> GridCells.Fixed(3)  // Small tablets
            else -> GridCells.Fixed(4)                 // Large tablets
        }
    }
    
    /**
     * Returns whether the device is a tablet (width >= 600dp)
     */
    @Composable
    fun isTablet(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp >= 600
    }
    
    /**
     * Returns the screen width category
     */
    @Composable
    fun getScreenCategory(): ScreenCategory {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        
        return when {
            screenWidthDp < 600 -> ScreenCategory.PHONE
            screenWidthDp < 840 -> ScreenCategory.TABLET
            else -> ScreenCategory.LARGE_TABLET
        }
    }
    
    /**
     * Modifier to constrain content width on tablets for better readability
     */
    @Composable
    fun Modifier.tabletContentWidth(): Modifier {
        val isTablet = isTablet()
        return if (isTablet) {
            this.widthIn(max = 1200.dp)  // Max width for content on tablets
        } else {
            this
        }
    }
}

enum class ScreenCategory {
    PHONE,
    TABLET,
    LARGE_TABLET
}

