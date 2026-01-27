package why.xee.kidsview.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for favorite videos
 */
@Dao
interface FavoriteVideoDao {
    
    @Query("SELECT * FROM favorite_videos ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteVideo>>
    
    @Query("SELECT * FROM favorite_videos WHERE videoId = :videoId")
    suspend fun getFavoriteById(videoId: String): FavoriteVideo?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteVideo)
    
    @Query("DELETE FROM favorite_videos WHERE videoId = :videoId")
    suspend fun deleteFavorite(videoId: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_videos WHERE videoId = :videoId)")
    suspend fun isFavorite(videoId: String): Boolean
}

