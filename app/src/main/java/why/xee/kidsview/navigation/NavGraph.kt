package why.xee.kidsview.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import why.xee.kidsview.ui.favorites.FavoritesScreen
import why.xee.kidsview.ui.home.HomeScreen
import why.xee.kidsview.ui.player.PlayerScreen
import why.xee.kidsview.ui.search.SearchScreen
import why.xee.kidsview.ui.category.CategoryScreen
import why.xee.kidsview.ui.subcategory.SubCategoryScreen
import why.xee.kidsview.ui.settings.SettingsScreen
import why.xee.kidsview.ui.privacy.PrivacyPolicyScreen
import why.xee.kidsview.ui.about.AboutScreen
import why.xee.kidsview.ui.welcome.WelcomeScreen
import why.xee.kidsview.ui.welcome.NewYearSplashScreen
import why.xee.kidsview.ui.parentmode.ParentModeScreen
import why.xee.kidsview.ui.parentmode.ParentSearchScreen
import why.xee.kidsview.ui.parentmode.ParentVideoDetailsScreen
import why.xee.kidsview.ui.parentmode.CategoryManagementScreen
import why.xee.kidsview.ui.parentmode.CategoryVideosScreen
import why.xee.kidsview.ui.parentmode.ParentSettingsScreenComposable
import why.xee.kidsview.ui.parentmode.CartoonBrowserScreen
import why.xee.kidsview.ui.parentmode.MyVideosScreen
import why.xee.kidsview.ui.parentmode.MyVideosScreen
import why.xee.kidsview.ui.kidsmode.KidsListScreen
import why.xee.kidsview.ui.kidsmode.VideoRequestScreen
import why.xee.kidsview.ui.parentmode.VideoRequestsScreen
import why.xee.kidsview.ui.parentmode.PinRecoveryScreen
import why.xee.kidsview.ui.parentmode.SecurityQuestionsSetupScreen
import why.xee.kidsview.utils.Categories
import why.xee.kidsview.data.preferences.PreferencesManager
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

