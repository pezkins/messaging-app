package com.intokapp.app.ui.screens.chat

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
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
import com.intokapp.app.ui.components.EmojiPickerSheet
import com.intokapp.app.ui.components.FrequentEmojiManager
import com.intokapp.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    
    // Show download success messages via Snackbar
    LaunchedEffect(uiState.downloadSuccess) {
        uiState.downloadSuccess?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearDownloadSuccess()
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
    
    // Document translation dialog
    if (uiState.showDocumentTranslationDialog) {
        DocumentTranslationDialog(
            fileName = uiState.pendingDocumentAttachment?.fileName ?: "Document",
            onTranslate = { viewModel.confirmSendDocument(translateDocument = true) },
            onSendWithoutTranslation = { viewModel.confirmSendDocument(translateDocument = false) },
            onDismiss = { viewModel.dismissDocumentDialog() }
        )
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
                .imePadding() // Scroll content when keyboard appears
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
                
                // Reply preview bar
                uiState.replyingTo?.let { replyMessage ->
                    ReplyPreviewBar(
                        message = replyMessage,
                        onClose = { viewModel.clearReply() }
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
                            placeholder = { 
                                Text(
                                    if (uiState.replyingTo != null) "Reply..." else "Message...", 
                                    color = Surface500
                                ) 
                            },
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
            
            // Message context menu (replaces simple reaction picker)
            selectedMessageForReaction?.let { message ->
                MessageContextMenu(
                    message = message,
                    isOwnMessage = message.senderId == uiState.currentUserId || message.id.startsWith("temp-"),
                    onReactionSelected = { emoji ->
                        viewModel.sendReaction(message, emoji)
                        selectedMessageForReaction = null
                    },
                    onReplyClick = {
                        viewModel.setReplyingTo(message)
                        selectedMessageForReaction = null
                    },
                    onCopyClick = {
                        selectedMessageForReaction = null
                    },
                    onDeleteClick = {
                        viewModel.showDeleteMessageDialog(message)
                        selectedMessageForReaction = null
                    },
                    onSaveToGallery = {
                        viewModel.saveImageToGallery(message)
                        selectedMessageForReaction = null
                    },
                    onDownloadDocument = {
                        viewModel.downloadDocument(message)
                        selectedMessageForReaction = null
                    },
                    onShowMoreEmojis = {
                        selectedMessageForReaction = null
                        viewModel.showEmojiPicker()
                    },
                    onDismiss = { selectedMessageForReaction = null }
                )
            }
        }
    }
    
    // Delete message confirmation dialog
    if (uiState.showDeleteDialog && uiState.messageToDelete != null) {
        DeleteMessageDialog(
            message = uiState.messageToDelete!!,
            isOwnMessage = uiState.messageToDelete!!.senderId == uiState.currentUserId,
            isDeleting = uiState.isDeletingMessage,
            onDeleteForMe = { viewModel.deleteMessage(forEveryone = false) },
            onDeleteForEveryone = { viewModel.deleteMessage(forEveryone = true) },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
    
    // Emoji picker bottom sheet
    if (uiState.showEmojiPicker) {
        EmojiPickerSheet(
            onEmojiSelected = { emoji ->
                // Find the last message to react to (for general emoji picker)
                uiState.messages.lastOrNull()?.let { msg ->
                    viewModel.sendReaction(msg, emoji)
                }
                viewModel.hideEmojiPicker()
            },
            onDismiss = { viewModel.hideEmojiPicker() }
        )
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
private fun ReplyPreviewBar(
    message: Message,
    onClose: () -> Unit
) {
    Surface(
        color = Surface800,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Purple accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(Purple500, RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Replying to ${message.sender?.username ?: "Unknown"}",
                    color = Purple500,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (message.type) {
                        MessageType.IMAGE -> "📷 Photo"
                        MessageType.GIF -> "🎬 GIF"
                        MessageType.FILE -> "📄 ${message.attachment?.fileName ?: "Document"}"
                        else -> message.originalContent.take(50) + if (message.originalContent.length > 50) "..." else ""
                    },
                    color = Surface400,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel reply",
                    tint = Surface400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageContextMenu(
    message: Message,
    isOwnMessage: Boolean,
    onReactionSelected: (String) -> Unit,
    onReplyClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveToGallery: () -> Unit,
    onDownloadDocument: () -> Unit,
    onShowMoreEmojis: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val frequentManager = remember { FrequentEmojiManager(context) }
    val frequentEmojis = remember { frequentManager.getTopFrequent(5) }
    
    // Check if message is deleted
    val isDeleted = message.originalContent == "This message was deleted"
    
    // Check message type for download options
    val isImage = message.type == MessageType.IMAGE || message.type == MessageType.GIF
    val isDocument = message.type == MessageType.FILE || message.type == MessageType.ATTACHMENT
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Surface800,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clickable(enabled = false) {} // Prevent dismiss when clicking on menu
        ) {
            Column {
                // Quick emoji reactions row (hide for deleted messages)
                if (!isDeleted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        frequentEmojis.forEach { emoji ->
                            Surface(
                                shape = CircleShape,
                                color = Surface700,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { 
                                        frequentManager.recordUsage(emoji)
                                        onReactionSelected(emoji) 
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                        }
                        
                        // More emojis button
                        Surface(
                            shape = CircleShape,
                            color = Purple500.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { onShowMoreEmojis() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "More emojis",
                                    tint = Purple500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    Divider(color = Surface700, thickness = 1.dp)
                    
                    // Reply option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReplyClick() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Reply,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Reply", color = White, style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    // Copy option (only for text messages)
                    if (message.type == null || message.type == MessageType.TEXT) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(message.originalContent))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    onCopyClick()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Copy", color = White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    
                    // Save to Gallery option (for images and GIFs)
                    if (isImage) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSaveToGallery() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SaveAlt,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Save to Gallery", color = White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    
                    // Download option (for documents)
                    if (isDocument) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDownloadDocument() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Download", color = White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                
                // Delete option (always show unless message is already deleted)
                if (!isDeleted) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeleteClick() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotedMessageView(
    replyTo: com.intokapp.app.data.models.ReplyTo,
    isOwnMessage: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isOwnMessage) Purple600.copy(alpha = 0.5f) else Surface700.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Purple accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(
                        if (isOwnMessage) White.copy(alpha = 0.7f) else Purple500,
                        RoundedCornerShape(2.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = replyTo.senderName,
                    color = if (isOwnMessage) White else Purple500,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (replyTo.type?.lowercase()) {
                        "image" -> "📷 Photo"
                        "gif" -> "🎬 GIF"
                        "file" -> "📄 Document"
                        else -> replyTo.content
                    },
                    color = if (isOwnMessage) White.copy(alpha = 0.8f) else Surface400,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
    
    val hasReactions = !message.reactions.isNullOrEmpty()
    val isRead = !message.readBy.isNullOrEmpty()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .padding(bottom = if (hasReactions) 22.dp else 0.dp) // Extra space for reactions hanging below (10% overlap)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top // Avatar aligned to top
    ) {
        if (!isOwnMessage) {
            // Avatar - aligned to top of message
            val senderAvatarUrl = message.sender?.displayAvatarUrl
            
            Box(
                modifier = Modifier
                    .padding(end = 8.dp, top = 18.dp) // top padding to align with bubble (after sender name)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Purple500),
                contentAlignment = Alignment.Center
            ) {
                if (senderAvatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(senderAvatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Sender avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = (message.sender?.username ?: "?").take(1).uppercase(),
                        color = White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
            
            // Bubble with overlapping reactions
            Box {
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
                    Column(
                        modifier = Modifier.padding(
                            start = if (message.type == MessageType.IMAGE || message.type == MessageType.GIF) 4.dp else 12.dp,
                            end = if (message.type == MessageType.IMAGE || message.type == MessageType.GIF) 4.dp else 12.dp,
                            top = if (message.type == MessageType.IMAGE || message.type == MessageType.GIF) 4.dp else 12.dp,
                            bottom = if (message.type == MessageType.IMAGE || message.type == MessageType.GIF) 4.dp else 8.dp
                        )
                    ) {
                        // Quoted message view (if replying)
                        message.replyTo?.let { replyTo ->
                            QuotedMessageView(
                                replyTo = replyTo,
                                isOwnMessage = isOwnMessage
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Content based on message type
                        when (message.type) {
                            MessageType.IMAGE -> {
                                // Image message
                                val imageUrl = attachmentUrl ?: message.attachment?.url ?: message.originalContent
                                Box {
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
                                    // Timestamp overlay for images
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = formatTime(message.createdAt),
                                            fontSize = 11.sp,
                                            color = White.copy(alpha = 0.9f)
                                        )
                                        if (isOwnMessage) {
                                            Icon(
                                                imageVector = when {
                                                    message.status == MessageStatus.SENDING -> Icons.Default.Schedule
                                                    message.status == MessageStatus.FAILED -> Icons.Default.Error
                                                    isRead || message.status == MessageStatus.SEEN -> Icons.Default.DoneAll
                                                    message.status == MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                                    else -> Icons.Default.Check
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isRead || message.status == MessageStatus.SEEN) Purple300 else White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            MessageType.GIF -> {
                                // GIF message
                                val gifUrl = attachmentUrl ?: message.attachment?.url ?: message.originalContent
                                Box {
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
                                    // Timestamp overlay for GIFs
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = formatTime(message.createdAt),
                                            fontSize = 11.sp,
                                            color = White.copy(alpha = 0.9f)
                                        )
                                        if (isOwnMessage) {
                                            Icon(
                                                imageVector = when {
                                                    message.status == MessageStatus.SENDING -> Icons.Default.Schedule
                                                    message.status == MessageStatus.FAILED -> Icons.Default.Error
                                                    isRead || message.status == MessageStatus.SEEN -> Icons.Default.DoneAll
                                                    message.status == MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                                    else -> Icons.Default.Check
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isRead || message.status == MessageStatus.SEEN) Purple300 else White.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            MessageType.FILE -> {
                                // Document message
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = "Document",
                                            tint = if (isOwnMessage) White else Purple500,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
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
                                    // Timestamp for documents
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = formatTime(message.createdAt),
                                            fontSize = 11.sp,
                                            color = if (isOwnMessage) White.copy(alpha = 0.7f) else Surface500
                                        )
                                        if (isOwnMessage) {
                                            Icon(
                                                imageVector = when {
                                                    message.status == MessageStatus.SENDING -> Icons.Default.Schedule
                                                    message.status == MessageStatus.FAILED -> Icons.Default.Error
                                                    isRead || message.status == MessageStatus.SEEN -> Icons.Default.DoneAll
                                                    message.status == MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                                    else -> Icons.Default.Check
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = when {
                                                    message.status == MessageStatus.FAILED -> MaterialTheme.colorScheme.error
                                                    isRead || message.status == MessageStatus.SEEN -> Purple300
                                                    else -> White.copy(alpha = 0.7f)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            else -> {
                                // Text message (default)
                                Column {
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
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Public,
                                                null,
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isOwnMessage) White.copy(alpha = 0.7f) else Purple500
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (showOriginal) "Translate" else "Original",
                                                fontSize = 11.sp,
                                                color = if (isOwnMessage) White.copy(alpha = 0.7f) else Purple500
                                            )
                                        }
                                    }
                                    
                                    // Timestamp inside bubble - bottom right
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = formatTime(message.createdAt),
                                            fontSize = 11.sp,
                                            color = if (isOwnMessage) White.copy(alpha = 0.7f) else Surface500
                                        )
                                        if (isOwnMessage) {
                                            Icon(
                                                imageVector = when {
                                                    message.status == MessageStatus.SENDING -> Icons.Default.Schedule
                                                    message.status == MessageStatus.FAILED -> Icons.Default.Error
                                                    isRead || message.status == MessageStatus.SEEN -> Icons.Default.DoneAll
                                                    message.status == MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                                    else -> Icons.Default.Check
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = when {
                                                    message.status == MessageStatus.FAILED -> MaterialTheme.colorScheme.error
                                                    isRead || message.status == MessageStatus.SEEN -> Purple300
                                                    else -> White.copy(alpha = 0.7f)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Reactions - only top ~10% overlaps, rest hangs below bubble
                if (hasReactions) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 8.dp, y = 20.dp) // Increased offset so only top 10% overlaps
                            .background(Surface800, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        message.reactions?.entries?.forEach { (emoji, users) ->
                            if (users.isNotEmpty()) {
                                val isMyReaction = users.contains(currentUserId)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isMyReaction) Purple500.copy(alpha = 0.3f) else Color.Transparent,
                                    modifier = Modifier.clickable { onReactionClick(emoji) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(emoji, fontSize = 14.sp)
                                        if (users.size > 1) {
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

@Composable
private fun DocumentTranslationDialog(
    fileName: String,
    onTranslate: () -> Unit,
    onSendWithoutTranslation: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Translate,
                contentDescription = null,
                tint = Purple500,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                "Translate Document?",
                color = White,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    "Would you like to translate \"$fileName\" for recipients who speak other languages?",
                    color = Surface300
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Note: Document content will be processed for translation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Surface500
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onTranslate) {
                Text("Translate", color = Purple500)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Surface400)
                }
                TextButton(onClick = onSendWithoutTranslation) {
                    Text("Send Without Translation", color = Surface300)
                }
            }
        },
        containerColor = Surface800,
        titleContentColor = White,
        textContentColor = Surface300
    )
}

@Composable
private fun DeleteMessageDialog(
    message: Message,
    isOwnMessage: Boolean,
    isDeleting: Boolean,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                "Delete this message?",
                color = White,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                if (isDeleting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Deleting...", color = Surface300)
                    }
                } else {
                    if (isOwnMessage) {
                        Text(
                            "You can delete this message for yourself or for everyone in this conversation.",
                            color = Surface300
                        )
                    } else {
                        Text(
                            "This message will be removed from your view only.",
                            color = Surface300
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isDeleting) {
                Column {
                    if (isOwnMessage) {
                        // Delete for Everyone button (only for own messages)
                        TextButton(
                            onClick = onDeleteForEveryone,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Delete for Everyone",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Delete for Me button
                    TextButton(
                        onClick = onDeleteForMe,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Delete for Me",
                            color = if (isOwnMessage) Surface300 else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        dismissButton = {
            if (!isDeleting) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Surface400)
                }
            }
        },
        containerColor = Surface800,
        titleContentColor = White,
        textContentColor = Surface300
    )
}
