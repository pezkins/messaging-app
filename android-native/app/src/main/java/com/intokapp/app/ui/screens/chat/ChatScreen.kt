package com.intokapp.app.ui.screens.chat

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.intokapp.app.data.models.Message
import com.intokapp.app.data.models.MessageStatus
import com.intokapp.app.data.models.MessageType
import com.intokapp.app.data.network.GiphyGif
import com.intokapp.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Emoji options for reactions
private val REACTION_EMOJIS = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Selected message for reactions
    var selectedMessageForReaction by remember { mutableStateOf<Message?>(null) }
    
    // Full screen image viewer
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    
    // Camera capture URI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.sendImage(it) }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.sendImage(cameraImageUri!!)
        }
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Create temp file and launch camera
            val photoFile = File.createTempFile("IMG_", ".jpg", context.cacheDir)
            cameraImageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            cameraImageUri?.let { cameraLauncher.launch(it) }
        }
    }
    
    // Document picker launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.sendDocument(it) }
    }
    
    // Show error messages via Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }
    
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearConversation()
        }
    }
    
    // Full screen image dialog
    fullScreenImageUrl?.let { url ->
        Dialog(onDismissRequest = { fullScreenImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { fullScreenImageUrl = null }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Full screen image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                IconButton(
                    onClick = { fullScreenImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.displayName,
                            color = White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.typingUsers.isNotEmpty()) {
                            Text(
                                text = "typing...",
                                color = Surface400,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, null, tint = White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, null, tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface950)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Surface950, Surface900)
                    )
                )
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (uiState.hasMoreMessages) {
                        item {
                            TextButton(
                                onClick = { viewModel.loadMoreMessages() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Load more...", color = Purple500)
                            }
                        }
                    }
                    
                    items(uiState.messages) { message ->
                        MessageBubble(
                            message = message,
                            isOwnMessage = message.senderId == uiState.currentUserId || message.id.startsWith("temp-"),
                            currentUserId = uiState.currentUserId,
                            onLongPress = { selectedMessageForReaction = message },
                            onReactionClick = { emoji -> viewModel.sendReaction(message, emoji) },
                            onImageClick = { url -> fullScreenImageUrl = url },
                            onGetDownloadUrl = { key -> viewModel.getDownloadUrl(key) }
                        )
                    }
                }
                
                // Typing Indicator
                if (uiState.typingUsers.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Someone is typing...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Surface400
                        )
                    }
                }
                
                // Upload progress indicator
                if (uiState.isUploading) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = uiState.uploadProgress,
                            modifier = Modifier.fillMaxWidth(),
                            color = Purple500,
                            trackColor = Surface700
                        )
                        Text(
                            text = "Uploading... ${(uiState.uploadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Surface400,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Attachment picker
                if (uiState.showAttachmentPicker) {
                    AttachmentPickerRow(
                        onImageClick = {
                            imagePickerLauncher.launch("image/*")
                            viewModel.hideAttachmentPicker()
                        },
                        onCameraClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            viewModel.hideAttachmentPicker()
                        },
                        onGifClick = {
                            viewModel.toggleGifPicker()
                        },
                        onDocumentClick = {
                            documentPickerLauncher.launch(arrayOf(
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "text/plain"
                            ))
                            viewModel.hideAttachmentPicker()
                        }
                    )
                }
                
                // GIF picker
                if (uiState.showGifPicker) {
                    GifPickerSheet(
                        gifs = uiState.gifs,
                        searchQuery = uiState.gifSearchQuery,
                        isLoading = uiState.isLoadingGifs,
                        onSearchQueryChange = { viewModel.searchGifs(it) },
                        onGifSelected = { gif -> viewModel.sendGif(gif) },
                        onDismiss = { viewModel.hideGifPicker() }
                    )
                }
                
                // Input
                Surface(
                    color = Surface900,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleAttachmentPicker() }) {
                            Icon(
                                Icons.Default.Add,
                                null,
                                tint = Purple500,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { 
                                messageText = it
                                viewModel.setTyping(it.isNotEmpty())
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message...", color = Surface500) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedBorderColor = Purple500,
                                unfocusedBorderColor = Surface700,
                                focusedContainerColor = Surface800,
                                unfocusedContainerColor = Surface800
                            ),
                            maxLines = 5
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    viewModel.sendMessage(messageText.trim())
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                null,
                                tint = if (messageText.isNotBlank()) Purple500 else Surface600,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            
            // Reaction picker overlay
            selectedMessageForReaction?.let { message ->
                ReactionPicker(
                    onReactionSelected = { emoji ->
                        viewModel.sendReaction(message, emoji)
                        selectedMessageForReaction = null
                    },
                    onDismiss = { selectedMessageForReaction = null }
                )
            }
        }
    }
}

@Composable
private fun AttachmentPickerRow(
    onImageClick: () -> Unit,
    onCameraClick: () -> Unit,
    onGifClick: () -> Unit,
    onDocumentClick: () -> Unit
) {
    Surface(
        color = Surface800,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentOption(
                icon = Icons.Default.Image,
                label = "Gallery",
                onClick = onImageClick
            )
            AttachmentOption(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                onClick = onCameraClick
            )
            AttachmentOption(
                icon = Icons.Default.Gif,
                label = "GIF",
                onClick = onGifClick
            )
            AttachmentOption(
                icon = Icons.Default.Description,
                label = "Document",
                onClick = onDocumentClick
            )
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = Purple500.copy(alpha = 0.2f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = Purple500,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Surface400
        )
    }
}

