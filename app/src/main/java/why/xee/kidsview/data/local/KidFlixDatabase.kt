package why.xee.kidsview.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for KidsView app
 */
@Database(
    entities = [FavoriteVideo::class],
    version = 1,
    exportSchema = false
)
abstract class KidsViewDatabase : RoomDatabase() {
    abstract fun favoriteVideoDao(): FavoriteVideoDao
}

