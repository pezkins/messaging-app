package com.intokapp.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.intokapp.app.data.models.UserPublic
import com.intokapp.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveParticipantsScreen(
    conversationId: String,
    onBackClick: () -> Unit,
    onParticipantRemoved: () -> Unit,
    viewModel: RemoveParticipantsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Confirmation dialog state
    var showConfirmDialog by remember { mutableStateOf(false) }
    var userToRemove by remember { mutableStateOf<UserPublic?>(null) }
    
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Show success message
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }
    
    // Confirmation dialog
    if (showConfirmDialog && userToRemove != null) {
        AlertDialog(
            onDismissRequest = { 
                showConfirmDialog = false
                userToRemove = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "Remove ${userToRemove!!.username}?",
                    color = White,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "Are you sure you want to remove this person from the group?",
                    color = Surface300
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeParticipant(userToRemove!!.id)
                        showConfirmDialog = false
                        userToRemove = null
                    },
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showConfirmDialog = false
                    userToRemove = null
                }) {
                    Text("Cancel", color = Surface400)
                }
            },
            containerColor = Surface800,
            titleContentColor = White,
            textContentColor = Surface300
        )
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Remove People", color = White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, null, tint = White)
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
            // Info text
            Text(
                text = "Tap a person to remove them from the group",
                style = MaterialTheme.typography.bodyMedium,
                color = Surface400,
                modifier = Modifier.padding(16.dp)
            )
            
            if (uiState.isLoadingParticipants) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Purple500)
                }
            } else if (uiState.participants.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Surface500
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No other participants in this group",
                            color = Surface400
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.participants) { user ->
                        ParticipantRemoveRow(
                            user = user,
                            onClick = {
                                userToRemove = user
                                showConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantRemoveRow(
    user: UserPublic,
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
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Purple500),
            contentAlignment = Alignment.Center
        ) {
            val avatarUrl = user.displayAvatarUrl
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = user.username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            user.preferredLanguage?.let { lang ->
                Text(
                    text = lang,
                    style = MaterialTheme.typography.bodySmall,
                    color = Surface400
                )
            }
        }
        
        // Remove icon
        Icon(
            Icons.Default.Close,
            contentDescription = "Remove",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )
    }
}
