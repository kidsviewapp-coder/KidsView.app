package why.xee.kidsview.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

// Local composition for theme name
val LocalThemeName = compositionLocalOf { "system" }

@Composable
fun KidsViewTheme(
    themeName: String = "system",
    content: @Composable () -> Unit
) {
    val colorScheme = PremiumThemeManager.getTheme(themeName)

    CompositionLocalProvider(LocalThemeName provides themeName) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}