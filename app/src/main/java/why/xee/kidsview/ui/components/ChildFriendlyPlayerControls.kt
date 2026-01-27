package why.xee.kidsview.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * Child-friendly custom player controls overlay
 * Features: Large buttons, vibrant colors, progress bar, next video button
 */
@Composable
fun ChildFriendlyPlayerControls(
    isPlaying: Boolean,
    currentTime: Float, // in seconds
    duration: Float, // in seconds
    hasPreviousVideo: Boolean,
    hasNextVideo: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousVideoClick: () -> Unit,
    onNextVideoClick: () -> Unit,
    onBackClick: () -> Unit,
    onSeek: ((Float) -> Unit)? = null, // Callback for seeking to a specific time
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
    onUnlockControlsClick: (() -> Unit)? = null, // Optional unlock controls callback
    unlockRemainingSeconds: Int = 0, // Remaining seconds for unlock timer
    isParentMode: Boolean = true // Whether app is in parent mode (default true for backward compatibility)
) {
    // In kids mode: hide all buttons while playing, show on tap or when paused
    // In parent mode: ALWAYS show buttons - never hide them
    var showControlsInKidsMode by remember { mutableStateOf(!isPlaying || isParentMode) }
    
    // Toggle controls visibility on tap (kids mode only, when playing)
    // CRITICAL: In parent mode, controls are ALWAYS visible
    LaunchedEffect(isPlaying, isParentMode) {
        if (isParentMode) {
            // Parent mode: Always show controls - never hide
            showControlsInKidsMode = true
        } else if (isPlaying) {
            // Kids mode when playing: hide controls
            showControlsInKidsMode = false
        } else {
            // Kids mode when paused: show controls
            showControlsInKidsMode = true
        }
    }
    
    // Auto-hide controls after 2 seconds when shown via tap in kids mode
    LaunchedEffect(showControlsInKidsMode, isPlaying, isParentMode) {
        // Only auto-hide in kids mode when playing and controls are shown
        if (!isParentMode && isPlaying && showControlsInKidsMode) {
            delay(2000) // Wait 2 seconds
            // Only hide if still playing and controls are still shown (user didn't interact)
            if (isPlaying && showControlsInKidsMode) {
                showControlsInKidsMode = false
            }
        }
    }
    
    // Always show controls (no animation, always visible)
    // Use high zIndex to ensure buttons are above all overlays
    // The overlay no longer has clickable, so buttons will receive touches
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(2f) // Higher than overlay (0.9f) to ensure clicks work
            .then(
                // In kids mode when playing, make entire area tappable to show controls
                if (!isParentMode && isPlaying && !showControlsInKidsMode) {
                    Modifier.clickable { showControlsInKidsMode = true }
                } else {
                    Modifier
                }
            )
    ) {
        // Top buttons row: Back button and Unlock Controls button
        // In parent mode: Only show back button (let YouTube controls handle the rest)
        // In kids mode: hide when playing (unless controls are shown)
        if (isParentMode) {
            // Parent mode: Only back button
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp)
                    .zIndex(10f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button - only control in parent mode
                BackButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else if (showControlsInKidsMode) {
            // Kids mode: Show back button and unlock button when controls are visible
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 16.dp)
                    .zIndex(10f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                BackButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                )
                
                // Unlock Controls button (only show if callback is provided)
                if (onUnlockControlsClick != null) {
                    UnlockControlsButton(
                        onClick = onUnlockControlsClick,
                        remainingSeconds = unlockRemainingSeconds,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        
        // Seek bar positioned at bottom, directly below YouTube's native seek bar (touching but not overlapping)
        // YouTube's native controls are typically ~48dp from bottom, so we position our seek bar just above them
        // In parent mode: Don't show custom seek bar (use YouTube's native controls)
        // In kids mode: hide when playing (unless controls are shown)
        if (!isParentMode && showControlsInKidsMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 40.dp), // Positioned right above YouTube's native controls (minimal spacing)
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Progress bar with time indicator - positioned parallel to and touching YouTube's native seek bar
                if (duration > 0) {
                    CustomProgressBar(
                        progress = currentTime / duration,
                        currentTime = currentTime,
                        totalTime = duration,
                        onSeek = onSeek, // Pass seek callback
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Control buttons positioned on the right side of screen
        // Use high zIndex to ensure buttons are above everything and can receive touches
        // In parent mode: Don't show custom controls (use YouTube's native controls)
        // In kids mode: hide all buttons when playing (unless controls are shown via tap)
        if (!isParentMode && showControlsInKidsMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp) // At the right edge of screen
                    .zIndex(10f), // Very high zIndex to ensure buttons are above all overlays
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Previous Video button
                // In kids mode: show when controls are visible (via tap or when paused)
                // In parent mode: always visible
                if (isParentMode || showControlsInKidsMode) {
                    PreviousVideoButton(
                        onClick = onPreviousVideoClick,
                        modifier = Modifier.size(48.dp),
                        enabled = hasPreviousVideo
                    )
                }
                
                // Play/Pause button
                // Always visible when controls are shown
                PlayPauseButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(56.dp)
                )
                
                // Next Video button
                // In kids mode: show when controls are visible (via tap or when paused)
                // In parent mode: always visible
                if (isParentMode || showControlsInKidsMode) {
                    NextVideoButton(
                        onClick = onNextVideoClick,
                        modifier = Modifier.size(48.dp),
                        enabled = hasNextVideo
                    )
                }
            }
        }
    }
}

/**
 * Large, colorful Play/Pause button with scale animation on click
 */
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ), label = "buttonScale"
    )
    val scope = rememberCoroutineScope()
    
    FloatingActionButton(
        onClick = {
            isPressed = true
            onClick()
            scope.launch {
                delay(200) // Animation duration
                isPressed = false
            }
        },
        modifier = modifier.scale(scale),
        containerColor = Color(0xFFFF6B6B), // Bright red/coral
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 3.dp
        ),
        shape = CircleShape
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = Modifier.size(26.dp), // Adjusted for 52dp button
            tint = Color.White
        )
    }
}

