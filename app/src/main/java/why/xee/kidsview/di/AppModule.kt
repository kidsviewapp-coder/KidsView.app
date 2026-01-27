package why.xee.kidsview.di

import android.app.Application
import androidx.room.Room
import why.xee.kidsview.BuildConfig
import why.xee.kidsview.data.local.FavoriteVideoDao
import why.xee.kidsview.data.local.KidsViewDatabase
import why.xee.kidsview.utils.Constants
import why.xee.kidsview.utils.ApiKeyManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-level dependency injection module
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provides list of YouTube API keys from BuildConfig
     * Supports multiple keys for rotation based on hour of day
     * In local.properties: youtube.api.keys=KEY1,KEY2,KEY3
     */
    @Provides
    @Singleton
    fun provideYouTubeApiKeys(): List<String> {
        val keysString = BuildConfig.YOUTUBE_API_KEYS
        return if (keysString.isNotEmpty()) {
            keysString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            // Fallback to single key
            if (BuildConfig.YOUTUBE_API_KEY.isNotEmpty()) {
                listOf(BuildConfig.YOUTUBE_API_KEY)
            } else {
                emptyList()
            }
        }
    }
    
    /**
     * Provides API Key Manager for rotating keys based on hour
     */
    @Provides
    @Singleton
    fun provideApiKeyManager(apiKeys: List<String>): ApiKeyManager {
        return ApiKeyManager(apiKeys)
    }
    
    /**
     * Provides current YouTube API key (backwards compatibility)
     * Deprecated: Use ApiKeyManager instead for rotation
     */
    @Deprecated("Use ApiKeyManager for key rotation")
    @Provides
    @Singleton
    fun provideYouTubeApiKey(): String {
        return BuildConfig.YOUTUBE_API_KEY
    }
    
    /**
     * Provides Room database instance
     */
    @Provides
    @Singleton
    fun provideDatabase(application: Application): KidsViewDatabase {
        return Room.databaseBuilder(
            application,
            KidsViewDatabase::class.java,
            Constants.DATABASE_NAME
        ).build()
    }
    
    /**
     * Provides FavoriteVideoDao
     */
    @Provides
    @Singleton
    fun provideFavoriteVideoDao(database: KidsViewDatabase): FavoriteVideoDao {
        return database.favoriteVideoDao()
    }
    
    /**
     * Provides Firebase Firestore instance
     * Used for caching video results to reduce YouTube API quota usage
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}

