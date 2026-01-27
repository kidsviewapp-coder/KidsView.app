import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    // Add the Crashlytics Gradle plugin
    id("com.google.firebase.crashlytics")
}

// Load local.properties file
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

// Get YouTube API keys from local.properties
// Supports multiple keys separated by commas for rotation
// Example: youtube.api.keys=KEY1,KEY2,KEY3
// If using single key, also supports: youtube.api.key=KEY1
val youtubeApiKeysString: String = localProperties.getProperty("youtube.api.keys") 
    ?: localProperties.getProperty("youtube.api.key") 
    ?: ""
val youtubeApiKeys: List<String> = youtubeApiKeysString
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }
val youtubeApiKey: String = youtubeApiKeys.firstOrNull() ?: "" // Fallback for single key

android {
    namespace = "why.xee.kidsview"
    compileSdk = 36

    signingConfigs {
        // Debug signing config (uses default debug keystore)
        getByName("debug") {
            // Uses default debug keystore automatically
        }
        
        // Release signing config for production and alpha testing
        // IMPORTANT: Use the SAME keystore for both alpha testing and production
        // Google Play requires consistent signing key for all app updates
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = Properties()
            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(keystorePropertiesFile.inputStream())
            }
            
            // Get keystore path (relative to project root or absolute path)
            val keystorePath = keystoreProperties.getProperty("storeFile") ?: ""
            if (keystorePath.isNotBlank()) {
                storeFile = file(keystorePath)
                storePassword = keystoreProperties.getProperty("storePassword") ?: ""
                keyAlias = keystoreProperties.getProperty("keyAlias") ?: ""
                keyPassword = keystoreProperties.getProperty("keyPassword") ?: ""
            } else {
                // Fallback to debug keystore if release keystore not configured
                // This allows building for alpha testing without keystore setup
                // WARNING: For production, you MUST set up a release keystore
                storeFile = signingConfigs.getByName("debug").storeFile
                storePassword = signingConfigs.getByName("debug").storePassword
                keyAlias = signingConfigs.getByName("debug").keyAlias
                keyPassword = signingConfigs.getByName("debug").keyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "why.xee.kidsview"
        minSdk = 24
        targetSdk = 36
        versionCode = 10002
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // YouTube API keys from local.properties (secure - not committed to version control)
        // For multiple keys (recommended): youtube.api.keys=KEY1,KEY2,KEY3
        // For single key: youtube.api.key=YOUR_API_KEY_HERE
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"") // Fallback single key
        buildConfigField("String", "YOUTUBE_API_KEYS", "\"$youtubeApiKeysString\"") // All keys for rotation
        
        // AdMob App ID for manifest placeholder
        // For debug: uses test ID, for release: uses production ID from local.properties
        val defaultAdmobAppId = localProperties.getProperty("admob.app.id")
            ?: "ca-app-pub-3940256099942544~3347511713" // Test ID fallback for debug
        manifestPlaceholders["ADMOB_APP_ID"] = defaultAdmobAppId
    }

    buildTypes {
        debug {
            isDebuggable = true
            
            // DEBUG BUILD: Use Google's official test ad IDs
            // Test ads will show "Test Ad" labels
            // Fake rewards granted after test ad completion
            val testAppId = "ca-app-pub-3940256099942544~3347511713"
            val testBannerId = "ca-app-pub-3940256099942544/6300978111"
            val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
            val testRewardedId = "ca-app-pub-3940256099942544/5224354917"
            
            manifestPlaceholders["ADMOB_APP_ID"] = testAppId
            
            buildConfigField("String", "ADMOB_APP_ID", "\"$testAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testInterstitialId\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$testRewardedId\"")
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("boolean", "USE_TEST_IDS", "true")
            buildConfigField("boolean", "IS_PRODUCTION", "false")
        }
        
        create("alpha") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // ALPHA BUILD: No ads, fake rewards immediately
            // Perfect for Play Store alpha testing
            // AdMob SDK will NOT be initialized (checked in AdManager.init())
            // Use test App ID in manifest to prevent crashes (even though SDK won't initialize)
            val testAppId = "ca-app-pub-3940256099942544~3347511713"
            manifestPlaceholders["ADMOB_APP_ID"] = testAppId
            
            // BuildConfig: Empty IDs (ads disabled, SDK won't initialize)
            buildConfigField("String", "ADMOB_APP_ID", "\"\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"\"")
            buildConfigField("boolean", "ADS_ENABLED", "false")
            buildConfigField("boolean", "USE_TEST_IDS", "false")
            buildConfigField("boolean", "IS_PRODUCTION", "false")
            
            signingConfig = signingConfigs.getByName("release")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // RELEASE BUILD: Production IDs only (from local.properties)
            // Real ads and real rewards
            // NO test IDs allowed - Play Store compliant
            val releaseAdmobAppId = localProperties.getProperty("admob.app.id") ?: ""
            val releaseAdmobBannerId = localProperties.getProperty("admob.banner.id") ?: ""
            val releaseAdmobInterstitialId = localProperties.getProperty("admob.interstitial.id") ?: ""
            val releaseAdmobRewardedId = localProperties.getProperty("admob.rewarded.id") ?: ""
            
            // Validate production IDs are present
            if (releaseAdmobAppId.isBlank() || releaseAdmobBannerId.isBlank() || 
                releaseAdmobInterstitialId.isBlank() || releaseAdmobRewardedId.isBlank()) {
                throw GradleException(
                    "ERROR: Production AdMob IDs are required for release builds!\n" +
                    "Add the following to local.properties:\n" +
                    "admob.app.id=YOUR_APP_ID\n" +
                    "admob.banner.id=YOUR_BANNER_ID\n" +
                    "admob.interstitial.id=YOUR_INTERSTITIAL_ID\n" +
                    "admob.rewarded.id=YOUR_REWARDED_ID\n\n" +
                    "Get your AdMob IDs from: https://admob.google.com/"
                )
            }
            
            manifestPlaceholders["ADMOB_APP_ID"] = releaseAdmobAppId
            
            buildConfigField("String", "ADMOB_APP_ID", "\"$releaseAdmobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$releaseAdmobBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$releaseAdmobInterstitialId\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$releaseAdmobRewardedId\"")
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("boolean", "USE_TEST_IDS", "false")
            buildConfigField("boolean", "IS_PRODUCTION", "true")
            
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    // Enable native debug symbol generation for crash reporting
    // With AGP 8.x, symbols are automatically embedded in the AAB
    // If a separate ZIP is needed, it will be in: app/build/outputs/native-debug-symbols/release/
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
    
    // Explicitly enable native debug symbol generation for all build types
    defaultConfig {
        ndk {
            debugSymbolLevel = "FULL"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// Configure kapt
// Note: The warning "The following options were not recognized by any processor" is harmless.
// Hilt automatically passes these options (dagger.fastInit, etc.), but some processors don't recognize them.
// This doesn't affect functionality and can be safely ignored.
kapt {
    correctErrorTypes = true
    useBuildCache = true
    mapDiagnosticLocations = true
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Material Icons Extended (for Visibility, VisibilityOff, etc.)
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose) // Required for androidx.hilt.lifecycle.viewmodel.compose
    kapt(libs.hilt.compiler)
    
    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Image Loading
    implementation(libs.coil.compose)
    implementation("io.coil-kt:coil-gif:2.6.0")
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    
    // YouTube Player
    implementation(libs.youtube.player)
    
    // AdMob
    implementation(libs.admob)
    
    // Unity Ads Mediation Adapter (for AdMob mediation)
    // Unity Ads will be served through AdMob mediation in Parent Mode only
    // Version 4.16.0.1+ required for full bidding support with all ad formats
    implementation("com.google.ads.mediation:unity:4.16.0.1")
    
    // WebView (required for AdMob ads - helps with JavascriptEngine errors)
    implementation("androidx.webkit:webkit:1.11.0")
    
    // Play In-App Update
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    
    // Play In-App Review
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")
    
    // Lottie
    implementation(libs.lottie.compose)
    
    // Firebase - Using BoM for version management
    // Import the BoM for the Firebase platform
    // Using 33.7.0 (stable version) - if 34.6.0 fails, try this
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    
    // Add the dependencies for Firebase libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    // Using -ktx versions for Kotlin coroutines support
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx") // Required for Crashlytics breadcrumb logs
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.firebase:firebase-auth-ktx") // Required for Firestore security rules
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}