/**
 * Back button to return to video list with scale animation on click
 */
@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ), label = "buttonScale"
    )
    val scope = rememberCoroutineScope()
    
    FloatingActionButton(
        onClick = {
            isPressed = true
            onClick()
            scope.launch {
                delay(200) // Animation duration
                isPressed = false
            }
        },
        modifier = modifier.scale(scale),
        containerColor = Color(0xFF6C757D), // Gray
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 3.dp
        ),
        shape = CircleShape
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
    }
}

/**
 * Previous Video button with playful design and scale animation on click
 */
@Composable
private fun PreviousVideoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ), label = "buttonScale"
    )
    val scope = rememberCoroutineScope()
    
    // Use Box with solid background instead of FloatingActionButton to ensure full opacity
    Box(
        modifier = modifier
            .background(
                color = if (enabled) Color(0xFF4ECDC4) else Color(0xFF6C757D).copy(alpha = 0.5f),
                shape = CircleShape
            )
            .scale(scale)
            .clickable(enabled = enabled) {
                android.util.Log.d("PreviousButton", "Clicked! enabled=$enabled")
                if (enabled) {
                    isPressed = true
                    onClick()
                    scope.launch {
                        delay(200) // Animation duration
                        isPressed = false
                    }
                } else {
                    android.util.Log.w("PreviousButton", "Button clicked but disabled!")
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SkipPrevious,
            contentDescription = "Previous Video",
            modifier = Modifier
                .size(24.dp)
                .alpha(if (enabled) 1f else 0.5f),
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * Next Video button with playful design and scale animation on click
 */
@Composable
private fun NextVideoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ), label = "buttonScale"
    )
    val scope = rememberCoroutineScope()
    
    // Use Box with solid background instead of FloatingActionButton to ensure full opacity
    Box(
        modifier = modifier
            .background(
                color = if (enabled) Color(0xFF4ECDC4) else Color(0xFF6C757D).copy(alpha = 0.5f),
                shape = CircleShape
            )
            .scale(scale)
            .clickable(enabled = enabled) {
                android.util.Log.d("NextButton", "Clicked! enabled=$enabled")
                if (enabled) {
                    isPressed = true
                    onClick()
                    scope.launch {
                        delay(200) // Animation duration
                        isPressed = false
                    }
                } else {
                    android.util.Log.w("NextButton", "Button clicked but disabled!")
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next Video",
            modifier = Modifier
                .size(24.dp)
                .alpha(if (enabled) 1f else 0.5f),
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}

/**
 * Custom progress bar with time indicator
 * Child-friendly design with large touch target
 */
@Composable
private fun CustomProgressBar(
    progress: Float, // 0.0 to 1.0
    currentTime: Float, // in seconds
    totalTime: Float, // in seconds
    onSeek: ((Float) -> Unit)? = null, // Callback for seeking
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(progress) { mutableStateOf(progress) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentTime),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(totalTime),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Interactive Slider for seeking (if onSeek is provided)
        if (onSeek != null && totalTime > 0) {
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    sliderValue = newValue
                },
                onValueChangeFinished = {
                    // Seek to the selected position when user releases
                    val seekTime = sliderValue * totalTime
                    onSeek(seekTime)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent, // We'll use custom gradient overlay
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        } else {
            // Non-interactive progress bar (fallback)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF6B6B), // Bright red
                                    Color(0xFFFFD93D)  // Bright yellow
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Unlock Controls button to temporarily unlock YouTube controls
 */
@Composable
private fun UnlockControlsButton(
    onClick: () -> Unit,
    remainingSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ), label = "buttonScale"
    )
    val scope = rememberCoroutineScope()
    
    FloatingActionButton(
        onClick = {
            android.util.Log.d("UnlockButton", "Button clicked! remainingSeconds=$remainingSeconds")
            isPressed = true
            onClick()
            scope.launch {
                delay(200) // Animation duration
                isPressed = false
            }
        },
        modifier = modifier.scale(scale),
        containerColor = if (remainingSeconds > 0) Color(0xFFFF9800) else Color(0xFF4ECDC4), // Orange when unlocked, teal when locked
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 3.dp
        ),
        shape = CircleShape
    ) {
        if (remainingSeconds > 0) {
            // Show countdown timer when unlocked
            Text(
                text = remainingSeconds.toString(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            // Show lock icon when locked
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = "Unlock YouTube Controls",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * Format time in seconds to MM:SS or HH:MM:SS format
 */
private fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

