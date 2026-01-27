package why.xee.kidsview.ui.privacy

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import why.xee.kidsview.BuildConfig

/**
 * Privacy Policy Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Last Updated: January 1, 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "App Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Privacy Policy Link
            val context = LocalContext.current
            Text(
                text = "View Full Privacy Policy Online",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/kidsview-privacy-policy/home"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PrivacySection(
                title = "1. App Structure and Modes",
                content = """
                    KidsView operates in two distinct modes:
                    
                    Kid Mode:
                    • KidsView does not display its own ads (YouTube may display ads within the embedded player according to YouTube's policies, which are outside KidsView's control)
                    • Only individual YouTube videos shown (playlists never displayed)
                    • KidsView uses the officially supported MINIMAL player style provided by the YouTube Player API
                    • Videos auto-close when finished, which limits interaction with suggested videos
                    • No intentional external links or redirects
                    • Minimal data collection (video names only, not personal information)
                    
                    Parent Mode:
                    • Contextual, non-personalized ads may be displayed
                    • Full parental controls and content management
                    • Protected by PIN or password authentication
                    • Normal player with controls (stays open after video ends)
                """.trimIndent()
            )
            
            PrivacySection(
                title = "2. Information We Collect",
                content = """
                    Kid Mode:
                    • Video names when children request videos (not personally identifiable)
                    • Time usage data (stored locally only)
                    • Device information for compatibility
                    
                    Parent Mode:
                    • Authentication data (PIN/password, stored locally and encrypted)
                    • Security questions and answers (3 questions, stored locally and encrypted, used for PIN/password recovery)
                    • App preferences (stored locally)
                    • Video management data (stored in cloud - Firebase Firestore)
                    • Bonus time data (timestamp and minutes from rewarded ads, stored locally only, expires after 8 hours)
                    • **Per-User Data Isolation:** Each user has their own private video and category collections. Complete privacy and data isolation between users.
                    
                    We do NOT collect:
                    • Personal information from children (names, emails, phone numbers)
                    • Location data
                    • Photos, files, or contact information
                    • YouTube account information
                """.trimIndent()
            )
            
            PrivacySection(
                title = "3. YouTube Integration",
                content = """
                    KidsView uses YouTube Player API to embed videos:
                    
                    • Only individual videos are shown - playlists are never displayed
                    • All searches use type=video only (no playlists)
                    • Multiple layers of filtering ensure no playlists appear
                    
                    Kid Mode Player Safety:
                    • KidsView uses the officially supported MINIMAL player style provided by the YouTube Player API
                    • KidsView does not modify or obscure YouTube-provided UI elements beyond options officially supported by the API
                    • Auto-closes when video finishes
                    • The player automatically closes when playback ends, which limits interaction with suggested videos
                    • Playlists are never loaded or displayed. The player automatically closes at video completion
                    • Complies with Google Play Families Policy
                    
                    Parent Mode:
                    • Normal player with controls
                    • Player stays open after video ends
                    • May redirect to YouTube app in error scenarios
                    
                    YouTube services are subject to YouTube's Terms of Service and Privacy Policy.
                """.trimIndent()
            )
            
            PrivacySection(
                title = "4. Advertising (Parent Mode Only)",
                content = """
                    • KidsView ads are displayed ONLY in Parent Mode
                    • KidsView does not display its own ads in Kid Mode. YouTube may display ads within the embedded player according to YouTube's policies, which are outside KidsView's control
                    • All ads are contextual and non-personalized
                    • Ads comply with Google AdMob Families Policy
                    • The app may request AD_ID permission when required by Google AdMob for ad delivery (including non-personalized ads)
                    • We do not use Advertising ID for tracking or profiling users
                    
                    Rewarded Advertisements (Parent Mode Only):
                    • Parents may optionally watch rewarded advertisements in Parent Mode to earn additional Kids Mode watch time
                    • Rewarded ads grant non-monetary in-app rewards (additional Kids Mode watch time: +30 minutes per completed ad)
                    • Earned watch time is stored in a wallet and does not automatically apply - parents must manually add it to the daily limit
                    • Children are NEVER required to view advertisements and ads are NOT shown during child usage (Kids Mode)
                    • Rewarded ads are completely optional and only available in Parent Mode
                    • All rewarded ads comply with Google Play Kids & Families and Rewarded Ads policies
                """.trimIndent()
            )
            
            PrivacySection(
                title = "5. Third-Party Services",
                content = """
                    KidsView uses:
                    
                    • Google AdMob: Contextual, non-personalized ads in Parent Mode only
                    • YouTube Data API & Player API: For video search and playback
                    • Firebase (Firestore, Crashlytics): For data storage and crash reporting
                    
                    All third-party services comply with COPPA requirements.
                    Third-party data collection is governed by their respective privacy policies.
                """.trimIndent()
            )
            
            PrivacySection(
                title = "6. Children's Privacy (COPPA Compliance)",
                content = """
                    KidsView is fully COPPA-compliant:
                    
                    • We do not knowingly collect personal information from children under 13
                    • No ads displayed by KidsView in Kid Mode (YouTube may display ads within the embedded player according to YouTube's policies)
                    • No playlists - only individual videos shown
                    • KidsView uses the officially supported MINIMAL player style. Videos auto-close when finished, which limits access to suggested videos
                    • No intentional external links or redirects
                    • No behavioral tracking or profiling of children
                    • Parents must approve all videos before children can watch them
                    • Parents can review and delete all data at any time
                """.trimIndent()
            )
            
            PrivacySection(
                title = "7. Data Storage and Security",
                content = """
                    Local Storage (Device):
                    • Authentication data (encrypted)
                    • App preferences and settings
                    • Time usage tracking
                    
                    Cloud Storage (Firebase Firestore):
                    • Video requests (video names only)
                    • Parent-selected videos (stored in per-user collections: users/{userId}/videos)
                    • Video categories (stored in per-user collections: users/{userId}/categories)
                    • **Per-User Isolation:** Each user can only access their own data. Complete privacy between users.
                    • All data transmission encrypted using HTTPS
                    
                    Crash Reporting:
                    • Firebase Crashlytics collects technical data for app improvement
                    • Includes device info, app version, error logs
                    • Does not include personal information
                """.trimIndent()
            )
            
            PrivacySection(
                title = "8. Your Rights",
                content = """
                    As a parent, you have the right to:
                    
                    • Access all data (video requests, approved videos, settings)
                    • Delete individual video requests or approved videos
                    • Request deletion of all cloud data (directly from app settings or via email)
                    • Control time limits and app settings
                    • Approve or reject video requests
                    
                    To exercise these rights, use the app's built-in features or contact us at: kidsview.app@gmail.com
                """.trimIndent()
            )
            
            PrivacySection(
                title = "9. Data Retention and Deletion",
                content = """
                    • Local data: Retained until app is uninstalled or data is cleared
                    • Cloud data: Retained until deleted by parents through the app
                    • Crash reports: Retained by Firebase (typically 90 days)
                    • To request deletion of all data: Use the data deletion option in Parent Mode settings (if available) or email kidsview.app@gmail.com
                    • We will delete all associated data within 30 days
                """.trimIndent()
            )
            
            PrivacySection(
                title = "10. Contact Us",
                content = """
                    If you have questions about this Privacy Policy or our data practices:
                    
                    • Email: kidsview.app@gmail.com
                    • Response Time: We aim to respond within 30 days
                    
                    We are committed to protecting children's privacy and will address all concerns promptly.
                """.trimIndent()
            )
            
            PrivacySection(
                title = "11. Version History and Updates",
                content = """
                    This Privacy Policy is versioned to track changes. All previous policy text is preserved.
                    
                    Version 1.0.2 (Build 10002) - January 2026:
                    • Enhanced watch-time system with Firebase cloud sync
                    • Base daily watch-time: 1 hour (60 minutes) default, up to 2 hours (120 minutes) maximum
                    • Wallet system: Earn 15 minutes per rewarded ad (reduced from 30 minutes)
                    • Wallet can accumulate up to 3 hours (180 minutes) independently
                    • Manual wallet application to increase daily watch-time
                    • Automatic midnight reset of watch-time, wallet, and applied time
                    • Reset after usage: Watch 3 consecutive ads to reset when reaching 2 hours
                    • Quick time selection dropdown menu
                    • Custom minutes input for precise time settings
                    • Real-time wallet and used time display
                    • Automatic time adjustment when exceeding limits
                    • Fixed wallet time updates when reducing time limit
                    • Fixed time limit validation
                    • Updated UI to reflect 15 minutes per ad (was showing 30 minutes)
                    • Fixed insufficient wallet error when reducing time limits
                    
                    Version 1.0.1 (Build 10001) - January 2026:
                    • Google Play Reviewer Unlock System with session-only bypass
                    • Reviewer passkey/password: 791989 (works in both PIN and password modes)
                    • Immediate setup skip when reviewer code is entered
                    • All parent-restricted features automatically unlocked for reviewers
                    
                    Version 1.0.0 (Build 10000) - January 1, 2026:
                    • Smart time limit conflict detection with one-tap ad watching to add more time
                    • Enhanced visual feedback for video deletion requests (greyed out videos with pending indicators)
                    • Precise midnight reset system for app time and wallet (12:00am exact reset)
                    • Improved time management with default 1-hour watch time and information dialogs
                    • Better error handling and user feedback for time limit conflicts
                    • Calendar-based midnight detection for accurate daily resets
                    • Enhanced rewarded ad integration in error dialogs
                    • Integrated Google Play In-App Review API for seamless rating experience
                    • Enhanced video request workflow - automatically opens search when requests are approved
                    • Improved Kids Mode player - controls hide while playing for distraction-free viewing
                    • Fixed YouTube native seekbar appearing during playback (changed to MINIMAL player style)
                    • Fixed play/pause button state synchronization after navigating between videos
                    • Fixed dialog sizing and menu layout issues
                    • Fixed favorites feature - favoriting no longer automatically adds videos to Kids Mode
                    • Improved Time Limit picker layout with side-by-side hours and minutes
                    • Enhanced app update management with download progress tracking
                    • Simplified rating option in Parent Settings with In-App Review integration
                    • Enhanced app stability and performance improvements
                    • Improved error handling and user feedback
                    • Better memory management and code optimizations
                    • Star rating feedback system with automatic Play Store navigation
                    • Automatic update installation when downloaded (no manual restart needed)
                    • Background update checks on app start and resume
                    • Enhanced video player controls - next/previous buttons appear on screen tap in kids mode
                    • Numeric keyboard for PIN entry (faster and more accurate)
                    • Interstitial ads when saving videos from parent mode or favorites
                    • Fixed timer reset bug preventing incorrect time resets in kids mode
                    • Fixed parent mode navigation to go directly to Parent Search
                    • Fixed menu positioning on right side in kids mode
                    • Improved video error handling with consistent behavior across all devices
                    • Automatic deletion of incompatible videos from user's collection
                    
                    Version 2.1.1 (Build 2101) - December 2025:
                    • Fixed dialog sizing issues - PIN entry and setup dialogs now properly fit content
                    • Removed dark overlay from PIN dialog for better video visibility
                    • Fixed menu layout issues on search page
                    • Fixed favorites feature - favoriting no longer automatically adds videos to Kids Mode
                    • Simplified rating option in Parent Settings
                    
                    Version 2.1.0 (Build 2100) - December 2025:
                    • Enhanced child-safe YouTube player experience in Kids Mode
                    • Implemented MINIMAL player style with limited controls for child safety
                    • Auto-close player when video ends to prevent browsing suggested videos
                    • Custom child-friendly controls overlay with large, colorful buttons
                    • Sequential video playback - auto-plays next approved video seamlessly
                    • Enhanced time limit enforcement - player locks when daily limit is reached
                    • Fixed play/pause button functionality after video navigation
                    • Enhanced ad system with strict Alpha/Production mode separation
                    • Improved rewarded ads implementation with explicit "Watch Ad" button labeling
                    • Fixed interstitial and banner ad display functionality
                    • Enhanced ad placement verification and testing support
                    • Improved ad SDK initialization and management
                    • Better separation between test and production ad configurations
                    • Enhanced privacy disclosures for ad functionality
                    • Improved ad cooldown and frequency management
                    
                    Version 2.0.0 (Build 2000) - December 2025:
                    • Added rewarded advertisements feature in Parent Mode only
                    • Implemented earned time wallet system for additional Kids Mode watch time
                    • Parents can optionally watch rewarded ads to earn +30 minutes per ad to their earned time wallet
                    • Earned time must be manually applied by parents - it does not auto-apply
                    • Added "Reset Daily Limit" feature requiring rewarded ad completion and deducting 60 minutes from earned time
                    • Daily watch time cap set to 180 minutes (3 hours) - never exceeded
                    • Default daily watch time set to 60 minutes, resets automatically each day
                    • Enhanced time limit enforcement: player pauses and locks when time reaches zero in Kids Mode
                    • All rewarded ads are optional, parent-only, and never shown to children
                    • Updated privacy disclosures to reflect rewarded ad functionality
                    
                    Previous versions:
                    • Version 1.0.0: Initial release with basic time limits and parental controls
                """.trimIndent()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Rate Our App Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                onClick = {
                    try {
                        val packageName = "why.xee.kidsview"
                        // Try to open Play Store app first
                        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                        try {
                            context.startActivity(playStoreIntent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            // If Play Store app is not available, open in browser
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                            context.startActivity(browserIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Rate Our App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            PrivacySection(
                title = "12. Permissions",
                content = """
                    Required Permissions:
                    • INTERNET: For API calls, ad loading, video playback, cloud data sync
                    • ACCESS_NETWORK_STATE: For checking internet connectivity
                    • AD_ID (Android 13+): For ad delivery by Google AdMob, including non-personalized ads
                    
                    We do NOT request:
                    • Location, Camera, Microphone, Contacts, Storage, Phone/SMS, Calendar, or Biometric authentication
                """.trimIndent()
            )
            
            PrivacySection(
                title = "13. International Data Transfers",
                content = """
                    Data collected by KidsView may be processed and stored in:
                    • United States (Google Firebase servers)
                    • Other countries where Google operates data centers
                    
                    Data transfers are governed by Google's data processing agreements and applicable data protection laws.
                """.trimIndent()
            )
            
            PrivacySection(
                title = "14. Changes to Privacy Policy",
                content = """
                    We may update this Privacy Policy from time to time.
                    Significant changes will be announced in the app.
                    Updated policy will be posted with a new "Last Updated" date.
                    Continued use of the app after changes constitutes acceptance.
                """.trimIndent()
            )
            
            PrivacySection(
                title = "15. Additional Information",
                content = """
                    App Information:
                    • App Name: KidsView
                    • Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})
                    • Platform: Android
                    • Minimum Android Version: Android 7.0 (API 24)
                    • Target Android Version: Android 14 (API 36)
                    
                    Developer Information:
                    • Developer: KidsView Development Team
                    • Contact: kidsview.app@gmail.com
                """.trimIndent()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "By using KidsView, you acknowledge that:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            Text(
                text = "• You have read and understood this Privacy Policy\n• You consent to the data practices described herein\n• You are a parent or legal guardian (for Parent Mode access)\n• You understand that Kid Mode is designed for children and is COPPA-compliant\n• You understand that YouTube services are subject to YouTube's own policies\n• You understand that ads in Parent Mode are provided by Google AdMob",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Effective Date: January 1, 2026\nApp Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}

