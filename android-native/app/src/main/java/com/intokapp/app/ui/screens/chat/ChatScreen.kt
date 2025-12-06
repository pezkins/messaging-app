package com.intokapp.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intokapp.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBackClick: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // TODO: Connect to ViewModel for real messages
    val messages = remember { listOf<ChatMessage>() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = Purple600
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("U", color = White)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "User Name",
                                style = MaterialTheme.typography.titleMedium,
                                color = White
                            )
                            Text(
                                text = "Online",
                                style = MaterialTheme.typography.bodySmall,
                                color = Surface400
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface900
                )
            )
        },
        containerColor = Surface950
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("💬", style = MaterialTheme.typography.displayMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No messages yet",
                                    color = Surface400
                                )
                                Text(
                                    "Send a message to start the conversation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Surface500
                                )
                            }
                        }
                    }
                }
            }
            
            // Input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Surface900
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment button
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = Surface400
                        )
                    }
                    
                    // Text input
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Type a message...", color = Surface500)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Surface800,
                            unfocusedContainerColor = Surface800,
                            focusedIndicatorColor = Purple500,
                            unfocusedIndicatorColor = Surface700,
                            cursorColor = Purple500,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = false,
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Send button
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                // TODO: Send message
                                messageText = ""
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (messageText.isNotBlank()) Purple500 else Surface700,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = White
                        )
                    }
                }
            }
            
            // Translation hint
            Text(
                text = "Messages translate automatically to each person's language",
                style = MaterialTheme.typography.bodySmall,
                color = Surface500,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface900)
                    .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}

data class ChatMessage(
    val id: String,
    val content: String,
    val isOwn: Boolean,
    val time: String,
    val status: String = "sent"
)

