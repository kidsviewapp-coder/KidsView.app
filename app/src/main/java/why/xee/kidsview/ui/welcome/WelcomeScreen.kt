package why.xee.kidsview.ui.welcome

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import coil.compose.AsyncImage
import coil.request.ImageRequest
import why.xee.kidsview.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.data.preferences.AuthMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import why.xee.kidsview.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import why.xee.kidsview.ui.components.InstructionsDialog

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onParentMode: () -> Unit,
    onKidsMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Start with false to ensure loading screen shows first
    var isContentReady by remember { mutableStateOf(false) }
    val preferencesManager = remember { 
        PreferencesManager(context.applicationContext)
    }
    
    var imageLoaded by remember { mutableStateOf(false) }
    
    // Check if initial setup is required - use mutableStateOf so it can update after setup
    var isAuthSet by remember { mutableStateOf(preferencesManager.isAuthSet()) }
    var hasSecurityQuestions by remember { mutableStateOf(preferencesManager.hasSecurityQuestionsSet()) }
    var showSetupDialog by remember { mutableStateOf(false) }
    
    // Check if instructions should be shown (show for new version even if app was already installed)
    var showInstructionsDialog by remember { mutableStateOf(false) }
    
    // Update auth status when content is ready
    LaunchedEffect(isContentReady) {
        if (isContentReady) {
            isAuthSet = preferencesManager.isAuthSet()
            hasSecurityQuestions = preferencesManager.hasSecurityQuestionsSet()
            if (!isAuthSet || !hasSecurityQuestions) {
                showSetupDialog = true
            } else {
                // User is already set up - check if instructions should be shown for new version
                val currentVersionCode = BuildConfig.VERSION_CODE
                if (!preferencesManager.hasInstructionsBeenShownForVersion(currentVersionCode)) {
                    kotlinx.coroutines.delay(500) // Small delay after content is ready
                    showInstructionsDialog = true
                    preferencesManager.markInstructionsShownForVersion(currentVersionCode)
                }
            }
        }
    }
    
    // Refresh state when dialog closes to ensure buttons are visible
    LaunchedEffect(showSetupDialog) {
        if (!showSetupDialog) {
            // Refresh state after dialog is dismissed/completed
            isAuthSet = preferencesManager.isAuthSet()
            hasSecurityQuestions = preferencesManager.hasSecurityQuestionsSet()
        }
    }
    
    // Show loading screen first - wait for everything to be ready
    LaunchedEffect(Unit) {
        // Wait enough time for all initialization and layout - longer delay
        kotlinx.coroutines.delay(2000) // Increased delay to prevent any structure flash
        isContentReady = true
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer // Match loading screen background
    ) {
        // Use when to completely prevent content composition until ready
        when {
            !isContentReady -> {
                // Show ONLY loading screen - nothing else is composed or measured
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    // Version and Build info at bottom left
                    Text(
                        text = "App Version ${BuildConfig.VERSION_NAME}(Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }
            else -> {
                // Content only composed when ready - no layout measurement until now
                // Always show content when ready, regardless of setup dialog state
                Box(modifier = Modifier.fillMaxSize()) {
                // Blurred Background Image Layer
                val imageUri = android.net.Uri.parse(
                    "android.resource://${context.packageName}/${R.drawable.kidsview}"
                )
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(false) // Disable crossfade to prevent structure flash
                        .build(),
                    contentDescription = null,
                    onSuccess = { 
                        imageLoaded = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 25.dp)
                        .alpha(if (imageLoaded) 1f else 0f),
                    contentScale = ContentScale.Fit // Maintains aspect ratio, prevents stretching in landscape
                )
                
                // Dark overlay for better readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
                
                // Content Layer (Video + Menu)
                val configuration = LocalConfiguration.current
                val isTablet = configuration.screenWidthDp >= 600
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = if (isTablet) Arrangement.Center else Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Welcome to KidsView",
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Kid-Friendly. Parent-Approved.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Welcome Video Player
                        if (isTablet) {
                            // Tablet: Video with buttons overlay
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .aspectRatio(4f / 3f), // Taller aspect ratio for tablets
                                shape = RoundedCornerShape(36.dp),
                                shadowElevation = 12.dp,
                                tonalElevation = 4.dp,
                                color = Color.Black.copy(alpha = 0.3f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(36.dp))
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            VideoView(ctx).apply {
                                                val videoUri = Uri.parse(
                                                    "android.resource://${ctx.packageName}/${R.raw.kidsview}"
                                                )
                                                
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                
                                                setVideoURI(videoUri)
                                                
                                                setOnPreparedListener { mediaPlayer ->
                                                    mediaPlayer.isLooping = false
                                                    mediaPlayer.setVolume(0f, 0f)
                                                    mediaPlayer.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                                                    start()
                                                }
                                                
                                                setOnErrorListener { _, what, extra ->
                                                    why.xee.kidsview.utils.AppLogger.e("Video error: what=$what, extra=$extra")
                                                    false
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    // Buttons Overlay on Video (Tablet only)
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 24.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.7f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Kids Mode Button
                                        Button(
                                            onClick = onKidsMode,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 32.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                                        ) {
                                            Text(
                                                text = "👶 Kids Mode",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                        // Parent Mode Button
                                        Button(
                                            onClick = {
                                                val currentAuthSet = preferencesManager.isAuthSet()
                                                val currentSecuritySet = preferencesManager.hasSecurityQuestionsSet()
                                                if (!currentAuthSet || !currentSecuritySet) {
                                                    isAuthSet = currentAuthSet
                                                    hasSecurityQuestions = currentSecuritySet
                                                    showSetupDialog = true
                                                } else {
                                                    onParentMode()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 32.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                                        ) {
                                            Text(
                                                text = "👨‍👩‍👧 Parent Mode",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center
                                            )
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                    }
                                }
                            }
                        } else {
                            // Mobile: Original layout with video and buttons separate
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f),
                                shape = RoundedCornerShape(36.dp),
                                shadowElevation = 12.dp,
                                tonalElevation = 4.dp,
                                color = Color.Black.copy(alpha = 0.3f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(36.dp))
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            VideoView(ctx).apply {
                                                val videoUri = Uri.parse(
                                                    "android.resource://${ctx.packageName}/${R.raw.kidsview}"
                                                )
                                                
                                                setVideoURI(videoUri)
                                                
                                                setOnPreparedListener { mediaPlayer ->
                                                    mediaPlayer.isLooping = false
                                                    mediaPlayer.setVolume(0f, 0f)
                                                    start()
                                                }
                                                
                                                setOnErrorListener { _, what, extra ->
                                                    why.xee.kidsview.utils.AppLogger.e("Video error: what=$what, extra=$extra")
                                                    false
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Buttons below video (Mobile only)
                    if (!isTablet) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Kids Mode Button
                            Button(
                                onClick = onKidsMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Text(
                                    text = "👶 Kids Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Parent Mode Button
                            Button(
                                onClick = {
                                    val currentAuthSet = preferencesManager.isAuthSet()
                                    val currentSecuritySet = preferencesManager.hasSecurityQuestionsSet()
                                    if (!currentAuthSet || !currentSecuritySet) {
                                        isAuthSet = currentAuthSet
                                        hasSecurityQuestions = currentSecuritySet
                                        showSetupDialog = true
                                    } else {
                                        onParentMode()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Text(
                                    text = "👨‍👩‍👧 Parent Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                
                // Version and Build info at bottom left - use Box with contentAlignment
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = "App Version ${BuildConfig.VERSION_NAME}(Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            }
        }
        
        // Instructions Dialog (shown on app start for new versions)
        if (showInstructionsDialog) {
            InstructionsDialog(
                onDismiss = { showInstructionsDialog = false }
            )
        }
        
        // Initial Setup Dialog
        if (showSetupDialog) {
            InitialSetupDialog(
                preferencesManager = preferencesManager,
                onDismiss = { 
                    showSetupDialog = false
                    // Refresh state after dialog is dismissed
                    scope.launch {
                        delay(100) // Ensure preferences are saved
                        isAuthSet = preferencesManager.isAuthSet()
                        hasSecurityQuestions = preferencesManager.hasSecurityQuestionsSet()
                    }
                },
                onSetupComplete = {
                    showSetupDialog = false
                    // Refresh state after setup completes - ensure buttons become visible
                    scope.launch {
                        delay(100) // Ensure preferences are saved
                        isAuthSet = preferencesManager.isAuthSet()
                        hasSecurityQuestions = preferencesManager.hasSecurityQuestionsSet()
                        
                        // Show instructions dialog after setup is complete (for new versions)
                        val currentVersionCode = BuildConfig.VERSION_CODE
                        if (!preferencesManager.hasInstructionsBeenShownForVersion(currentVersionCode)) {
                            delay(500) // Small delay after setup
                            showInstructionsDialog = true
                            // Mark as shown for this version
                            preferencesManager.markInstructionsShownForVersion(currentVersionCode)
                        }
                    }
                }
            )
        }
    }
}

/**
 * Animated wave text effect
 */
@Composable
fun WaveText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        text.forEachIndexed { index, char ->
            val offset = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1000,
                        delayMillis = index * 100,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_$index"
            )
            
            Text(
                text = char.toString(),
                style = style,
                color = color,
                textAlign = textAlign,
                modifier = Modifier
                    .graphicsLayer {
                        translationY = offset.value
                        alpha = 0.9f + (offset.value / 50f) // Subtle alpha change
                    }
            )
        }
    }
}