@Composable
private fun ReactionPicker(
    onReactionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Surface800,
            modifier = Modifier.clickable(enabled = false) {} // Prevent dismiss when clicking on picker
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                REACTION_EMOJIS.forEach { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = Surface700,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onReactionSelected(emoji) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GifPickerSheet(
    gifs: List<GiphyGif>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onGifSelected: (GiphyGif) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Debounce search
    var debouncedQuery by remember { mutableStateOf(searchQuery) }
    LaunchedEffect(debouncedQuery) {
        delay(300)
        onSearchQueryChange(debouncedQuery)
    }
    
    Surface(
        color = Surface800,
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GIFs",
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    fontWeight = FontWeight.SemiBold
                )
                Row {
                    Text(
                        "Powered by GIPHY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Surface500
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Surface400)
                    }
                }
            }
            
            // Search bar
            OutlinedTextField(
                value = debouncedQuery,
                onValueChange = { debouncedQuery = it },
                placeholder = { Text("Search GIFs...", color = Surface500) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Surface500) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = Purple500,
                    unfocusedBorderColor = Surface700,
                    focusedContainerColor = Surface900,
                    unfocusedContainerColor = Surface900
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // GIF grid
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Purple500)
                }
            } else if (gifs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No GIFs found" else "Loading GIFs...",
                        color = Surface400
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(gifs) { gif ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(gif.previewUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "GIF",
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onGifSelected(gif) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    currentUserId: String?,
    onLongPress: () -> Unit,
    onReactionClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onGetDownloadUrl: suspend (String) -> String?
) {
    // Default to showing translated content, toggle to see original
    var showOriginal by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // For attachment URLs
    var attachmentUrl by remember { mutableStateOf<String?>(null) }
    
    // Load attachment URL if needed
    LaunchedEffect(message.attachment?.key) {
        message.attachment?.key?.let { key ->
            if (message.type == MessageType.IMAGE || message.type == MessageType.GIF) {
                // Check if it's already a URL or needs to be fetched
                if (key.startsWith("http")) {
                    attachmentUrl = key
                } else {
                    attachmentUrl = onGetDownloadUrl(key)
                }
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage) {
            // Avatar
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Purple500)
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (message.sender?.username ?: "?").take(1).uppercase(),
                    color = White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Sender name
            if (!isOwnMessage) {
                Text(
                    text = message.sender?.username ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall,
                    color = Surface400,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            
            // Bubble
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                    bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                ),
                color = if (isOwnMessage) Purple500 else Surface800
            ) {
                Column(modifier = Modifier.padding(if (message.type == MessageType.IMAGE || message.type == MessageType.GIF) 4.dp else 12.dp)) {
                    // Content based on message type
                    when (message.type) {
                        MessageType.IMAGE -> {
                            // Image message
                            val imageUrl = attachmentUrl ?: message.attachment?.url ?: message.originalContent
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(imageUrl) },
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        MessageType.GIF -> {
                            // GIF message
                            val gifUrl = attachmentUrl ?: message.attachment?.url ?: message.originalContent
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(gifUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "GIF",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(gifUrl) },
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        MessageType.FILE -> {
                            // Document message
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = "Document",
                                    tint = if (isOwnMessage) White else Purple500,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = message.attachment?.fileName ?: message.originalContent,
                                        color = White,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    message.attachment?.fileSize?.let { size ->
                                        Text(
                                            text = formatFileSize(size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOwnMessage) White.copy(alpha = 0.7f) else Surface400
                                        )
                                    }
                                }
                            }
                        }
                        
                        else -> {
                            // Text message (default)
                            Text(
                                text = if (showOriginal) 
                                    message.originalContent 
                                else 
                                    message.translatedContent ?: message.originalContent,
                                color = White
                            )
                            
                            // Translation toggle - only show if there's a translation available
                            if (message.translatedContent != null && message.translatedContent != message.originalContent) {
                                TextButton(
                                    onClick = { showOriginal = !showOriginal },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Public,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isOwnMessage) White.copy(alpha = 0.7f) else Purple500
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (showOriginal) "Translate" else "Original",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOwnMessage) White.copy(alpha = 0.7f) else Purple500
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Status and Time
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Surface500
                )
                
                if (isOwnMessage) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = when (message.status) {
                            MessageStatus.SENDING -> Icons.Default.Schedule
                            MessageStatus.SENT, null -> Icons.Default.Check
                            MessageStatus.DELIVERED -> Icons.Default.DoneAll
                            MessageStatus.SEEN -> Icons.Default.DoneAll
                            MessageStatus.FAILED -> Icons.Default.Error
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = when (message.status) {
                            MessageStatus.FAILED -> MaterialTheme.colorScheme.error
                            MessageStatus.SEEN -> Purple500
                            else -> Surface500
                        }
                    )
                }
            }
            
            // Reactions
            if (!message.reactions.isNullOrEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(message.reactions.entries.toList()) { (emoji, users) ->
                        if (users.isNotEmpty()) {
                            val isMyReaction = users.contains(currentUserId)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isMyReaction) Purple500.copy(alpha = 0.3f) else Surface800,
                                modifier = Modifier.clickable { onReactionClick(emoji) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(emoji)
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        "${users.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMyReaction) Purple500 else Surface400
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(isoString: String): String {
    return try {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        )
        
        var date: Date? = null
        for (format in formats) {
            format.timeZone = TimeZone.getTimeZone("UTC")
            try {
                date = format.parse(isoString)
                break
            } catch (e: Exception) { continue }
        }
        
        date?.let {
            SimpleDateFormat("h:mm a", Locale.US).format(it)
        } ?: ""
    } catch (e: Exception) { "" }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
