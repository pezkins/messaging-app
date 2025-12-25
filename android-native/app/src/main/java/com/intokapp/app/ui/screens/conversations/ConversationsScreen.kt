package com.intokapp.app.ui.screens.conversations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.intokapp.app.R
import com.intokapp.app.data.models.Conversation
import com.intokapp.app.data.models.MessageType
import com.intokapp.app.data.repository.localizedString
import com.intokapp.app.ui.components.WhatsNewAutoDialog
import com.intokapp.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onConversationClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf("") }
    
    val filteredConversations = remember(uiState.conversations, searchText) {
        if (searchText.isEmpty()) uiState.conversations
        else uiState.conversations.filter { conv ->
            conv.name?.contains(searchText, ignoreCase = true) == true ||
            conv.participants.any { it.username.contains(searchText, ignoreCase = true) }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }
    
    // What's New Dialog
    if (uiState.showWhatsNew && uiState.whatsNewEntries.isNotEmpty()) {
        WhatsNewAutoDialog(
            entries = uiState.whatsNewEntries,
            onDismiss = { viewModel.dismissWhatsNew() }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedString(R.string.conversations_title, "conversations.conversations_title"), color = White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onSettingsClick) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Purple500),
                            contentAlignment = Alignment.Center
                        ) {
                            val userAvatarUrl = uiState.user?.profilePicture ?: uiState.user?.avatarUrl
                            if (userAvatarUrl != null) {
                                AsyncImage(
                                    model = userAvatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = (uiState.user?.username?.take(1) ?: "?").uppercase(),
                                    color = White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNewChatClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = localizedString(R.string.conversations_new_chat, "conversations.conversations_new_chat"),
                            tint = Purple500
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface950)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = Purple500
            ) {
                Icon(Icons.Default.Add, contentDescription = localizedString(R.string.conversations_new_chat, "conversations.conversations_new_chat"), tint = White)
            }
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
            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(localizedString(R.string.conversations_search_hint, "conversations.conversations_search_placeholder"), color = Surface500) },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = Surface500)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = Purple500,
                    unfocusedBorderColor = Surface700,
                    focusedContainerColor = Surface800.copy(alpha = 0.5f),
                    unfocusedContainerColor = Surface800.copy(alpha = 0.5f)
                ),
                singleLine = true
            )
            
            // Content
            when {
                uiState.isLoading && uiState.conversations.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Purple500)
                            Text(
                                "Loading conversations...",
                                color = Surface400,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                
                uiState.conversations.isEmpty() -> {
                    EmptyConversationsView(onNewChatClick = onNewChatClick)
                }
                
                else -> {
                    LazyColumn {
                        items(filteredConversations) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                currentUserId = uiState.user?.id,
                                onClick = { onConversationClick(conversation.id) },
                                onLongClick = { viewModel.showDeleteConversationDialog(conversation) }
                            )
                            
                            Divider(
                                color = Surface700,
                                modifier = Modifier.padding(start = 76.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete conversation confirmation dialog
    if (uiState.showDeleteDialog && uiState.conversationToDelete != null) {
        DeleteConversationDialog(
            conversation = uiState.conversationToDelete!!,
            currentUserId = uiState.user?.id,
            isDeleting = uiState.isDeletingConversation,
            onDelete = { viewModel.deleteConversation() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}

@Composable
private fun EmptyConversationsView(onNewChatClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Message,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Purple500.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No conversations yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = White
        )
        
        Text(
            text = "Start a conversation by tapping the compose button",
            style = MaterialTheme.typography.bodyMedium,
            color = Surface400,
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNewChatClick,
            colors = ButtonDefaults.buttonColors(containerColor = Purple500),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text("Start New Chat")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    currentUserId: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val displayName = remember(conversation) {
        if (conversation.type == "group") {
            conversation.name ?: "Group Chat"
        } else {
            conversation.participants
                .firstOrNull { it.id != currentUserId }
                ?.username ?: "Unknown"
        }
    }
    
    val avatarUrl = remember(conversation) {
        if (conversation.type == "direct") {
            conversation.participants.firstOrNull { it.id != currentUserId }?.displayAvatarUrl
        } else null
    }
    
    val lastMessagePreview = remember(conversation.lastMessage) {
        conversation.lastMessage?.let { message ->
            when (message.type) {
                MessageType.IMAGE -> "📷 Photo"
                MessageType.VOICE -> "🎤 Voice message"
                MessageType.FILE, MessageType.ATTACHMENT -> "📎 File"
                MessageType.GIF -> "GIF"
                MessageType.TEXT, null -> message.translatedContent ?: message.originalContent
            }
        } ?: "No messages yet"
    }
    
    val timeString = remember(conversation.lastMessage?.createdAt) {
        conversation.lastMessage?.createdAt?.let { formatTime(it) } ?: ""
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Purple500),
            contentAlignment = Alignment.Center
        ) {
            if (conversation.type == "group") {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = White
                )
            } else if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    color = White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (conversation.unreadCount > 0) Purple500 else Surface400
                    )
                    
                    // Unread badge
                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(Purple500, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                color = White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Text(
                text = lastMessagePreview,
                style = MaterialTheme.typography.bodyMedium,
                color = if (conversation.unreadCount > 0) White else Surface400,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
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
            } catch (e: Exception) {
                continue
            }
        }
        
        if (date == null) return ""
        
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.time = date
        
        when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("h:mm a", Locale.US).format(date)
            }
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> {
                "Yesterday"
            }
            else -> {
                SimpleDateFormat("MMM d", Locale.US).format(date)
            }
        }
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun DeleteConversationDialog(
    conversation: Conversation,
    currentUserId: String?,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val displayName = remember(conversation) {
        if (conversation.type == "group") {
            conversation.name ?: "Group Chat"
        } else {
            conversation.participants
                .firstOrNull { it.id != currentUserId }
                ?.username ?: "Unknown"
        }
    }
    
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
                "Delete conversation?",
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
                    Text(
                        "Delete your conversation with \"$displayName\"? This action cannot be undone.",
                        color = Surface300
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Note: This only removes the conversation from your view. Other participants will still have access to the conversation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Surface500
                    )
                }
            }
        },
        confirmButton = {
            if (!isDeleting) {
                TextButton(onClick = onDelete) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
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
