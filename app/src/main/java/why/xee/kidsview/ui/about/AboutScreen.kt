package why.xee.kidsview.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import why.xee.kidsview.ui.components.FeedbackDialog
import why.xee.kidsview.ui.components.UpdateButton
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    var showFeedbackDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon and Name
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "App Icon",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "KidsView",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "App Version: ${why.xee.kidsview.BuildConfig.VERSION_NAME} (Build ${why.xee.kidsview.BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Description
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About KidsView",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "KidsView is a safe and educational video app designed specifically for children. " +
                                "Parents can curate age-appropriate content, set time limits, and manage their child's viewing experience. " +
                                "Kids can enjoy watching approved videos in a secure, ad-free environment.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Features Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Features",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureItem(icon = Icons.Default.Lock, text = "Parental Controls & Time Limits")
                    FeatureItem(icon = Icons.Default.Search, text = "Safe Video Search & Browsing")
                    FeatureItem(icon = Icons.Default.Favorite, text = "Video Request System")
                    FeatureItem(icon = Icons.Default.Star, text = "Organized Categories")
                    FeatureItem(icon = Icons.Default.Settings, text = "Customizable Themes")
                    FeatureItem(icon = Icons.Default.CheckCircle, text = "COPPA Compliant & Safe")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Privacy Policy Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Privacy Policy Link
                    val context = LocalContext.current
                    Text(
                        text = "View Full Privacy Policy",
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
                            .padding(vertical = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    PrivacyPolicyContent()
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Feedback & Update Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Feedback & Updates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Feedback Button
                    Button(
                        onClick = { showFeedbackDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Feedback,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Feedback")
                    }
                    
                    // Update Button
                    val activity = LocalContext.current as? android.app.Activity
                    if (activity != null) {
                        UpdateButton(
                            activity = activity,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contact & Support
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Contact & Support",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "For questions, concerns, or support, please contact us through the app store listing or visit our website.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp
                    )
                }
            }
            
            if (showFeedbackDialog) {
                FeedbackDialog(
                    onDismiss = { showFeedbackDialog = false }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Copyright
            Text(
                text = "© 2026-2027 KidsView. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PrivacySection(
            title = "1. Information We Collect",
            content = "KidsView is designed with privacy in mind. We collect minimal information:\n\n" +
                    "• App Usage Data: We collect anonymous usage statistics to improve the app experience.\n" +
                    "• Device Information: Basic device information (device type, OS version) for compatibility.\n" +
                    "• Video Requests: When children request videos, only the video name is stored (not personal information).\n\n" +
                    "We do NOT collect:\n" +
                    "• Personal information from children (names, emails, phone numbers)\n" +
                    "• Location data\n" +
                    "• Contact information\n" +
                    "• Photos or files from the device"
        )
        
        PrivacySection(
            title = "2. How We Use Information",
            content = "The limited information we collect is used solely to:\n\n" +
                    "• Provide and improve app functionality\n" +
                    "• Display age-appropriate content\n" +
                    "• Ensure app security and prevent abuse\n" +
                    "• Comply with legal requirements\n\n" +
                    "We do NOT use information for:\n" +
                    "• Behavioral advertising targeting children\n" +
                    "• Selling data to third parties\n" +
                    "• Creating user profiles"
        )
        
        PrivacySection(
            title = "3. Third-Party Services",
            content = "KidsView uses the following third-party services:\n\n" +
                    "• Google AdMob: For displaying contextual ads in Parent Mode only. All ads are COPPA-compliant and non-personalized.\n" +
                    "• YouTube Data API: For searching and displaying video content. We do not collect or store YouTube account information.\n" +
                    "• Firebase: For storing video requests and categories. Data is encrypted in transit.\n\n" +
                    "All third-party services comply with COPPA (Children's Online Privacy Protection Act) requirements."
        )
        
        PrivacySection(
            title = "4. Parental Controls",
            content = "KidsView includes comprehensive parental controls:\n\n" +
                    "• Parent Mode: Protected by PIN or password, only accessible to parents.\n" +
                    "• Time Limits: Parents can set daily viewing time limits.\n" +
                    "• Content Curation: Parents approve all videos before children can watch them.\n" +
                    "• App Lock: Parents can lock the app to prevent unauthorized access.\n\n" +
                    "Children cannot access Parent Mode or change settings without parental authentication."
        )
        
        PrivacySection(
            title = "5. Data Security",
            content = "We take data security seriously:\n\n" +
                    "• All data transmission is encrypted using HTTPS.\n" +
                    "• Local app data is stored securely on the device.\n" +
                    "• We do not store sensitive personal information.\n" +
                    "• Regular security updates and patches are applied."
        )
        
        PrivacySection(
            title = "6. Children's Privacy (COPPA Compliance)",
            content = "KidsView is fully COPPA-compliant:\n\n" +
                    "• We do not knowingly collect personal information from children under 13.\n" +
                    "• All ads shown are contextual and non-personalized.\n" +
                    "• No behavioral tracking or profiling of children.\n" +
                    "• Parental consent is required for any data collection.\n" +
                    "• Parents can review and delete their child's data at any time."
        )
        
        PrivacySection(
            title = "7. Data Retention & Deletion",
            content = "• Video requests and app preferences are stored locally on your device.\n" +
                    "• You can delete all app data by uninstalling the app.\n" +
                    "• To request data deletion, contact us at kidsview.app@gmail.com or through the app store.\n" +
                    "• We retain minimal anonymous usage data for app improvement purposes only."
        )
        
        PrivacySection(
            title = "8. Changes to Privacy Policy",
            content = "We may update this Privacy Policy from time to time. " +
                    "Any changes will be posted in this section and within the app. " +
                    "Continued use of the app after changes constitutes acceptance of the updated policy."
        )
        
        PrivacySection(
            title = "9. Your Rights",
            content = "You have the right to:\n\n" +
                    "• Access your data\n" +
                    "• Request data deletion\n" +
                    "• Opt-out of data collection (note: some features may not work)\n" +
                    "• Contact us with privacy concerns\n\n" +
                    "To exercise these rights, contact us at kidsview.app@gmail.com or through the app store listing."
        )
        
        PrivacySection(
            title = "10. Contact Us",
            content = "If you have questions about this Privacy Policy or our data practices, please contact us through:\n\n" +
                    "• Email: kidsview.app@gmail.com\n" +
                    "• The app store listing\n\n" +
                    "We are committed to protecting children's privacy and will respond to all inquiries promptly."
        )
        
        Text(
            text = "Last Updated: January 2026\nApp Version: ${why.xee.kidsview.BuildConfig.VERSION_NAME} (Build ${why.xee.kidsview.BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PrivacySection(title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

