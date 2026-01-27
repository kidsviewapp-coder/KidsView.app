package why.xee.kidsview.ui.parentmode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import why.xee.kidsview.data.preferences.AuthMode
import why.xee.kidsview.data.preferences.PreferencesManager
import why.xee.kidsview.ui.components.AdManager as KotlinAdManager
import why.xee.kidsview.utils.AdManager
import why.xee.kidsview.ui.components.AnimatedSuccessMessage
import why.xee.kidsview.ui.components.ElegantAlertDialog
import why.xee.kidsview.ui.theme.PremiumThemeManager
import why.xee.kidsview.ui.viewmodel.ParentSettingsViewModel
import why.xee.kidsview.ui.viewmodel.ReviewTrackingViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import why.xee.kidsview.utils.ReviewManager
import why.xee.kidsview.utils.ReviewerUnlockManager
import why.xee.kidsview.utils.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettingsScreenComposable(
    onNavigateBack: () -> Unit,
    onThemeChange: (String) -> Unit = {},
    onNavigateToSecurityQuestionsSetup: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    viewModel: ParentSettingsViewModel = hiltViewModel()
) {
    val preferencesManager = viewModel.preferencesManager
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTheme by remember { mutableStateOf(preferencesManager.getSelectedTheme()) }
    var authMode by remember { mutableStateOf(preferencesManager.getAuthMode()) }
    var isAppLocked by remember { mutableStateOf(preferencesManager.isAppLocked()) }
    var timeLimitEnabled by remember { mutableStateOf(preferencesManager.isTimeLimitEnabled()) }
    var timeLimitMinutes by remember { mutableStateOf(preferencesManager.getBaseTimeLimitMinutes()) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showTimeResetInfoDialog by remember { mutableStateOf(false) }
    var showWalletErrorDialog by remember { mutableStateOf(false) }
    var conflictingTimeHours by remember { mutableStateOf(0) }
    var conflictingTimeMinutes by remember { mutableStateOf(0) }
    
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    // Earned time wallet state (accessible throughout the composable)
    var totalEarnedTime by remember { mutableStateOf(preferencesManager.getTotalEarnedTimeMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            totalEarnedTime = preferencesManager.getTotalEarnedTimeMinutes()
        }
    }
    
    // Ad readiness indicator state
    var isRewardedAdReady by remember { mutableStateOf(AdManager.getInstance().isRewardedAdReady()) }
    var adLoadingStartTime by remember { mutableStateOf(0L) }
    var loadingElapsedSeconds by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        AdManager.getInstance().loadInterstitialAd(context, true)
        if (!AdManager.getInstance().isRewardedAdCurrentlyLoading() && !AdManager.getInstance().isRewardedAdReady()) {
            AdManager.getInstance().loadRewardedAd(context, true)
            adLoadingStartTime = System.currentTimeMillis()
        }
    }
    
    // Poll ad readiness status every 500ms
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val wasReady = isRewardedAdReady
            isRewardedAdReady = AdManager.getInstance().isRewardedAdReady()
            
            if (isRewardedAdReady) {
                // Ad is ready - reset loading timer
                adLoadingStartTime = 0L
                loadingElapsedSeconds = 0
            } else if (AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                // Ad is loading - track time
                if (adLoadingStartTime == 0L) {
                    adLoadingStartTime = System.currentTimeMillis()
                }
                val elapsed = (System.currentTimeMillis() - adLoadingStartTime) / 1000
                loadingElapsedSeconds = elapsed.toInt()
            } else {
                // Ad not loading and not ready - reset timer
                adLoadingStartTime = 0L
                loadingElapsedSeconds = 0
            }
        }
    }
    
    fun showSuccess(msg: String) {
        successMessage = msg
        showSuccessMessage = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text("Parent Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Support Us Section (moved to top)
            SettingSection(
                title = "Support Us",
                icon = Icons.Default.Favorite,
                description = "You can support the development team through ads by clicking this button. You can watch multiple ads to support the development team."
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Support Development",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "You can support the development team through ads by clicking this button.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "You can watch this ad more than once if you wish to support development.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Cooldown state for support us button
                        var supportUsCooldownRemaining by remember { mutableStateOf(0L) }
                        
                        // Update cooldown counter every second
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(1000)
                                supportUsCooldownRemaining = AdManager.getInstance().getRewardedAdCooldownRemainingSeconds(context)
                            }
                        }
                        
                        val canShowSupportAd = AdManager.getInstance().canShowRewardedAd(context)
                        val buttonText = if (canShowSupportAd) {
                            "Support Us"
                        } else {
                            "Support Us (${supportUsCooldownRemaining}s)"
                        }
                        
                        Button(
                            onClick = {
                                if (!canShowSupportAd) {
                                    // Show cooldown message
                                    showSuccess("Ad cooldown: ${supportUsCooldownRemaining} seconds remaining")
                                    return@Button
                                }
                                
                                // Always call showRewardedAd - it handles loading if needed
                                if (!AdManager.getInstance().isRewardedAdReady()) {
                                    // Ad not ready - try to load it and wait
                                    if (!AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                                        AdManager.getInstance().loadRewardedAd(context, true)
                                    }
                                    coroutineScope.launch {
                                        var attempts = 0
                                        val maxAttempts = 20 // Wait up to 10 seconds
                                        while (!AdManager.getInstance().isRewardedAdReady() && attempts < maxAttempts) {
                                            delay(500)
                                            attempts++
                                        }
                                        
                                        // Record cooldown and show ad
                                        AdManager.getInstance().recordRewardedAdShown(context)
                                        var rewardGranted = false
                                        AdManager.getInstance().showRewardedAd(
                                            context,
                                            true,
                                            {
                                                rewardGranted = true
                                                showSuccess("Thank you for your support")
                                            },
                                            {
                                                if (!rewardGranted) {
                                                if (attempts >= maxAttempts) {
                                                    showSuccess("Ad is taking longer to load. Please try again in a moment.")
                                                } else {
                                                    showSuccess("Ad not completed")
                                                    }
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    // Ad ready - show it immediately
                                    AdManager.getInstance().recordRewardedAdShown(context)
                                    var rewardGranted = false
                                    AdManager.getInstance().showRewardedAd(
                                        context,
                                        true,
                                        {
                                            rewardGranted = true
                                            showSuccess("Thank you for your support")
                                        },
                                        {
                                            if (!rewardGranted) {
                                            showSuccess("Ad not completed")
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canShowSupportAd) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(buttonText)
                            }
                        }
                    }
                }
            }
            
            // Theme Section
            SettingSection(
                title = "Theme",
                icon = Icons.Default.Settings,
                description = "Choose a beautiful theme for the app. Changes apply immediately across all screens."
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Select Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Sort themes: system first, then others alphabetically
                        val sortedThemes = PremiumThemeManager.themes.keys.sortedWith { a, b ->
                            when {
                                a == "system" -> -1
                                b == "system" -> 1
                                else -> a.compareTo(b)
                            }
                        }
                        sortedThemes.forEach { themeName ->
                            val isUnlocked = preferencesManager.isThemeUnlocked(themeName)
                            val isSelected = selectedTheme == themeName
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isUnlocked) {
                                            // Theme is unlocked - show interstitial ad when selecting
                                            AdManager.getInstance().showInterstitialAd(context, true, false) {
                                                selectedTheme = themeName
                                                preferencesManager.setSelectedTheme(themeName)
                                                onThemeChange(themeName)
                                                showSuccess("Theme changed to ${PremiumThemeManager.getThemeName(themeName)}")
                                            }
                                        } else {
                                            // Check reviewer mode - if active, skip ad and unlock immediately
                                            if (ReviewerUnlockManager.isUnlocked()) {
                                                preferencesManager.unlockTheme(themeName)
                                                selectedTheme = themeName
                                                preferencesManager.setSelectedTheme(themeName)
                                                onThemeChange(themeName)
                                                showSuccess("${PremiumThemeManager.getThemeName(themeName)} theme unlocked!")
                                                return@clickable
                                            }
                                            
                                            // Theme is locked - show rewarded ad to unlock
                                            if (!AdManager.getInstance().isRewardedAdReady()) {
                                                // Ad not ready - try to load it and wait
                                                if (!AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                                                    AdManager.getInstance().loadRewardedAd(context, true)
                                                }
                                                coroutineScope.launch {
                                                    var attempts = 0
                                                    val maxAttempts = 20
                                                    while (!AdManager.getInstance().isRewardedAdReady() && attempts < maxAttempts) {
                                                        delay(500)
                                                        attempts++
                                                    }
                                                    
                                                    var rewardGranted = false
                                                    AdManager.getInstance().showRewardedAd(
                                                        context,
                                                        true,
                                                        {
                                                            rewardGranted = true
                                                            preferencesManager.unlockTheme(themeName)
                                                            selectedTheme = themeName
                                                            preferencesManager.setSelectedTheme(themeName)
                                                            onThemeChange(themeName)
                                                            showSuccess("${PremiumThemeManager.getThemeName(themeName)} theme unlocked!")
                                                        },
                                                        {
                                                            if (!rewardGranted) {
                                                            if (attempts >= maxAttempts) {
                                                                showSuccess("Ad is taking longer to load. Please try again.")
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            } else {
                                                var rewardGranted = false
                                                AdManager.getInstance().showRewardedAd(
                                                    context,
                                                    true,
                                                    {
                                                        rewardGranted = true
                                                        preferencesManager.unlockTheme(themeName)
                                                        selectedTheme = themeName
                                                        preferencesManager.setSelectedTheme(themeName)
                                                        onThemeChange(themeName)
                                                        showSuccess("${PremiumThemeManager.getThemeName(themeName)} theme unlocked!")
                                                    },
                                                    {
                                                        // Ad was dismissed - only show error if reward wasn't granted
                                                        // (rewardGranted check not needed here as it's just a silent dismiss)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left side: Theme name
                                Text(
                                    PremiumThemeManager.getThemeName(themeName),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isUnlocked) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    }
                                )
                                
                                // Right side: Lock icon + "Watch ad" text OR Selected checkmark
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isUnlocked) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Locked - Watch ad to unlock",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Watch ad",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Time Limit Section
            SettingSection(
                title = "Time Limit",
                icon = Icons.Default.Settings,
                description = "Set daily time limits for video watching in Kids Mode. Timer only counts when videos are playing. Resets daily at 12:00am (midnight)."
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Enable Time Limit",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Set daily usage time for kids",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                                Switch(
                                    checked = timeLimitEnabled,
                                    onCheckedChange = { newValue ->
                                        // Show ad when time limit is enabled or disabled (with cooldown)
                                        AdManager.getInstance().showInterstitialAd(context, true, false) {
                                            timeLimitEnabled = newValue
                                            preferencesManager.setTimeLimitEnabled(newValue)
                                            showSuccess(if (newValue) "Time limit enabled" else "Time limit disabled")
                                        }
                                    }
                                )
                        }
                        if (timeLimitEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val hours = timeLimitMinutes / 60
                            val minutes = timeLimitMinutes % 60
                            val timeLimitDisplay = String.format("%02d:%02d", hours, minutes)
                            
                            OutlinedTextField(
                                value = timeLimitDisplay,
                                onValueChange = { },
                                readOnly = true,
                                enabled = false,
                                label = { Text("Daily Time Limit (HH:MM)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePickerDialog = true },
                                trailingIcon = {
                                    IconButton(onClick = { showTimePickerDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Set Time")
                                    }
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Refresh time usage and limit display - use state to update when bonus time is added
                            var timeUsedDisplay by remember { mutableStateOf("") }
                            var effectiveDisplay by remember { mutableStateOf("") }
                            
                            // Update display values initially and when time limit changes
                            LaunchedEffect(timeLimitEnabled, timeLimitMinutes) {
                                val timeUsed = preferencesManager.getTimeUsedToday()
                                // Convert milliseconds to minutes, ensuring we don't have overflow issues
                                // Cap at reasonable maximum (e.g., 24 hours = 1440 minutes) to prevent display bugs
                                val timeUsedMinutes = ((timeUsed / (60 * 1000)).toInt()).coerceIn(0, 1440)
                                val timeUsedHours = timeUsedMinutes / 60
                                val timeUsedMins = timeUsedMinutes % 60
                                timeUsedDisplay = String.format("%02d:%02d", timeUsedHours, timeUsedMins)

                                // Effective limit includes any bonus minutes granted via rewarded ads
                                val effectiveLimitMinutes = preferencesManager.getTimeLimitMinutes()
                                val effectiveHours = effectiveLimitMinutes / 60
                                val effectiveMins = effectiveLimitMinutes % 60
                                effectiveDisplay = String.format("%02d:%02d", effectiveHours, effectiveMins)
                            }
                            
                            // Poll for updates every second to refresh after bonus time is added
                            LaunchedEffect(Unit) {
                                while (timeLimitEnabled) {
                                    delay(1000)
                                    val timeUsed = preferencesManager.getTimeUsedToday()
                                    // Convert milliseconds to minutes, ensuring we don't have overflow issues
                                    // Cap at reasonable maximum (e.g., 24 hours = 1440 minutes) to prevent display bugs
                                    val timeUsedMinutes = ((timeUsed / (60 * 1000)).toInt()).coerceIn(0, 1440)
                                    val timeUsedHours = timeUsedMinutes / 60
                                    val timeUsedMins = timeUsedMinutes % 60
                                    timeUsedDisplay = String.format("%02d:%02d", timeUsedHours, timeUsedMins)

                                    // Effective limit includes any bonus minutes granted via rewarded ads
                                    val effectiveLimitMinutes = preferencesManager.getTimeLimitMinutes()
                                    val effectiveHours = effectiveLimitMinutes / 60
                                    val effectiveMins = effectiveLimitMinutes % 60
                                    effectiveDisplay = String.format("%02d:%02d", effectiveHours, effectiveMins)
                                }
                            }

                            Text(
                                "Used today: $timeUsedDisplay / $effectiveDisplay | Wallet: ${totalEarnedTime} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Time Reset Information Button
                            OutlinedButton(
                                onClick = { showTimeResetInfoDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Learn about Time Reset & Wallet")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Display applied earned time
                            val appliedEarnedTime = preferencesManager.getTimeLimitMinutes() - preferencesManager.getBaseTimeLimitMinutes()
                            if (appliedEarnedTime > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Applied earned time: +${appliedEarnedTime} minutes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Display earned time wallet (totalEarnedTime is now at composable level)
                            if (totalEarnedTime > 0) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Earned Time Wallet: ${totalEarnedTime} minutes",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Earned time does not auto-apply. You must manually add it to the daily limit.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Button(
                                            onClick = {
                                                // Get current state
                                                val base = preferencesManager.getBaseTimeLimitMinutes()
                                                val applied = appliedEarnedTime
                                                val wallet = preferencesManager.getTotalEarnedTimeMinutes() // Get fresh wallet value
                                                
                                                // Calculate maximum that can be applied considering:
                                                // 1. What fits in 180-minute cap
                                                // 2. What's available in wallet
                                                val maxByCap = (180 - base - applied).coerceAtLeast(0)
                                                val maxByWallet = wallet
                                                
                                                // Apply the MINIMUM of what fits in cap and what's in wallet
                                                val toApply = minOf(maxByCap, maxByWallet)
                                                
                                                Log.d("ApplyWallet", "Base=$base, Applied=$applied, Wallet=$wallet, MaxByCap=$maxByCap, ToApply=$toApply")
                                                
                                                if (toApply > 0) {
                                                    if (preferencesManager.applyEarnedTimeToDailyLimit(toApply)) {
                                                        timeLimitMinutes = preferencesManager.getBaseTimeLimitMinutes()
                                                        totalEarnedTime = preferencesManager.getTotalEarnedTimeMinutes()
                                                        showSuccess("Applied $toApply minutes to daily limit")
                                                    } else {
                                                        showSuccess("Unable to apply earned time. Check wallet balance.")
                                                    }
                                                } else {
                                                    if (maxByCap <= 0) {
                                                        showSuccess("Daily limit already at maximum (180 minutes)")
                                                    } else if (maxByWallet <= 0) {
                                                        showSuccess("Wallet is empty. Watch ads to earn time.")
                                                    } else {
                                                        showSuccess("Cannot apply time")
                                                    }
                                                }
                                            },
                                            enabled = totalEarnedTime > 0 && appliedEarnedTime + preferencesManager.getBaseTimeLimitMinutes() < 180,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Apply Earned Time to Daily Limit")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            // Rewarded ad: Earn +15 minutes to wallet (does not auto-apply)
                            OutlinedButton(
                                    onClick = {
                                        // Check reviewer mode - if active, skip ad and grant reward immediately
                                        if (ReviewerUnlockManager.isUnlocked()) {
                                            preferencesManager.addEarnedTimeToWallet()
                                            totalEarnedTime = preferencesManager.getTotalEarnedTimeMinutes()
                                            showSuccess("15 minutes added to earned time wallet")
                                            return@OutlinedButton
                                        }
                                        
                                        if (!AdManager.getInstance().isRewardedAdReady()) {
                                            // Ad not ready - try to load it and wait
                                            if (!AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                                                AdManager.getInstance().loadRewardedAd(context, true)
                                                adLoadingStartTime = System.currentTimeMillis()
                                            }
                                            coroutineScope.launch {
                                                var attempts = 0
                                                val maxAttempts = 20
                                                while (!AdManager.getInstance().isRewardedAdReady() && attempts < maxAttempts) {
                                                    delay(500)
                                                    attempts++
                                                }
                                                
                                                var rewardGranted = false
                                                AdManager.getInstance().showRewardedAd(
                                                    context,
                                                    true,
                                                    {
                                                        rewardGranted = true
                                                        preferencesManager.addEarnedTimeToWallet()
                                                        totalEarnedTime = preferencesManager.getTotalEarnedTimeMinutes()
                                                        showSuccess("15 minutes added to earned time wallet")
                                                    },
                                                    {
                                                        if (!rewardGranted) {
                                                        if (attempts >= maxAttempts) {
                                                            showSuccess("Ad is taking longer to load. Please try again.")
                                                        } else {
                                                            showSuccess("Ad not completed - Earned time not added")
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        } else {
                                            // Ad ready, show it
                                            var rewardGranted = false
                                            AdManager.getInstance().showRewardedAd(
                                                context,
                                                true,
                                                {
                                                    rewardGranted = true
                                                    preferencesManager.addEarnedTimeToWallet()
                                                    totalEarnedTime = preferencesManager.getTotalEarnedTimeMinutes()
                                                    showSuccess("15 minutes added to earned time wallet")
                                                },
                                                {
                                                    if (!rewardGranted) {
                                                    showSuccess("Ad not completed - Earned time not added")
                                                    }
                                                }
                                            )
                                        }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ad readiness indicator
                                    AdReadinessIndicator(
                                        isReady = isRewardedAdReady,
                                        loadingSeconds = if (!isRewardedAdReady && AdManager.getInstance().isRewardedAdCurrentlyLoading()) loadingElapsedSeconds else null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Earn +15 minutes to wallet (Watch ad)")
                                }
                            }
                        }
                    }
                }
            }
            
            // Screen Lock Section
                SettingSection(
                    title = "Screen Lock",
                icon = Icons.Default.Lock,
                description = if (isAppLocked) 
                    "App is locked. Kids cannot exit until parent enters PIN/Password. Unlock from Kids Mode or Parent Settings."
                else
                    "When enabled, kids cannot exit the app without entering the parent PIN/Password. Prevents accidental exits."
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAppLocked) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Check if Screen Lock feature is enabled
                        var isFeatureEnabled by remember { mutableStateOf(preferencesManager.isScreenLockFeatureEnabled()) }
                        var remainingHours by remember { mutableStateOf(preferencesManager.getScreenLockFeatureRemainingHours()) }
                        
                        // Update feature status and remaining hours every minute
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(60000) // Update every minute
                                isFeatureEnabled = preferencesManager.isScreenLockFeatureEnabled()
                                remainingHours = preferencesManager.getScreenLockFeatureRemainingHours()
                            }
                        }
                        
                        if (!isFeatureEnabled) {
                            // Feature is disabled - need to watch ad to enable it
                            Text(
                                "To enable this feature, you need to watch a rewarded ad. The feature disables after 24 hours and requires watching an ad again to re-enable. Additionally, every time you toggle the lock switch ON to lock the app, you need to watch a rewarded ad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Button(
                                onClick = {
                                    // Check reviewer mode - if active, skip ad and unlock immediately
                                    if (ReviewerUnlockManager.isUnlocked()) {
                                        preferencesManager.enableScreenLockFeature()
                                        isFeatureEnabled = true
                                        remainingHours = 24
                                        showSuccess("Screen Lock feature enabled for 24 hours")
                                    } else {
                                    // Enable feature with rewarded ad
                                    if (!AdManager.getInstance().isRewardedAdReady()) {
                                        // Ad not ready - try to load it and wait
                                        if (!AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                                            AdManager.getInstance().loadRewardedAd(context, true)
                                        }
                                        coroutineScope.launch {
                                            var attempts = 0
                                            val maxAttempts = 20 // Wait up to 10 seconds
                                            while (!AdManager.getInstance().isRewardedAdReady() && attempts < maxAttempts) {
                                                delay(500)
                                                attempts++
                                            }
                                            
                                            // Try to show ad (will show if ready, or handle if not)
                                            var rewardGranted = false
                                            AdManager.getInstance().showRewardedAd(
                                                context,
                                                true,
                                                {
                                                    rewardGranted = true
                                                    preferencesManager.enableScreenLockFeature()
                                                    isFeatureEnabled = true
                                                    remainingHours = 24
                                                    showSuccess("Screen Lock feature enabled for 24 hours")
                                                },
                                                {
                                                    if (!rewardGranted) {
                                                    if (attempts >= maxAttempts) {
                                                        showSuccess("Ad is taking longer to load. Please try again in a moment.")
                                                    } else {
                                                        showSuccess("Ad not completed - Feature not enabled")
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        // Ad is ready - show it immediately
                                        var rewardGranted = false
                                        AdManager.getInstance().showRewardedAd(
                                            context,
                                            true,
                                            {
                                                rewardGranted = true
                                                preferencesManager.enableScreenLockFeature()
                                                isFeatureEnabled = true
                                                remainingHours = 24
                                                showSuccess("Screen Lock feature enabled for 24 hours")
                                            },
                                            {
                                                if (!rewardGranted) {
                                                showSuccess("Ad not completed - Feature not enabled")
                                                }
                                            }
                                        )
                                    }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enable Feature with Ad (Disables in 24h)")
                            }
                        } else {
                            // Feature is enabled - can use it normally
                            Text(
                                "Feature enabled (Disables in ${remainingHours}h). Every time you toggle the lock switch ON to lock the app, you need to watch a rewarded ad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Screen Lock Status",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isAppLocked) "🔒 App is locked" else "🔓 App is unlocked",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isAppLocked)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isAppLocked,
                                    onCheckedChange = { newValue ->
                                        if (newValue && !isAppLocked) {
                                            // Check reviewer mode - if active, skip ad and lock immediately
                                            if (ReviewerUnlockManager.isUnlocked()) {
                                                preferencesManager.setAppLocked(true)
                                                isAppLocked = true
                                                showSuccess("Screen lock enabled")
                                            } else {
                                            // Locking the app - requires rewarded ad
                                            if (!AdManager.getInstance().isRewardedAdReady()) {
                                                // Ad not ready - try to load it and wait
                                                if (!AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                                                    AdManager.getInstance().loadRewardedAd(context, true)
                                                }
                                                coroutineScope.launch {
                                                    var attempts = 0
                                                    val maxAttempts = 20 // Wait up to 10 seconds
                                                    while (!AdManager.getInstance().isRewardedAdReady() && attempts < maxAttempts) {
                                                        delay(500)
                                                        attempts++
                                                    }
                                                    
                                                    // Try to show ad (will show if ready, or handle if not)
                                                    var rewardGranted = false
                                                    AdManager.getInstance().showRewardedAd(
                                                        context,
                                                        true,
                                                        {
                                                            rewardGranted = true
                                                            preferencesManager.setAppLocked(true)
                                                            isAppLocked = true
                                                            showSuccess("Screen lock enabled")
                                                        },
                                                        {
                                                            if (!rewardGranted) {
                                                            if (attempts >= maxAttempts) {
                                                                showSuccess("Ad is taking longer to load. Please try again in a moment.")
                                                            } else {
                                                                showSuccess("Ad not completed - Screen lock not enabled")
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            } else {
                                                // Ad is ready - show it immediately
                                                var rewardGranted = false
                                                AdManager.getInstance().showRewardedAd(
                                                    context,
                                                    true,
                                                    {
                                                        rewardGranted = true
                                                        preferencesManager.setAppLocked(true)
                                                        isAppLocked = true
                                                        showSuccess("Screen lock enabled")
                                                    },
                                                    {
                                                        if (!rewardGranted) {
                                                        showSuccess("Ad not completed - Screen lock not enabled")
                                                        }
                                                    }
                                                )
                                            }
                                            }
                                        } else if (!newValue && isAppLocked) {
                                            // Unlocking the app - no ad required
                                            preferencesManager.setAppLocked(false)
                                            isAppLocked = false
                                            showSuccess("Screen lock disabled")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Authentication Section
            SettingSection(
                title = "Authentication",
                icon = Icons.Default.Lock,
                description = "Choose between PIN (6 digits) or Password (8+ characters) for parent access. Change your PIN/Password anytime."
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Authentication Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (authMode == AuthMode.PIN) "PIN (6 digits)" else "Password (8+ chars)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                FilterChip(
                                    selected = authMode == AuthMode.PIN,
                                    onClick = {
                                        authMode = AuthMode.PIN
                                        preferencesManager.setAuthMode(AuthMode.PIN)
                                        showSuccess("Authentication mode changed to PIN")
                                    },
                                    label = { Text("PIN") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = authMode == AuthMode.PASSWORD,
                                    onClick = {
                                        authMode = AuthMode.PASSWORD
                                        preferencesManager.setAuthMode(AuthMode.PASSWORD)
                                        showSuccess("Authentication mode changed to Password")
                                    },
                                    label = { Text("Password") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (authMode == AuthMode.PIN) {
                                    showChangePinDialog = true
                                } else {
                                    showChangePasswordDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change ${if (authMode == AuthMode.PIN) "PIN" else "Password"}")
                        }
                    }
                }
            }
            
            // Security Questions Section
            SettingSection(
                title = "Security Questions",
                icon = Icons.Default.Info,
                description = "Set up 3 security questions to recover your PIN/Password if forgotten. All 3 answers must be correct to reset."
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Security Questions Status",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (preferencesManager.hasSecurityQuestionsSet()) 
                                        "✓ All 3 questions are set"
                                    else
                                        "⚠️ Not set up",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (preferencesManager.hasSecurityQuestionsSet())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToSecurityQuestionsSetup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (preferencesManager.hasSecurityQuestionsSet()) "Update Questions" else "Set Up Questions")
                        }
                    }
                }
            }
            
            // Rate App Section
            SettingSection(
                title = "Rate Our App",
                icon = Icons.Default.Star,
                description = "Enjoying KidsView? Please rate us on the Play Store to help other parents discover our app!"
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Help Us Improve",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Your feedback helps us make KidsView better. Rate us on the Play Store!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // In-App Review Button (uses Google's In-App Review API)
                        val reviewViewModel = hiltViewModel<ReviewTrackingViewModel>()
                        val activity = context as? android.app.Activity
                        Button(
                            onClick = {
                                // Manually trigger review request (bypasses criteria)
                                if (activity != null) {
                                    ReviewManager.requestReviewManually(activity, preferencesManager)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rate our app")
                        }
                        
                        // Fallback: Open Play Store directly if In-App Review doesn't work
                        OutlinedButton(
                            onClick = {
                                try {
                                    val packageName = "why.xee.kidsview"
                                    // Try to open Play Store app first
                                    val playStoreIntent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("market://details?id=$packageName")
                                    )
                                    try {
                                        context.startActivity(playStoreIntent)
                                    } catch (e: android.content.ActivityNotFoundException) {
                                        // If Play Store app is not available, open in browser
                                        val browserIntent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                                        )
                                        context.startActivity(browserIntent)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Play Store")
                        }
                    }
                }
            }
            
            // Contact Us Section
            SettingSection(
                title = "Contact Us",
                icon = Icons.Default.Email,
                description = "You can contact the developer to share the feedback to make the app more better."
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Get in Touch",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "You can contact the developer to share the feedback to make the app more better.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Button(
                            onClick = {
                                try {
                                    val email = "kidsview.app@gmail.com"
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                                        putExtra(Intent.EXTRA_SUBJECT, "KidsView App Feedback")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    showSuccess("Unable to open email client")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Contact Us")
                        }
                    }
                }
            }
            
            // Privacy Policy Section
            SettingSection(
                title = "Privacy Policy",
                icon = Icons.Default.Info,
                description = "View our privacy policy and learn how we protect your child's data. COPPA compliant and transparent."
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Privacy & Data Protection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "KidsView is fully COPPA-compliant. We collect minimal data and never share personal information. " +
                            "All ads are contextual and non-personalized. View the full privacy policy for details.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "This Privacy Policy applies to the KidsView mobile application and all its versions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                        Button(
                            onClick = onNavigateToPrivacyPolicy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Privacy Policy")
                        }
                    }
                }
            }
            } // Close Column
            
        // Success Message Overlay (fixed at top, outside scrollable area)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding() + 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Auto-dismiss success message after 3 seconds
            LaunchedEffect(successMessage) {
                if (successMessage != null) {
                    showSuccessMessage = true
                    kotlinx.coroutines.delay(3000) // Show for 3 seconds
                    showSuccessMessage = false
                    kotlinx.coroutines.delay(300) // Wait for animation to complete
                    successMessage = null
                }
            }
            
            AnimatedSuccessMessage(
                message = successMessage ?: "",
                visible = showSuccessMessage && successMessage != null,
                onDismiss = { 
                    showSuccessMessage = false
                    successMessage = null
                }
            )
        }
        
        } // Close main Box
        
        // Dialogs (outside Box, at Scaffold level)
        // Change PIN Dialog
        if (showChangePinDialog) {
            ChangePinDialog(
                onDismiss = { showChangePinDialog = false },
                onConfirm = { newPin: String ->
                    if (preferencesManager.setParentPin(newPin)) {
                        showChangePinDialog = false
                        showSuccess("PIN changed successfully")
                    }
                },
                preferencesManager = preferencesManager
            )
        }
        
        // Change Password Dialog
        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                onDismiss = { showChangePasswordDialog = false },
                onConfirm = { newPassword: String ->
                    if (preferencesManager.setParentPassword(newPassword)) {
                        showChangePasswordDialog = false
                        showSuccess("Password changed successfully")
                    }
                },
                preferencesManager = preferencesManager
            )
        }
        
        // Time Picker Dialog
        if (showTimePickerDialog) {
            TimeLimitPickerDialog(
                currentMinutes = timeLimitMinutes,
                onDismiss = { showTimePickerDialog = false },
                onConfirm = { hours: Int, mins: Int ->
                    val totalMinutes = (hours * 60 + mins).coerceIn(60, 120) // Cap at 120 minutes (2 hours max)
                    
                    // Get current state BEFORE any changes
                    val oldBase = preferencesManager.getBaseTimeLimitMinutes()
                    val currentApplied = preferencesManager.getAppliedEarnedTimeToday()
                    val walletTime = preferencesManager.getTotalEarnedTimeMinutes()
                    val oldEffective = preferencesManager.getTimeLimitMinutes()
                    
                    Log.d("TimeLimit", "Setting time limit:")
                    Log.d("TimeLimit", "  Old: Base=$oldBase, Applied=$currentApplied, Wallet=$walletTime, Effective=$oldEffective")
                    Log.d("TimeLimit", "  New: Base=$totalMinutes")
                    
                    // Check if user is INCREASING base time from current value
                    // Default is 60 minutes (free), any increase beyond current base requires wallet time
                    val DEFAULT_BASE_TIME = 60
                    
                    // Only check wallet if user is actually INCREASING the base time (not reducing)
                    if (totalMinutes > oldBase) {
                        // User is increasing base time - check if they have wallet time to cover the increase
                        val increaseAmount = totalMinutes - oldBase
                        
                        // If increasing beyond default (60), need wallet time for the full increase
                        // If oldBase was already above 60, need wallet time for the difference
                        val walletNeeded = if (oldBase >= DEFAULT_BASE_TIME) {
                            // Old base was already above default, need wallet for the increase
                            increaseAmount
                        } else {
                            // Old base was below default, only need wallet for amount above default
                            (totalMinutes - DEFAULT_BASE_TIME).coerceAtLeast(0)
                        }
                        
                        if (walletNeeded > 0 && walletTime < walletNeeded) {
                            showTimePickerDialog = false
                            showSuccess("Insufficient wallet time. Need $walletNeeded minutes to increase from ${oldBase}min to ${totalMinutes}min, have $walletTime minutes. Watch ads to earn time.")
                            return@TimeLimitPickerDialog
                        }
                        
                        // Valid increase - will deduct from wallet after ad (see ad callback below)
                    }
                    // If reducing or same, skip wallet check (reduction logic below handles returning time to wallet)
                    
                    // Calculate what the new effective time would be with new base and current applied
                    val newEffectiveWithCurrentApplied = totalMinutes + currentApplied
                    
                    // Calculate how much time needs to be returned to wallet
                    var timeToAddToWallet = 0
                    var newApplied = currentApplied
                    var wasAutoAdjusted = false
                    
                    // If base is being REDUCED, return only what was paid from wallet
                    if (totalMinutes < oldBase) {
                        val baseReduction = oldBase - totalMinutes
                        
                        // Only return to wallet what was actually paid from wallet (amount beyond default 60)
                        // If oldBase was 60 or less, nothing was paid, so return 0
                        // If oldBase > 60, return the amount that was paid (oldBase - 60), but capped at the reduction amount
                        val paidAmount = if (oldBase > DEFAULT_BASE_TIME) {
                            // User paid (oldBase - 60) to get to oldBase
                            // Return the minimum of what was paid and the reduction amount
                            (oldBase - DEFAULT_BASE_TIME).coerceAtMost(baseReduction)
                        } else {
                            // Old base was 60 or less, nothing was paid from wallet
                            0
                        }
                        
                        if (paidAmount > 0) {
                            timeToAddToWallet += paidAmount
                            Log.d("TimeLimit", "Base reduced: $oldBase -> $totalMinutes, returning $paidAmount min to wallet (was paid: ${oldBase - DEFAULT_BASE_TIME}, reduction: $baseReduction)")
                        } else {
                            Log.d("TimeLimit", "Base reduced: $oldBase -> $totalMinutes, nothing to return (old base was default or less)")
                        }
                        
                        // Also return any applied time to wallet
                        if (currentApplied > 0) {
                            timeToAddToWallet += currentApplied
                            newApplied = 0
                        }
                    }
                    // If new effective would exceed 180, automatically adjust by reducing applied time
                    else if (newEffectiveWithCurrentApplied > 180) {
                        // Calculate excess that needs to be returned to wallet
                        val excess = newEffectiveWithCurrentApplied - 180
                        // Return the excess from applied time to wallet
                        val appliedToReturn = excess.coerceAtMost(currentApplied)
                        if (appliedToReturn > 0) {
                            newApplied = currentApplied - appliedToReturn
                            timeToAddToWallet = appliedToReturn
                            wasAutoAdjusted = true
                            Log.d("TimeLimit", "Auto-adjusting: Total would be ${newEffectiveWithCurrentApplied}min, reducing applied by $appliedToReturn min to fit 180min limit")
                        }
                    }
                    // If base is same or increased, but effective time is being reduced
                    else if (newEffectiveWithCurrentApplied < oldEffective) {
                        // Calculate the reduction in effective time
                        val effectiveReduction = oldEffective - newEffectiveWithCurrentApplied
                        
                        // This reduction should come from applied time and go to wallet
                        val appliedToReturn = effectiveReduction.coerceAtMost(currentApplied)
                        if (appliedToReturn > 0) {
                            newApplied = currentApplied - appliedToReturn
                            timeToAddToWallet = appliedToReturn
                        }
                    }
                    
                    // Log the calculation
                    Log.d("TimeLimit", "Final calculation:")
                    Log.d("TimeLimit", "  New: Base=$totalMinutes, Applied=$newApplied, Effective=${totalMinutes + newApplied}")
                    Log.d("TimeLimit", "  Time to add to wallet: $timeToAddToWallet")
                    
                    // Show ad when timer is selected/set (with cooldown)
                    AdManager.getInstance().showInterstitialAd(context, true, false) {
                        // STEP 1: If increasing base time, deduct from wallet FIRST
                        if (totalMinutes > oldBase) {
                            // Calculate wallet needed for the increase
                            val walletNeeded = if (oldBase >= DEFAULT_BASE_TIME) {
                                // Old base was already above default, need wallet for the increase
                                totalMinutes - oldBase
                            } else {
                                // Old base was below default, only need wallet for amount above default
                                (totalMinutes - DEFAULT_BASE_TIME).coerceAtLeast(0)
                            }
                            
                            if (walletNeeded > 0) {
                                if (preferencesManager.consumeMinutesFromWallet(walletNeeded)) {
                                    totalEarnedTime = preferencesManager.getTotalEarnedTimeMinutes()
                                    Log.d("TimeLimit", "Deducted $walletNeeded minutes from wallet for base time increase ($oldBase -> $totalMinutes)")
                                } else {
                                    // This shouldn't happen as we checked above, but handle it
                                    showTimePickerDialog = false
                                    showSuccess("Failed to deduct wallet time. Please try again.")
                                    return@showInterstitialAd
                                }
                            }
                        }
                        
                        // STEP 2: Add to wallet (for reductions)
                        if (timeToAddToWallet > 0) {
                            val oldWallet = preferencesManager.getTotalEarnedTimeMinutes()
                            preferencesManager.addMinutesToWallet(timeToAddToWallet)
                            // Manually update state immediately
                            totalEarnedTime = totalEarnedTime + timeToAddToWallet
                            val newWallet = preferencesManager.getTotalEarnedTimeMinutes()
                            Log.d("TimeLimit", "Wallet update: Old=$oldWallet, Added=$timeToAddToWallet, New=$newWallet, State=$totalEarnedTime")
                        }
                        
                        // STEP 3: Update applied time (after wallet is updated)
                        if (newApplied != currentApplied) {
                            preferencesManager.setAppliedEarnedTime(newApplied)
                            Log.d("TimeLimit", "Applied time updated: $currentApplied -> $newApplied")
                        }
                        
                        // STEP 4: Set new base limit
                        timeLimitMinutes = totalMinutes
                        preferencesManager.setTimeLimitMinutes(totalMinutes)
                        
                        showTimePickerDialog = false
                        val hoursDisplay = totalMinutes / 60
                        val minsDisplay = totalMinutes % 60
                        val timeStr = if (hoursDisplay > 0) "${hoursDisplay}h ${minsDisplay}m" else "${minsDisplay}m"
                        val effectiveMinutes = totalMinutes + newApplied
                        val effectiveStr = if (effectiveMinutes / 60 > 0) "${effectiveMinutes / 60}h ${effectiveMinutes % 60}m" else "${effectiveMinutes % 60}m"
                        
                        val message = when {
                            // Auto-adjusted to fit 180-minute limit
                            wasAutoAdjusted -> {
                                "Time limit set to $timeStr (effective: $effectiveStr). Adjusted to fit 180min limit. $timeToAddToWallet minutes returned to wallet."
                            }
                            // Time returned to wallet (reduction)
                            timeToAddToWallet > 0 -> {
                                "Time limit set to $timeStr. $timeToAddToWallet minutes returned to wallet."
                            }
                            // Normal set
                            else -> {
                                "Time limit set to $timeStr"
                            }
                        }
                        showSuccess(message)
                    }
                },
                onResetTimeUsed = {
                    // Reset Daily Limit: Requires rewarded ad, deducts 60 min from earned time, restores to 180 min
                    val resetStartTime = System.currentTimeMillis()
                    Log.d("TimeReset", "═══════════════════════════════════════════════════════════")
                    Log.d("TimeReset", "Reset button clicked at ${resetStartTime}")
                        showTimePickerDialog = false
                    
                    // Check reviewer mode - if active, skip ad and reset immediately
                    if (ReviewerUnlockManager.isUnlocked()) {
                        Log.d("TimeReset", "Reviewer mode active - skipping ad, resetting immediately")
                        coroutineScope.launch(Dispatchers.IO) {
                            val resetOpStartTime = System.currentTimeMillis()
                            Log.d("TimeReset", "resetDailyLimit() called at ${resetOpStartTime} (reviewer mode)")
                            val result = preferencesManager.resetDailyLimit()
                            val resetOpDuration = System.currentTimeMillis() - resetOpStartTime
                            Log.d("TimeReset", "resetDailyLimit() completed in ${resetOpDuration}ms - Success: ${result.success}")
                            withContext(Dispatchers.Main) {
                                val uiUpdateStartTime = System.currentTimeMillis()
                                if (result.success) {
                                    timeLimitMinutes = preferencesManager.getBaseTimeLimitMinutes()
                                    showSuccess(result.message)
                                } else {
                                    showSuccess(result.message.ifEmpty { "Reset failed - insufficient earned time" })
                                }
                                val uiUpdateDuration = System.currentTimeMillis() - uiUpdateStartTime
                                val totalDuration = System.currentTimeMillis() - resetStartTime
                                Log.d("TimeReset", "UI update took ${uiUpdateDuration}ms")
                                Log.d("TimeReset", "Total reset flow took ${totalDuration}ms (reviewer mode - no ad)")
                                Log.d("TimeReset", "═══════════════════════════════════════════════════════════")
                            }
                        }
                        return@TimeLimitPickerDialog
                    }
                    
                    // Check if we have enough time (wallet + applied time combined)
                    val checkStartTime = System.currentTimeMillis()
                    val walletTime = preferencesManager.getTotalEarnedTimeMinutes()
                    val appliedTime = preferencesManager.getAppliedEarnedTimeToday()
                    val totalAvailable = walletTime + appliedTime
                    Log.d("TimeReset", "Time check took ${System.currentTimeMillis() - checkStartTime}ms - Wallet: $walletTime, Applied: $appliedTime, Total: $totalAvailable")
                    
                    if (totalAvailable < 60) {
                        showSuccess("Insufficient earned time. Need 60 minutes, have $totalAvailable minutes (Wallet: $walletTime, Applied: $appliedTime). Watch rewarded ads to earn time.")
                        return@TimeLimitPickerDialog
                    }
                    
                    // Show rewarded ad
                    if (!AdManager.getInstance().isRewardedAdReady()) {
                        // Ad not ready - try to load it and wait
                        if (!AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                            AdManager.getInstance().loadRewardedAd(context, true)
                        }
                        coroutineScope.launch {
                            var attempts = 0
                            val maxAttempts = 20
                            while (!AdManager.getInstance().isRewardedAdReady() && attempts < maxAttempts) {
                                delay(500)
                                attempts++
                            }
                            
                            var rewardGranted = false
                            AdManager.getInstance().showRewardedAd(
                                context,
                                true,
                                {
                                    rewardGranted = true
                                    // Run reset in background coroutine to avoid blocking UI
                                    Log.d("TimeReset", "Ad completed, starting reset operation...")
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val resetOpStartTime = System.currentTimeMillis()
                                        Log.d("TimeReset", "resetDailyLimit() called at ${resetOpStartTime}")
                                        val result = preferencesManager.resetDailyLimit()
                                        val resetOpDuration = System.currentTimeMillis() - resetOpStartTime
                                        Log.d("TimeReset", "resetDailyLimit() completed in ${resetOpDuration}ms - Success: ${result.success}")
                                        withContext(Dispatchers.Main) {
                                            val uiUpdateStartTime = System.currentTimeMillis()
                                            if (result.success) {
                                        timeLimitMinutes = preferencesManager.getBaseTimeLimitMinutes()
                                                showSuccess(result.message)
                                    } else {
                                                showSuccess(result.message.ifEmpty { "Reset failed - insufficient earned time" })
                                            }
                                            val uiUpdateDuration = System.currentTimeMillis() - uiUpdateStartTime
                                            val totalDuration = System.currentTimeMillis() - resetStartTime
                                            Log.d("TimeReset", "UI update took ${uiUpdateDuration}ms")
                                            Log.d("TimeReset", "Total reset flow took ${totalDuration}ms")
                                            Log.d("TimeReset", "═══════════════════════════════════════════════════════════")
                                        }
                                    }
                                },
                                {
                                    // Only show error if reward was not granted
                                    if (!rewardGranted) {
                                    if (attempts >= maxAttempts) {
                                        showSuccess("Ad is taking longer to load. Please try again.")
                                    } else {
                                        showSuccess("Ad not completed - Reset cancelled")
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        // Ad ready, show it
                        var rewardGranted = false
                        AdManager.getInstance().showRewardedAd(
                            context,
                            true,
                            {
                                rewardGranted = true
                                // Run reset in background coroutine to avoid blocking UI
                                Log.d("TimeReset", "Ad completed, starting reset operation (ad ready path)...")
                                coroutineScope.launch(Dispatchers.IO) {
                                    val resetOpStartTime = System.currentTimeMillis()
                                    Log.d("TimeReset", "resetDailyLimit() called at ${resetOpStartTime}")
                                    val result = preferencesManager.resetDailyLimit()
                                    val resetOpDuration = System.currentTimeMillis() - resetOpStartTime
                                    Log.d("TimeReset", "resetDailyLimit() completed in ${resetOpDuration}ms - Success: ${result.success}")
                                    AppLogger.d("ResetDailyLimit took ${resetOpDuration}ms")
                                    withContext(Dispatchers.Main) {
                                        val uiUpdateStartTime = System.currentTimeMillis()
                                        if (result.success) {
                                    timeLimitMinutes = preferencesManager.getBaseTimeLimitMinutes()
                                            showSuccess(result.message)
                                } else {
                                            showSuccess(result.message.ifEmpty { "Reset failed - insufficient earned time" })
                                        }
                                        val uiUpdateDuration = System.currentTimeMillis() - uiUpdateStartTime
                                        val totalDuration = System.currentTimeMillis() - resetStartTime
                                        Log.d("TimeReset", "UI update took ${uiUpdateDuration}ms")
                                        Log.d("TimeReset", "Total reset flow took ${totalDuration}ms")
                                        Log.d("TimeReset", "═══════════════════════════════════════════════════════════")
                                    }
                                }
                            },
                            {
                                // Only show error if reward was not granted
                                if (!rewardGranted) {
                                showSuccess("Ad not completed - Reset cancelled")
                                }
                            }
                        )
                    }
                }
            )
        }
        
        // Time Reset Information Dialog
        if (showTimeResetInfoDialog) {
            ElegantAlertDialog(
                onDismissRequest = { showTimeResetInfoDialog = false },
                title = {
                    Text(
                        "Time Reset & Wallet Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        """
                        ⏰ Daily Reset at Midnight (12:00am):
                        • App time resets to default 1 hour (60 minutes) at exactly 12:00am
                        • Wallet (earned time from ads) also resets at midnight
                        • All earned time entries are cleared at midnight
                        
                        ⏱️ Default Watch Time:
                        • Default daily watch time is 1 hour (60 minutes)
                        • Parents can increase this up to 3 hours (180 minutes) maximum
                        
                        💰 Earned Time Wallet:
                        • Watch rewarded ads to earn 15 minutes per ad
                        • Earned time is stored in your wallet
                        • Wallet resets at midnight (12:00am) daily
                        • Earned time must be manually applied to daily limit
                        • Quick select buttons are capped at 3 hours maximum, even if wallet has more time
                        
                        📊 Quick Select:
                        • Quick select buttons allow you to quickly set time limits
                        • Maximum quick select is 3 hours (180 minutes)
                        • This limit applies even if your wallet has more earned time available
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = { showTimeResetInfoDialog = false }) {
                        Text("Got it!")
                    }
                }
            )
        }
        
        // Wallet Time Conflict Error Dialog
        if (showWalletErrorDialog) {
            var isRewardedAdReady by remember { mutableStateOf(AdManager.getInstance().isRewardedAdReady()) }
            var adLoadingStartTime by remember { mutableStateOf(0L) }
            var loadingElapsedSeconds by remember { mutableStateOf(0) }
            
            // Poll ad readiness
            LaunchedEffect(Unit) {
                while (showWalletErrorDialog) {
                    delay(500)
                    isRewardedAdReady = AdManager.getInstance().isRewardedAdReady()
                    if (AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                        val elapsed = (System.currentTimeMillis() - adLoadingStartTime) / 1000
                        loadingElapsedSeconds = elapsed.toInt()
                    }
                }
            }
            
            // Load ad if not ready
            LaunchedEffect(showWalletErrorDialog) {
                if (showWalletErrorDialog && !isRewardedAdReady && !AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                    AdManager.getInstance().loadRewardedAd(context, true)
                    adLoadingStartTime = System.currentTimeMillis()
                }
            }
            
            val base = preferencesManager.getBaseTimeLimitMinutes()
            val appliedEarnedTime = preferencesManager.getTimeLimitMinutes() - base
            val selectedTotal = conflictingTimeHours * 60 + conflictingTimeMinutes
            val totalWithNewBase = selectedTotal + appliedEarnedTime
            val excessMinutes = totalWithNewBase - 180
            val totalEarnedTime = preferencesManager.getTotalEarnedTimeMinutes()
            
            ElegantAlertDialog(
                onDismissRequest = { showWalletErrorDialog = false },
                title = {
                    Text(
                        "Time Limit Conflict",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "The selected time limit conflicts with your applied earned time.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Selected base time: ${conflictingTimeHours}h ${conflictingTimeMinutes}m",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Applied earned time: ${appliedEarnedTime} minutes",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Total would be: ${selectedTotal + appliedEarnedTime} minutes (exceeds 180 min limit by $excessMinutes minutes)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Text(
                            "Watch an ad to add 15 minutes to your wallet, then you can adjust your time limit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            "Current wallet: $totalEarnedTime minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Check reviewer mode - if active, skip ad and grant reward immediately
                            if (ReviewerUnlockManager.isUnlocked()) {
                                preferencesManager.addEarnedTimeToWallet()
                                showWalletErrorDialog = false
                                showSuccess("30 minutes added to wallet. You can now set your time limit.")
                                showTimePickerDialog = true
                                return@Button
                            }
                            
                            // Show rewarded ad to add time to wallet
                            if (!AdManager.getInstance().isRewardedAdReady()) {
                                // Ad not ready - try to load it and wait
                                if (!AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                                    AdManager.getInstance().loadRewardedAd(context, true)
                                    adLoadingStartTime = System.currentTimeMillis()
                                }
                                coroutineScope.launch {
                                    var attempts = 0
                                    val maxAttempts = 20
                                    while (!AdManager.getInstance().isRewardedAdReady() && attempts < maxAttempts) {
                                        delay(500)
                                        attempts++
                                    }
                                    
                                    AdManager.getInstance().showRewardedAd(
                                        context,
                                        true,
                                        {
                                            preferencesManager.addEarnedTimeToWallet()
                                            showWalletErrorDialog = false
                                            showSuccess("15 minutes added to wallet. You can now set your time limit.")
                                            showTimePickerDialog = true
                                        },
                                        {
                                            if (attempts >= maxAttempts) {
                                                showSuccess("Ad is taking longer to load. Please try again.")
                                            } else {
                                                showSuccess("Ad not completed - Time not added to wallet")
                                            }
                                        }
                                    )
                                }
                            } else {
                                // Ad ready, show it
                                AdManager.getInstance().showRewardedAd(
                                    context,
                                    true,
                                    {
                                        preferencesManager.addEarnedTimeToWallet()
                                        showWalletErrorDialog = false
                                        showSuccess("15 minutes added to wallet. You can now set your time limit.")
                                        showTimePickerDialog = true
                                    },
                                    {
                                        showSuccess("Ad not completed - Time not added to wallet")
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isRewardedAdReady && AdManager.getInstance().isRewardedAdCurrentlyLoading()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading ad... (${loadingElapsedSeconds}s)")
                            } else {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Watch Ad to Add 15 Minutes")
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWalletErrorDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    } // Close Scaffold lambda
}

data class SecurityQuestionData(
    val question: String,
    val answer: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeLimitPickerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (hours: Int, minutes: Int) -> Unit,
    onResetTimeUsed: () -> Unit = {}
) {
    val currentHours = currentMinutes / 60
    val currentMins = currentMinutes % 60
    
    // Default to 1 hour (60 minutes) if current time is less than 1 hour or not in valid options
    val defaultHours = if (currentMinutes >= 60) currentHours else 1
    val defaultMins = if (currentMinutes >= 60) currentMins else 0
    
    var selectedHours by remember { mutableStateOf(defaultHours) }
    var selectedMinutes by remember { mutableStateOf(defaultMins) }
    
    // Max limit is now 2 hours (120 minutes) for effective time limit
    val maxLimit = 120
    
    ElegantAlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Set Daily Time Limit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description
                Text(
                    "Set base time limit (up to 3 hours). Watch ads to add bonus time!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                // Total time display (prominent)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Selected Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            String.format("%02d:%02d", selectedHours, selectedMinutes),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Quick Select Dropdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Quick Select",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    data class TimeOption(val hours: Int, val minutes: Int, val label: String)
                    val quickSelectOptions = listOf(
                        TimeOption(1, 0, "1 hour (default)"), // Default base time
                        TimeOption(1, 30, "1 hour 30 minutes"),
                        TimeOption(2, 0, "2 hours (maximum)") // Maximum effective time
                    )
                    
                    // Ensure selection defaults to 1 hour if current selection is not in options
                    LaunchedEffect(Unit) {
                        val isInOptions = quickSelectOptions.any { 
                            it.hours == selectedHours && it.minutes == selectedMinutes 
                        }
                        if (!isInOptions) {
                            selectedHours = 1
                            selectedMinutes = 0
                        }
                    }
                    
                    val currentSelectionText = quickSelectOptions.find { 
                        it.hours == selectedHours && it.minutes == selectedMinutes 
                    }?.label ?: "${selectedHours}h ${selectedMinutes}m"
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                            Text(currentSelectionText)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select time"
                            )
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickSelectOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                            onClick = {
                                        selectedHours = option.hours
                                        selectedMinutes = option.minutes
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Custom Minutes Input
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Custom Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    var showCustomInput by remember { mutableStateOf(false) }
                    var customMinutesText by remember { mutableStateOf("") }
                    
                    if (showCustomInput) {
                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = { newValue ->
                                // Only allow numeric input
                                if (newValue.all { it.isDigit() }) {
                                    customMinutesText = newValue
                                }
                            },
                            label = { Text("Enter minutes (60-120)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Row {
                                IconButton(
                                    onClick = { 
                                            val minutes = customMinutesText.toIntOrNull() ?: 0
                                            if (minutes in 60..120) {
                                                selectedHours = minutes / 60
                                                selectedMinutes = minutes % 60
                                                showCustomInput = false
                                                customMinutesText = ""
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Apply")
                                    }
                                IconButton(
                                    onClick = { 
                                            showCustomInput = false
                                            customMinutesText = ""
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                                    }
                                }
                            }
                        )
                    } else {
                        OutlinedButton(
                            onClick = { showCustomInput = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enter Custom Minutes")
                        }
                    }
                }
                
                // Reset Button (inside content)
                OutlinedButton(
                    onClick = {
                        onResetTimeUsed()
                        // Reset to current values
                        selectedHours = currentHours
                        selectedMinutes = currentMins
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "Reset Daily Limit",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Watch ad",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedHours, selectedMinutes) },
                enabled = run {
                    val total = selectedHours * 60 + selectedMinutes
                    total >= 1 && total <= 180 // Between 1 minute and 180 minutes (3 hours)
                },
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    "Set Time",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String? = null,
    onIconClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    
    // Auto-hide info after 3 seconds
    LaunchedEffect(showInfo) {
        if (showInfo) {
            delay(3000) // 3 seconds
            showInfo = false
        }
    }
    
    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .then(
                            if (onIconClick != null) {
                                Modifier.clickable(onClick = onIconClick)
                            } else {
                                Modifier
                            }
                        )
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (description != null) {
                    IconButton(
                        onClick = { showInfo = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            content()
        }
        
        // Info Bubble Overlay - appears as external bubble above the screen
        if (showInfo && description != null) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                    animationSpec = tween(200),
                    expandFrom = Alignment.Bottom
                ),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(
                    animationSpec = tween(200),
                    shrinkTowards = Alignment.Bottom
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp)
                    .widthIn(max = 280.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdReadinessIndicator(
    isReady: Boolean,
    loadingSeconds: Int?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    
    if (isReady) {
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blink_alpha"
        )
        
        Box(
            modifier = Modifier.size(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = size.minDimension / 2,
                    alpha = alpha,
                    style = Fill
                )
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.size(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFFF44336),
                        radius = size.minDimension / 2,
                        style = Fill
                    )
                }
            }
            
            if (loadingSeconds != null && loadingSeconds > 0) {
                Text(
                    text = "${loadingSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    ElegantAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change PIN") },
        text = {
            Column {
                Text("Enter a 6-digit PIN", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        pin = it.filter { char -> char.isDigit() }.take(6)
                        error = null
                    },
                    label = { Text("New PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { 
                        confirmPin = it.filter { char -> char.isDigit() }.take(6)
                        error = null
                    },
                    label = { Text("Confirm PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.length < 6 -> error = "PIN must be 6 digits"
                        pin != confirmPin -> error = "PINs do not match"
                        else -> onConfirm(pin)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val passwordRequirements = """
        Password Requirements:
        • Minimum 8 characters
        • At least one numeric digit (0-9)
        • At least one special character (@#$%&*!)
    """.trimIndent()
    
    ElegantAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = passwordRequirements,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        error = null
                    },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        error = null
                    },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        password != confirmPassword -> error = "Passwords do not match"
                        else -> {
                            val (isValid, validationError) = preferencesManager.validatePasswordRequirements(password)
                            if (isValid) {
                                if (preferencesManager.setParentPassword(password)) {
                                    onConfirm(password)
                                } else {
                                    error = "Failed to save password. Please try again."
                                }
                            } else {
                                error = validationError ?: "Invalid password"
                            }
                        }
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
