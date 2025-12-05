package com.intokapp.app.ui.screens.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intokapp.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onConversationClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // TODO: Connect to ViewModel
    val conversations = remember { listOf<ConversationItem>() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Messages",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface950,
                    titleContentColor = White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = Purple500,
                contentColor = White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        },
        containerColor = Surface950
    ) { padding ->
        if (conversations.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💬",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = Surface400
                    )
                    Text(
                        text = "Start chatting with someone!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Surface500,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) }
                    )
                }
            }
        }
    }
}

data class ConversationItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0
)

@Composable
private fun ConversationItem(
    conversation: ConversationItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Purple600
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = conversation.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = White
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = conversation.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = conversation.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = Surface500
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Surface400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (conversation.unreadCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Purple500,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