/**
 * Navigation graph for the app
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    onThemeChange: (String) -> Unit = {},
    onPasswordVerified: () -> Unit = {}
) {
    // Check if we should show New Year splash
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context.applicationContext) }
    
    val shouldShowNewYearSplash = remember {
        // Check if user has disabled it
        if (preferencesManager.isNewYearSplashDisabled()) return@remember false
        
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-11, where 0 is January
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Show from today onwards - for testing, show it always (if not disabled)
        // In production, you can restrict this to: isJanuary || isDecember
        // This allows testing from today regardless of the current date
        true
    }
    
    NavHost(
        navController = navController,
        startDestination = if (shouldShowNewYearSplash) Screen.NewYearSplash.route else Screen.Welcome.route
    ) {
        composable(Screen.NewYearSplash.route) {
            NewYearSplashScreen(
                onSkip = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.NewYearSplash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onParentMode = {
                    navController.navigate(Screen.ParentMode.route)
                },
                onKidsMode = {
                    navController.navigate(Screen.KidsList.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCategory = { categoryId ->
                    navController.navigate(Screen.Category.createRoute(categoryId))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToPlayer = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                }
            )
        }
        
        composable(
            route = Screen.Category.route,
            arguments = listOf(navArgument("categoryId") {})
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val category = Categories.getCategoryById(categoryId)
            
            // If category has sub-categories, show sub-category screen
            if (category?.hasSubCategories == true) {
                SubCategoryScreen(
                    categoryId = categoryId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubCategory = { catId, subCatId ->
                        navController.navigate(Screen.SubCategory.createRoute(catId, subCatId))
                    }
                )
            } else {
                // Otherwise, show videos directly
                CategoryScreen(
                    categoryId = categoryId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { videoId ->
                        navController.navigate(Screen.Player.createRoute(videoId))
                    }
                )
            }
        }
        
        composable(
            route = Screen.SubCategory.route,
            arguments = listOf(
                navArgument("categoryId") {},
                navArgument("subCategoryId") {}
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val subCategoryId = backStackEntry.arguments?.getString("subCategoryId") ?: ""
            CategoryScreen(
                categoryId = categoryId,
                subCategoryId = subCategoryId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                }
            )
        }
        
        composable(
            route = "player/{videoId}/{isParentMode}",
            arguments = listOf(
                navArgument("videoId") {},
                navArgument("isParentMode") {
                    defaultValue = false
                    type = androidx.navigation.NavType.BoolType
                }
            )
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            // Get isParentMode from arguments - defaults to false if not provided
            // Favorites and parent mode videos explicitly pass true to use parent mode player with back button
            val isParentMode = backStackEntry.arguments?.getBoolean("isParentMode") ?: false
            PlayerScreen(
                videoId = videoId,
                isParentMode = isParentMode, // true for favorites and parent mode, false for kids mode
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { videoId ->
                    // Favorites screen uses parent mode player with back button
                    // Explicitly pass isParentMode = true to use parent mode player
                    val route = Screen.Player.createRoute(videoId, isParentMode = true)
                    navController.navigate(route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onThemeChange = onThemeChange,
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.About.route)
                }
            )
        }
        
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Parent Mode screens
        composable(Screen.ParentMode.route) {
            val previousRoute = remember {
                navController.previousBackStackEntry?.destination?.route
            }
            
            ParentModeScreen(
                onNavigateBack = {
                    // Try to pop back, if nothing to pop, navigate to Welcome
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.ParentMode.route) { inclusive = true }
                        }
                    }
                },
                onPinVerified = { context ->
                    // Notify that password was verified - this unlocks the session
                    onPasswordVerified()
                    
                    // Always go directly to ParentSearch (whether from KidsList or normal entry)
                    navController.navigate(Screen.ParentSearch.route) {
                        popUpTo(Screen.ParentMode.route) { inclusive = true }
                    }
                },
                onForgotPin = {
                    navController.navigate(Screen.PinRecovery.route)
                }
            )
        }
        
        // ParentSearch without query (normal navigation)
        composable("parent_search") {
            ParentSearchScreen(
                onNavigateBack = {
                    // Try to pop back, if nothing to pop, navigate to Welcome
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo("parent_search") { inclusive = true }
                        }
                    }
                },
                onVideoSelected = { videoId ->
                    navController.navigate(Screen.ParentVideoDetails.createRoute(videoId))
                },
                onNavigateToCategoryManagement = {
                    navController.navigate(Screen.CategoryManagement.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.ParentSettings.route)
                },
                onNavigateToVideoRequests = {
                    navController.navigate(Screen.VideoRequests.route)
                },
                onNavigateToCartoonBrowse = {
                    navController.navigate(Screen.CartoonBrowse.route)
                },
                onNavigateToMyVideos = {
                    navController.navigate(Screen.MyVideos.route)
                },
                onNavigateToCategoryVideos = { categoryId, categoryName ->
                    navController.navigate(Screen.CategoryVideos.createRoute(categoryId, categoryName))
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                }
            )
        }
        
        // ParentSearch with query (from approved video request)
        composable(
            route = "parent_search?query={query}",
            arguments = listOf(
                navArgument("query") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query")
            ParentSearchScreen(
                onNavigateBack = {
                    // Try to pop back, if nothing to pop, navigate to Welcome
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo("parent_search") { inclusive = true }
                        }
                    }
                },
                onVideoSelected = { videoId ->
                    navController.navigate(Screen.ParentVideoDetails.createRoute(videoId))
                },
                onNavigateToCategoryManagement = {
                    navController.navigate(Screen.CategoryManagement.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.ParentSettings.route)
                },
                onNavigateToVideoRequests = {
                    navController.navigate(Screen.VideoRequests.route)
                },
                onNavigateToCartoonBrowse = {
                    navController.navigate(Screen.CartoonBrowse.route)
                },
                onNavigateToMyVideos = {
                    navController.navigate(Screen.MyVideos.route)
                },
                onNavigateToCategoryVideos = { categoryId, categoryName ->
                    navController.navigate(Screen.CategoryVideos.createRoute(categoryId, categoryName))
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                initialSearchQuery = query
            )
        }
        
        composable(Screen.ParentSettings.route) {
            ParentSettingsScreenComposable(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.ParentSearch.route)
                    }
                },
                onThemeChange = onThemeChange, // Pass theme change callback
                onNavigateToSecurityQuestionsSetup = {
                    navController.navigate(Screen.SecurityQuestionsSetup.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.About.route)
                }
            )
        }
        
        composable(Screen.SecurityQuestionsSetup.route) {
            SecurityQuestionsSetupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.MyVideos.route) {
            MyVideosScreen(
                onNavigateBack = { navController.popBackStack() },
                onVideoSelected = { videoId ->
                    navController.navigate(Screen.ParentVideoDetails.createRoute(videoId))
                }
            )
        }
        
        composable(Screen.CartoonBrowse.route) {
            CartoonBrowserScreen(
                onNavigateBack = { navController.popBackStack() },
                onVideoSelected = { videoId ->
                    navController.navigate(Screen.ParentVideoDetails.createRoute(videoId))
                }
            )
        }
        
        composable(Screen.CategoryManagement.route) {
            CategoryManagementScreen(
                onNavigateBack = {
                    // Try to pop back, if nothing to pop, navigate to Welcome
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.CategoryManagement.route) { inclusive = true }
                        }
                    }
                },
                onViewCategoryVideos = { categoryId, categoryName ->
                    navController.navigate(Screen.CategoryVideos.createRoute(categoryId, categoryName))
                }
            )
        }
        
        composable(
            route = Screen.CategoryVideos.route,
            arguments = listOf(
                navArgument("categoryId") {},
                navArgument("categoryName") {}
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryVideosScreen(
                categoryId = categoryId,
                categoryName = categoryName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVideoDetails = { videoId ->
                    navController.navigate(Screen.ParentVideoDetails.createRoute(videoId))
                },
                onNavigateToPlayer = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                }
            )
        }
        
        composable(
            route = Screen.ParentVideoDetails.route,
            arguments = listOf(navArgument("videoId") {})
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            ParentVideoDetailsScreen(
                videoId = videoId,
                onNavigateBack = {
                    // Try to pop back, if nothing to pop, navigate to Welcome
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.ParentVideoDetails.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToPlayer = { playerVideoId ->
                    // Parent search videos use parent mode player with back button
                    navController.navigate(Screen.Player.createRoute(playerVideoId, isParentMode = true))
                }
            )
        }
        
        // Kids Mode screen
        composable(Screen.KidsList.route) {
            KidsListScreen(
                onNavigateBack = {
                    // Try to pop back, if nothing to pop, navigate to Welcome
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.KidsList.route) { inclusive = true }
                        }
                    }
                },
                onVideoSelected = { videoId ->
                    navController.navigate(Screen.Player.createRoute(videoId))
                },
                onNavigateToUnlock = {
                    // Navigate to ParentModeScreen for unlocking
                    navController.navigate(Screen.ParentMode.route) {
                        // Don't pop KidsList so we can return to it after unlock
                    }
                },
                onNavigateToVideoRequest = {
                    navController.navigate(Screen.VideoRequest.route)
                },
                onNavigateToParentMode = {
                    // Navigate to ParentModeScreen for parent access
                    navController.navigate(Screen.ParentMode.route) {
                        // Don't pop KidsList so we can return to it after authentication
                    }
                }
            )
        }
        
        composable(Screen.VideoRequest.route) {
            VideoRequestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.VideoRequests.route) {
            VideoRequestsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSearchWithQuery = { query ->
                    // Navigate to ParentSearch with the search query
                    navController.navigate(Screen.ParentSearch.createRouteWithQuery(query)) {
                        // Pop VideoRequests from back stack
                        popUpTo(Screen.ParentSearch.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(Screen.PinRecovery.route) {
            PinRecoveryScreen(
                onNavigateBack = { navController.popBackStack() },
                onPinRecovered = {
                    navController.popBackStack()
                }
            )
        }
    }
}

