# KidsView - Kids Video App with Parental Controls

A full-featured Android application designed for kids to watch approved cartoon videos with comprehensive parental controls. Built with modern Android development practices using Kotlin, Jetpack Compose, MVVM architecture, and integrated with YouTube Data API v3, Firebase Firestore, and Google AdMob.

**📋 Version History**: See [CHANGELOG.md](CHANGELOG.md) for complete version history and changes.

## Features

- 🎬 **Home Screen** with category slider and popular videos
- 🔍 **Search** functionality to find kids' content
- 📺 **Category Browsing** with predefined playlists (Cartoons, Anime, Nursery Rhymes, etc.)
- ▶️ **Video Player** with YouTube Player API integration
- ❤️ **Favorites** system with automatic Favorites category - organizes existing saved videos into a dedicated Favorites category. In Kids Mode, favoriting prevents deletion requests. In Parent Mode, it helps organize favorite videos.
- ⚙️ **Settings** screen with dark mode toggle
- 📱 **AdMob Integration** (Banner, Interstitial, and Rewarded ads)
- 🎨 **Kid-Friendly UI** with dark theme and bright accents
- ✨ **Smooth Animations** and modern UX

## Technical Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: Jetpack Compose
- **Navigation**: Navigation Component
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Database**: Room
- **Video Player**: YouTube Player API for Android
- **Ads**: Google AdMob

## Setup Instructions

### 1. Get YouTube API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable **YouTube Data API v3**
4. Create credentials (API Key)
5. Copy your API key

### 2. Add YouTube API Key (Secure Method)

**IMPORTANT**: For security, API keys are stored in `local.properties` which is excluded from version control.

1. Open the `local.properties` file in the project root
2. Add your YouTube API key:

```properties
youtube.api.key=YOUR_ACTUAL_API_KEY_HERE
```

**Security Note**: The `local.properties` file is already in `.gitignore` and will not be committed to version control. Never commit API keys to a repository.

### 3. Get AdMob Account & Ad IDs

1. Go to [Google AdMob](https://admob.google.com/)
2. Create an account and app
3. Get your **App ID** and **Ad Unit IDs** for:
   - Banner Ad
   - Interstitial Ad
   - Rewarded Ad (optional)

### 4. Update AdMob IDs

#### In `app/src/main/AndroidManifest.xml`:

Replace the test App ID with your actual AdMob App ID:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
```

#### In `app/src/main/java/com/example/kidflix/utils/Constants.kt`:

Replace the test Ad IDs with your actual Ad Unit IDs:

```kotlin
const val ADMOB_APP_ID = "ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"
const val ADMOB_BANNER_AD_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
const val ADMOB_INTERSTITIAL_AD_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
const val ADMOB_REWARDED_AD_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
```

### 5. Update Category Playlist IDs

In `app/src/main/java/com/example/kidflix/utils/Constants.kt`, replace the example playlist IDs with actual YouTube playlist IDs:

```kotlin
const val PLAYLIST_CARTOONS = "YOUR_PLAYLIST_ID_HERE"
const val PLAYLIST_KIDS_SHOWS = "YOUR_PLAYLIST_ID_HERE"
// ... etc
```

To find playlist IDs:
1. Go to a YouTube playlist
2. The playlist ID is in the URL: `youtube.com/playlist?list=PLAYLIST_ID_HERE`

### 6. Build and Run

1. Open the project in Android Studio
2. Sync Gradle files
3. Build the project
4. Run on an emulator or physical device

## Project Structure

```
app/
├── data/
│   ├── api/              # YouTube API service
│   ├── model/            # Data models
│   ├── repository/       # Repository layer
│   └── local/            # Room database
├── di/                   # Dependency injection modules
├── navigation/           # Navigation setup
├── ui/
│   ├── home/            # Home screen
│   ├── search/          # Search screen
│   ├── category/        # Category screen
│   ├── player/          # Video player screen
│   ├── favorites/       # Favorites screen
│   ├── settings/        # Settings screen
│   ├── components/      # Reusable UI components
│   └── theme/           # App theme and colors
├── utils/               # Constants and utilities
└── viewmodel/           # ViewModels for all screens
```

## Key Features Implementation

### MVVM Architecture

- **Model**: Data classes and Room entities
- **View**: Jetpack Compose screens
- **ViewModel**: State management with StateFlow

### YouTube Integration

- Uses YouTube Data API v3 for:
  - Video search
  - Playlist items retrieval
- Uses YouTube Player API for video playback

### AdMob Integration

- **Banner Ads**: Displayed at the bottom of Home screen
- **Interstitial Ads**: Shown before playing videos
- **Rewarded Ads**: Available for unlocking special features (optional)

### Database

- **Room database** for storing favorite videos locally
- **Firestore** for cloud storage of videos and categories
- **Favorites Category**: Automatically created category that organizes all favorited videos
- Flow-based reactive data updates

## Testing

### Alpha Testing

For Google Play Console alpha testing, use the `alpha` build variant:

```bash
# Build alpha AAB
./gradlew bundleAlpha
```

**Alpha Build Features:**
- ✅ Production-like build (minified, signed, optimized)
- ✅ Ads disabled (`ADS_ENABLED = false`)
- ✅ Features unlock automatically (fake rewards for testing)
- ✅ Safe for Play Console testing

**See `PLAY_CONSOLE_ALPHA_DESCRIPTION.md` for Play Console upload instructions and release notes.**

### Production Builds

For production releases, use the `release` build variant:

```bash
# Build release AAB
./gradlew bundleRelease
```

**Production Build Features:**
- ✅ Ads enabled (`ADS_ENABLED = true`)
- ✅ Production AdMob IDs required
- ✅ All features require real ads for unlocks

**Note:** The app uses production AdMob IDs from `local.properties`. Make sure to add your actual Ad IDs before building for production.

## Requirements

- Android Studio Hedgehog or later
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 36
- Kotlin 2.0.21
- Gradle 8.13.1

## Security Best Practices

- ✅ **API Keys**: Stored securely in `local.properties` (excluded from version control)
- ✅ **Never commit**: `local.properties` is in `.gitignore` - never commit API keys
- ⚠️ **Production**: Replace test Ad IDs with production AdMob IDs before release
- ⚠️ **Playlist IDs**: Replace placeholder playlist IDs in `Constants.kt` with real YouTube playlist IDs

## Notes

- The app uses dark theme by default with bright, kid-friendly accent colors
- All API keys are loaded from `local.properties` for security
- Test Ad IDs are used for development - replace with production IDs before publishing
- Replace all placeholder playlist IDs before building for production
- Make sure to enable YouTube Data API v3 in your Google Cloud Console

## License

This project is created for educational purposes.

## Support

For issues or questions, please check:
- [YouTube Data API Documentation](https://developers.google.com/youtube/v3)
- [AdMob Documentation](https://developers.google.com/admob/android/quick-start)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)

---

**Made with ❤️ for kids**

