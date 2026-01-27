package why.xee.kidsview.utils

/**
 * Video category data class
 */
data class Category(
    val id: String,
    val name: String,
    val playlistId: String,
    val icon: String, // Emoji or icon identifier
    val hasSubCategories: Boolean = false
)

/**
 * Sub-category data class
 */
data class SubCategory(
    val id: String,
    val name: String,
    val searchQuery: String, // Search query for this sub-category
    val icon: String = "📺"
)

/**
 * Predefined categories for the app
 */
object Categories {
    val allCategories = listOf(
        Category("cartoons", "Cartoons", Constants.PLAYLIST_CARTOONS, "🎬", hasSubCategories = true),
        Category("kids_shows", "Kids Shows", Constants.PLAYLIST_KIDS_SHOWS, "📺"),
        Category("nursery_rhymes", "Nursery Rhymes", Constants.PLAYLIST_NURSERY_RHYMES, "🎵"),
        Category("anime", "Anime", Constants.PLAYLIST_ANIME, "🎌"),
        Category("kids_songs", "Kids Songs", Constants.PLAYLIST_KIDS_SONGS, "🎶"),
        Category("educational", "Educational", Constants.PLAYLIST_EDUCATIONAL, "📚"),
        Category("superheroes", "Superheroes", Constants.PLAYLIST_SUPERHEROES, "🦸"),
        Category("english_learning", "English Learning", Constants.PLAYLIST_ENGLISH_LEARNING, "🔤")
    )
    
    fun getCategoryById(id: String): Category? {
        return allCategories.find { it.id == id }
    }
    
    /**
     * Get sub-categories for a main category
     */
    fun getSubCategories(categoryId: String): List<SubCategory> {
        return when (categoryId) {
            "cartoons" -> cartoonsSubCategories
            else -> emptyList()
        }
    }
    
    /**
     * Get a specific sub-category by its ID within a main category.
     */
    fun getSubCategoryById(categoryId: String, subCategoryId: String): SubCategory? {
        return getSubCategories(categoryId).find { it.id == subCategoryId }
    }
    
    /**
     * Sub-categories for Cartoons
     */
    private val cartoonsSubCategories = listOf(
        SubCategory("pakistani", "Pakistani Cartoons", "Pakistani cartoons for kids", "🇵🇰"),
        SubCategory("indian", "Indian Cartoons", "Indian cartoons for kids", "🇮🇳"),
        SubCategory("english", "English Cartoons", "English cartoons for kids", "🇬🇧"),
        SubCategory("arabic", "Arabic Cartoons", "Arabic cartoons for kids", "🇸🇦"),
        SubCategory("all", "All Cartoons", "cartoons for kids", "🎬")
    )
}

