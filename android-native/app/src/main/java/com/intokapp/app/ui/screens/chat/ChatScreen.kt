package com.intokapp.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intokapp.app.data.models.Message
import com.intokapp.app.data.models.MessageStatus
import com.intokapp.app.ui.theme.*
import kotlinx.coroutines.launch
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
    
    Scaffold(
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Surface950, Surface900)
                    )
                )
                .padding(padding)
        ) {
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
                        isOwnMessage = message.senderId == uiState.currentUserId || message.id.startsWith("temp-")
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
                    IconButton(onClick = { /* Attachments */ }) {
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
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean
) {
    var showTranslation by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
                    text = message.sender.username.take(1).uppercase(),
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
                    text = message.sender.username,
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
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (showTranslation) 
                            message.translatedContent ?: message.originalContent 
                        else 
                            message.originalContent,
                        color = White
                    )
                    
                    // Translation toggle
                    if (message.translatedContent != null) {
                        TextButton(
                            onClick = { showTranslation = !showTranslation },
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
                                text = if (showTranslation) "Original" else "Translate",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOwnMessage) White.copy(alpha = 0.7f) else Purple500
                            )
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
                            MessageStatus.SENT -> Icons.Default.Check
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
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.reactions?.forEach { (emoji, users) ->
                        if (users.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Surface800
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
                                        color = Surface400
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
