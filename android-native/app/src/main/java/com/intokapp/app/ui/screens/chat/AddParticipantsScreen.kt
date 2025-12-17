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
fun AddParticipantsScreen(
    conversationId: String,
    onBackClick: () -> Unit,
    onParticipantsAdded: () -> Unit,
    viewModel: AddParticipantsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    
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
    
    // Navigate back on success
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onParticipantsAdded()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add People", color = White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, null, tint = White)
                    }
                },
                actions = {
                    // Add button (enabled when at least one user is selected)
                    if (uiState.selectedUsers.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.addSelectedParticipants() },
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Purple500
                                )
                            } else {
                                Text(
                                    "Add (${uiState.selectedUsers.size})",
                                    color = Purple500,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchUsers(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search users...", color = Surface500) },
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
            
            // Selected users chips
            if (uiState.selectedUsers.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedUsers.chunked(3)) { rowUsers ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowUsers.forEach { user ->
                                AssistChip(
                                    onClick = { viewModel.toggleUserSelection(user) },
                                    label = { Text(user.username, color = White) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp),
                                            tint = Surface400
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Purple500.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Surface700)
            }
            
            // Search results
            if (uiState.isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Purple500)
                }
            } else if (uiState.searchResults.isEmpty() && searchQuery.length >= 2) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No users found", color = Surface400)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.searchResults) { user ->
                        UserSelectRow(
                            user = user,
                            isSelected = uiState.selectedUsers.any { it.id == user.id },
                            isExistingParticipant = uiState.existingParticipantIds.contains(user.id),
                            onClick = { viewModel.toggleUserSelection(user) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserSelectRow(
    user: UserPublic,
    isSelected: Boolean,
    isExistingParticipant: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isExistingParticipant, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isExistingParticipant) Surface700 else Purple500),
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
                    color = if (isExistingParticipant) Surface500 else White
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
                color = if (isExistingParticipant) Surface500 else White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isExistingParticipant) {
                Text(
                    text = "Already in group",
                    style = MaterialTheme.typography.bodySmall,
                    color = Surface500
                )
            }
        }
        
        // Selection checkbox
        if (!isExistingParticipant) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Purple500,
                    uncheckedColor = Surface500
                )
            )
        }
    }
}
