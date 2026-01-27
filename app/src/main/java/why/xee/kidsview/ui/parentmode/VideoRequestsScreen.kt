package why.xee.kidsview.ui.parentmode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import why.xee.kidsview.data.model.RequestStatus
import why.xee.kidsview.data.model.RequestType
import why.xee.kidsview.data.model.VideoRequest
import why.xee.kidsview.ui.viewmodel.VideoRequestsViewModel
import why.xee.kidsview.ui.components.BannerAd
import why.xee.kidsview.ui.components.ElegantAlertDialog
import why.xee.kidsview.utils.AdManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoRequestsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearchWithQuery: (String) -> Unit = {}, // Navigate to search with query
    viewModel: VideoRequestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedRequestForReject by remember { mutableStateOf<VideoRequest?>(null) }
    var rejectionMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadRequests()
        AdManager.getInstance().loadInterstitialAd(context, true)
        AdManager.getInstance().loadRewardedAd(context, true)
    }
    
    // Show success message from ViewModel
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    // Show error message from ViewModel
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar("Error: $error")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Video Requests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.requests.isEmpty() && !uiState.isLoading) {
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
                items(uiState.requests) { request ->
                    RequestCard(
                        request = request,
                        thumbnailUrl = if (request.type == RequestType.DELETE && request.videoId != null) {
                            uiState.videoThumbnails[request.videoId]
                        } else null,
                        onCopy = {
                            // Only copy the video name (for DELETE requests)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Video Name", request.videoName)
                            clipboard.setPrimaryClip(clip)
                            scope.launch {
                                snackbarHostState.showSnackbar("Video name copied to clipboard")
                            }
                        },
                        onSearch = {
                            // Navigate to search page with video name (for ADD requests)
                            onNavigateToSearchWithQuery(request.videoName)
                        },
                        onApprove = { 
                            viewModel.updateRequestStatus(request.requestId, RequestStatus.APPROVED)
                            // Show success message
                            scope.launch {
                                if (request.type == RequestType.DELETE) {
                                    snackbarHostState.showSnackbar("Video removed successfully")
                                } else {
                                    snackbarHostState.showSnackbar("Request approved")
                                    // For ADD requests, navigate to search page with video name
                                    if (request.type == RequestType.ADD) {
                                        onNavigateToSearchWithQuery(request.videoName)
                                    }
                                }
                            }
                        },
                        onReject = {
                            selectedRequestForReject = request
                            rejectionMessage = ""
                            showRejectDialog = true
                        }
                    )
                }
            }
        }
            
            // Fixed Banner Ad at bottom of screen (always visible in Parent Mode)
            BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                isParentMode = true
            )
        }
        
        // Reject Dialog
        if (showRejectDialog && selectedRequestForReject != null) {
            ElegantAlertDialog(
                onDismissRequest = { showRejectDialog = false },
                title = { Text("Reject Request") },
                text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please provide a reason for rejection (this will be shown to your kid):")
                    OutlinedTextField(
                        value = rejectionMessage,
                        onValueChange = { rejectionMessage = it },
                        label = { Text("Rejection Message") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter reason for rejection...") },
                        maxLines = 3
                    )
                }
            },
                confirmButton = {
                    Button(
                    onClick = {
                        selectedRequestForReject?.let { request ->
                            viewModel.updateRequestStatus(
                                request.requestId,
                                RequestStatus.REJECTED,
                                rejectionMessage
                            )
                        }
                        showRejectDialog = false
                        rejectionMessage = ""
                        selectedRequestForReject = null
                    },
                    enabled = rejectionMessage.isNotBlank()
                ) {
                    Text("Reject")
                }
            },
                dismissButton = {
                    TextButton(onClick = {
                        showRejectDialog = false
                        rejectionMessage = ""
                        selectedRequestForReject = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun RequestCard(
    request: VideoRequest,
    thumbnailUrl: String? = null,
    onCopy: () -> Unit,
    onSearch: () -> Unit = {}, // For ADD requests - navigate to search
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (request.status) {
                RequestStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                RequestStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
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
                    text = if (request.type == RequestType.ADD) "➕ Add" else "➖ Remove",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = request.status.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show thumbnail for DELETE requests if available
            if (request.type == RequestType.DELETE && thumbnailUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = request.videoName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = request.videoName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (request.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kid's message: ${request.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (request.rejectionMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "Rejection reason: ${request.rejectionMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show "Search" button for ADD requests, "Copy Name" for DELETE requests
                if (request.type == RequestType.ADD) {
                    TextButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search Video", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search")
                    }
                } else {
                    // DELETE requests - show Copy Name button
                    TextButton(onClick = onCopy) {
                        Icon(Icons.Default.Share, contentDescription = "Copy Video Name", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Name")
                    }
                }
                if (request.status == RequestStatus.PENDING) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onReject,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reject")
                    }
                    Button(onClick = onApprove) {
                        Text("Approve")
                    }
                }
            }
        }
    }
}

fun getRequestText(request: VideoRequest): String {
    return buildString {
        append("Type: ${request.type.name}\n")
        append("Video: ${request.videoName}\n")
        if (request.message.isNotEmpty()) {
            append("Message: ${request.message}\n")
        }
        append("Status: ${request.status.name}")
    }
}

