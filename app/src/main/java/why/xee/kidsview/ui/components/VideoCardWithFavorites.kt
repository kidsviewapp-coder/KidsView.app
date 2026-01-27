package why.xee.kidsview.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import why.xee.kidsview.data.model.VideoItem
import why.xee.kidsview.ui.viewmodel.FavoritesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * VideoCard with integrated favorites functionality
 * Uses a shared FavoritesViewModel instance to maintain state consistency
 */
@Composable
fun VideoCardWithFavorites(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    var isFavorite by remember(video.id) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Check favorite status when video changes
    LaunchedEffect(video.id) {
        coroutineScope.launch {
            isFavorite = favoritesViewModel.isFavorite(video.id)
        }
    }
    
    // Refresh favorite state after toggling (with a small delay to allow DB update)
    LaunchedEffect(Unit) {
        // Initial check
        coroutineScope.launch {
            isFavorite = favoritesViewModel.isFavorite(video.id)
        }
    }
    
    VideoCard(
        video = video,
        onClick = onClick,
        modifier = modifier,
        isFavorite = isFavorite,
        onFavoriteClick = {
            // Optimistically update UI for better UX - immediate visual feedback
            val newState = !isFavorite
            isFavorite = newState
            
            coroutineScope.launch {
                // Toggle favorite in ViewModel
                favoritesViewModel.toggleFavorite(video)
                // Wait for database operation to complete
                delay(600) // Wait for Firestore sync
                // Verify state matches database (in case of error, revert)
                val actualState = favoritesViewModel.isFavorite(video.id)
                isFavorite = actualState // Always sync with database
            }
        }
    )
}

