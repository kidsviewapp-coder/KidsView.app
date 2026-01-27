package why.xee.kidsview.ui.welcome

import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import why.xee.kidsview.R
import why.xee.kidsview.data.preferences.PreferencesManager
import java.util.Calendar

/**
 * New Year Splash Screen
 * Shows a New Year video during January (until January 31 at midnight)
 * Video loops continuously with a skip button in landscape orientation
 */
@Composable
fun NewYearSplashScreen(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferencesManager = remember { PreferencesManager(context.applicationContext) }
    var videoView: VideoView? by remember { mutableStateOf(null) }
    var isVideoReady by remember { mutableStateOf(false) }
    
    // Check if user has disabled the splash
    val isDisabled = remember { preferencesManager.isNewYearSplashDisabled() }
    
    // Check if we should show the New Year splash
    // Show from today until January 31 at midnight
    val shouldShow = remember {
        if (isDisabled) return@remember false
        
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-11, where 0 is January
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Show from today onwards - for testing, show it always (if not disabled)
        // In production, you can restrict this to: isJanuary || isDecember
        // This allows testing from today regardless of the current date
        true
    }
    
    // Set landscape orientation when video is shown
    LaunchedEffect(shouldShow) {
        if (shouldShow) {
            val activity = context as? android.app.Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    
    // Restore orientation when leaving
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                val activity = context as? android.app.Activity
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val activity = context as? android.app.Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    
    if (!shouldShow) {
        // If not January, skip immediately
        LaunchedEffect(Unit) {
            onSkip()
        }
        return
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoView = this
                    val videoUri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.new_year}")
                    
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    setVideoURI(videoUri)
                    
                    setOnPreparedListener { mediaPlayer ->
                        isVideoReady = true
                        mediaPlayer.isLooping = true
                        mediaPlayer.start()
                    }
                    
                    setOnErrorListener { _, _, _ ->
                        // If video fails to load, skip
                        onSkip()
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (!isVideoReady && view.isPlaying) {
                    isVideoReady = true
                }
            }
        )
        
        // Buttons Row - Skip and Don't Show Again
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Don't Show Again Button - Skip video once and for all
            Button(
                onClick = {
                    preferencesManager.setNewYearSplashDisabled(true)
                    videoView?.stopPlayback()
                    onSkip()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    text = "Don't Show Again",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Skip Button
            Button(
                onClick = {
                    videoView?.stopPlayback()
                    onSkip()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

