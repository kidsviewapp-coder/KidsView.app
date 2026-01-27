package why.xee.kidsview.utils

/**
 * App constants
 */
object Constants {
    
    // YouTube API Base URL
    const val YOUTUBE_API_BASE_URL = "https://www.googleapis.com/youtube/v3/"
    
    // Category Playlist IDs
    // OPTIONAL: Replace these with actual YouTube playlist IDs for better content curation
    // If left empty, the app will use search queries instead (works fine, but less curated)
    // To find playlist IDs:
    // 1. Go to a YouTube playlist
    // 2. The playlist ID is in the URL: youtube.com/playlist?list=PLAYLIST_ID_HERE
    // 3. Copy the ID and replace the empty string below
    // Example: const val PLAYLIST_CARTOONS = "PLrAXtmRdnEQy6nuLMH7uL0xQqFXL0o2l8"
    const val PLAYLIST_CARTOONS = "" // Optional: Add YouTube playlist ID for curated cartoons
    const val PLAYLIST_KIDS_SHOWS = "" // Optional: Add YouTube playlist ID for curated kids shows
    const val PLAYLIST_NURSERY_RHYMES = "" // Optional: Add YouTube playlist ID for curated nursery rhymes
    const val PLAYLIST_ANIME = "" // Optional: Add YouTube playlist ID for curated anime
    const val PLAYLIST_KIDS_SONGS = "" // Optional: Add YouTube playlist ID for curated kids songs
    const val PLAYLIST_EDUCATIONAL = "" // Optional: Add YouTube playlist ID for curated educational content
    const val PLAYLIST_SUPERHEROES = "" // Optional: Add YouTube playlist ID for curated superhero content
    const val PLAYLIST_ENGLISH_LEARNING = "" // Optional: Add YouTube playlist ID for curated English learning content
    
    // AdMob Ad IDs - Loaded from BuildConfig (set in build.gradle.kts from local.properties)
    // IMPORTANT: Add your production AdMob IDs to local.properties before release:
    // admob.app.id=YOUR_APP_ID
    // admob.banner.id=YOUR_BANNER_ID
    // admob.interstitial.id=YOUR_INTERSTITIAL_ID
    // admob.rewarded.id=YOUR_REWARDED_ID
    // For debug builds, test IDs are used automatically
    // Using getter functions to access BuildConfig at runtime
    @JvmStatic
    fun getAdMobAppId(): String = why.xee.kidsview.BuildConfig.ADMOB_APP_ID
    
    @JvmStatic
    fun getAdMobBannerId(): String = why.xee.kidsview.BuildConfig.ADMOB_BANNER_ID
    
    @JvmStatic
    fun getAdMobInterstitialId(): String = why.xee.kidsview.BuildConfig.ADMOB_INTERSTITIAL_ID
    
    @JvmStatic
    fun getAdMobRewardedId(): String = why.xee.kidsview.BuildConfig.ADMOB_REWARDED_ID
    
    // Properties for backward compatibility - access BuildConfig directly
    val ADMOB_BANNER_AD_ID: String 
        get() = why.xee.kidsview.BuildConfig.ADMOB_BANNER_ID
    
    val ADMOB_INTERSTITIAL_AD_ID: String 
        get() = why.xee.kidsview.BuildConfig.ADMOB_INTERSTITIAL_ID
    
    val ADMOB_REWARDED_AD_ID: String 
        get() = why.xee.kidsview.BuildConfig.ADMOB_REWARDED_ID
    
    // Database
    const val DATABASE_NAME = "kidflix_database"
}

