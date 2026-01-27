package why.xee.kidsview.ui.kidsmode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import why.xee.kidsview.data.model.FirestoreVideo
import why.xee.kidsview.data.model.RequestStatus
import why.xee.kidsview.data.model.RequestType
import why.xee.kidsview.data.model.VideoRequest
import why.xee.kidsview.ui.components.AnimatedSuccessMessage
import why.xee.kidsview.ui.viewmodel.VideoRequestViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoRequestScreen(
    onNavigateBack: () -> Unit,
    viewModel: VideoRequestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var requestType by remember { mutableStateOf(RequestType.ADD) }
    var videoName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedVideo by remember { mutableStateOf<FirestoreVideo?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    
    // Show success when request is submitted
    LaunchedEffect(uiState.requestSubmitted) {
        if (uiState.requestSubmitted) {
            showSuccess = true
            viewModel.refreshRequestHistory()
            // Auto-dismiss after 3 seconds
            kotlinx.coroutines.delay(3000)
            showSuccess = false
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.refreshRequestHistory()
    }
    
    // Mark all rejections as read when history is viewed
    LaunchedEffect(showHistory) {
        if (showHistory) {
            // When history screen is opened, mark all rejections as read
            viewModel.markAllRejectionsAsRead()
        }
    }
    

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showHistory) "Request History" else "Request Video") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showHistory) {
                            showHistory = false
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!showHistory) {
                        TextButton(onClick = { showHistory = true }) {
                            Text("History")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (showHistory) {
                // Request History View
                if (uiState.requestHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📝", style = MaterialTheme.typography.displayLarge)
                            Text("No requests yet")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.requestHistory) { request ->
                            RequestHistoryCard(request = request)
                        }
                    }
                }
            } else {
                // Request Form View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Success Message at the top
                    AnimatedSuccessMessage(
                        message = "Request submitted! Your parent will review it.",
                        visible = showSuccess,
                        onDismiss = { showSuccess = false }
                    )
            // Request Type Selection
            Text("Request Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = requestType == RequestType.ADD,
                    onClick = { 
                        requestType = RequestType.ADD
                        selectedVideo = null // Reset selection when switching types
                    },
                    label = { Text("Add Video") }
                )
                FilterChip(
                    selected = requestType == RequestType.DELETE,
                    onClick = { 
                        requestType = RequestType.DELETE
                        videoName = "" // Reset video name when switching types
                    },
                    label = { Text("Remove Video") }
                )
            }

            if (requestType == RequestType.DELETE) {
                if (uiState.videos.isEmpty()) {
                    // No videos available message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No videos available to remove",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Video selection for delete
                    Text("Select Video to Remove", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    uiState.videos.forEach { video ->
                        val isSelected = selectedVideo?.videoId == video.videoId
                        val isPendingDelete = uiState.pendingDeleteVideoIds.contains(video.videoId)
                        val shouldGreyOut = isSelected || isPendingDelete
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (shouldGreyOut) 0.5f else 1f), // Grey out if selected or pending delete
                            onClick = { 
                                // Don't allow selecting videos that already have pending delete requests
                                if (!isPendingDelete) {
                                    selectedVideo = video
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isPendingDelete -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(80.dp, 45.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = video.thumbnail,
                                        contentDescription = video.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                
                                // Video title
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (shouldGreyOut) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    // Show indicator if pending delete
                                    if (isPendingDelete) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                        ) {
                                            Text(
                                                text = "⏳ Already requested for deletion",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Video name input for add
                OutlinedTextField(
                    value = videoName,
                    onValueChange = { videoName = it },
                    label = { Text("Video Name") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter the name of the video you want to watch") }
                )
            }

            // Message (optional)
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Any additional message for your parent") },
                maxLines = 3
            )

            // Submit button
            Button(
                onClick = {
                    if (requestType == RequestType.ADD) {
                        if (videoName.isNotBlank()) {
                            viewModel.createRequest(RequestType.ADD, videoName, null, message)
                            // Reset form
                            videoName = ""
                            message = ""
                        }
                    } else {
                        // DELETE request
                        val video = selectedVideo
                        if (video != null && video.videoId != null) {
                            viewModel.createRequest(RequestType.DELETE, video.title, video.videoId, message)
                            // Reset form
                            message = ""
                            selectedVideo = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = if (requestType == RequestType.ADD) {
                    videoName.isNotBlank()
                } else {
                    selectedVideo != null && selectedVideo?.videoId != null
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Request")
            }
            } // Close Column
            } // Close else block
        } // Close Box
    } // Close Scaffold
}

@Composable
private fun RequestHistoryCard(request: VideoRequest) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (request.status) {
                RequestStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                RequestStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (request.type == RequestType.ADD) "➕ Add Video" else "➖ Remove Video",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (request.status) {
                        RequestStatus.APPROVED -> MaterialTheme.colorScheme.primary
                        RequestStatus.REJECTED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                ) {
                    Text(
                        text = request.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (request.status) {
                            RequestStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                            RequestStatus.REJECTED -> MaterialTheme.colorScheme.onError
                            else -> MaterialTheme.colorScheme.onSecondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = request.videoName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (request.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your message: ${request.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (request.rejectionMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📨 Message from Parent:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = request.rejectionMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(Date(request.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

