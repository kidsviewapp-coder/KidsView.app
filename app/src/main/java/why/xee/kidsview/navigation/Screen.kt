package why.xee.kidsview.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object NewYearSplash : Screen("new_year_splash")
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object Search : Screen("search")
    object Category : Screen("category/{categoryId}") {
        fun createRoute(categoryId: String) = "category/$categoryId"
    }
    object SubCategory : Screen("subcategory/{categoryId}/{subCategoryId}") {
        fun createRoute(categoryId: String, subCategoryId: String) = "subcategory/$categoryId/$subCategoryId"
    }
    object Player : Screen("player/{videoId}/{isParentMode}") {
        fun createRoute(videoId: String, isParentMode: Boolean = false) = "player/$videoId/${if (isParentMode) "true" else "false"}"
    }
    object Favorites : Screen("favorites")
    object Settings : Screen("settings")
    object PrivacyPolicy : Screen("privacy_policy")
    
    // Parent Mode screens
    object ParentMode : Screen("parent_mode")
    object ParentSearch : Screen("parent_search") {
        fun createRouteWithQuery(query: String) = "parent_search?query=${android.net.Uri.encode(query)}"
    }
    object ParentVideoDetails : Screen("parent_video_details/{videoId}") {
        fun createRoute(videoId: String) = "parent_video_details/$videoId"
    }
    object CategoryManagement : Screen("category_management")
    object CategoryVideos : Screen("category_videos/{categoryId}/{categoryName}") {
        fun createRoute(categoryId: String, categoryName: String) = "category_videos/$categoryId/$categoryName"
    }
    
    // Kids Mode screen
    object KidsList : Screen("kids_list")
    object VideoRequest : Screen("video_request")
    
    // Parent Settings screen
    object ParentSettings : Screen("parent_settings")
    object VideoRequests : Screen("video_requests")
    object PinRecovery : Screen("pin_recovery")
    object SecurityQuestionsSetup : Screen("security_questions_setup")
    object CartoonBrowse : Screen("cartoon_browse")
    object MyVideos : Screen("my_videos")
    object About : Screen("about")
}

