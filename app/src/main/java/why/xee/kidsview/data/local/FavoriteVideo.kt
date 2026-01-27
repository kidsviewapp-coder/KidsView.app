package why.xee.kidsview.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for favorite videos
 */
@Entity(tableName = "favorite_videos")
data class FavoriteVideo(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val description: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val addedAt: Long = System.currentTimeMillis()
)

