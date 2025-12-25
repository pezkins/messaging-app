package com.intokapp.app.ui.screens.newchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.intokapp.app.R
import com.intokapp.app.data.constants.getLanguageByCode
import com.intokapp.app.data.models.UserPublic
import com.intokapp.app.data.repository.localizedString
import com.intokapp.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    onBackClick: () -> Unit,
    onChatCreated: (String) -> Unit,
    viewModel: NewChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var isGroupMode by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    
    LaunchedEffect(searchText) {
        viewModel.searchUsers(searchText)
    }
    
    LaunchedEffect(uiState.createdConversationId) {
        uiState.createdConversationId?.let { id ->
            onChatCreated(id)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isGroupMode) localizedString(R.string.new_chat_new_group, "new_chat.new_group_title") else localizedString(R.string.new_chat_title, "new_chat.new_chat_title"), color = White) },
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
            // Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isGroupMode,
                    onClick = { isGroupMode = false },
                    label = { Text(localizedString(R.string.new_chat_direct, "new_chat.new_chat_start_conversation")) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Purple500,
                        selectedLabelColor = White
                    )
                )
                
                FilterChip(
                    selected = isGroupMode,
                    onClick = { isGroupMode = true },
                    label = { Text(localizedString(R.string.new_chat_group, "new_chat.new_group_title")) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Purple500,
                        selectedLabelColor = White
                    )
                )
            }
            
            // Group Name
            if (isGroupMode) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text(localizedString(R.string.new_chat_group_name_hint, "new_chat.new_group_name_placeholder"), color = Surface500) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = Purple500,
                        unfocusedBorderColor = Surface700
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(localizedString(R.string.new_chat_search_hint, "new_chat.new_chat_search_placeholder"), color = Surface500) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Surface500) },
                trailingIcon = {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Purple500,
                            strokeWidth = 2.dp
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
            
            // Selected Users (Group Mode)
            if (isGroupMode && uiState.selectedUsers.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedUsers) { user ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggleUserSelection(user) },
                            label = { Text(user.username, color = White) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = White
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = Purple500.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Search Results
            when {
                uiState.searchResults.isEmpty() && searchText.isNotEmpty() && !uiState.isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PersonOff,
                                null,
                                modifier = Modifier.size(50.dp),
                                tint = Surface500
                            )
                            Text(
                                localizedString(R.string.new_chat_no_results, "new_chat.new_chat_no_results"),
                                color = Surface500,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.searchResults.filter { it.id != uiState.currentUserId }) { user ->
                            UserRow(
                                user = user,
                                isGroupMode = isGroupMode,
                                isSelected = uiState.selectedUsers.any { it.id == user.id },
                                onClick = {
                                    if (isGroupMode) {
                                        viewModel.toggleUserSelection(user)
                                    } else {
                                        viewModel.startDirectChat(user)
                                    }
                                }
                            )
                            
                        Divider(
                            color = Surface700,
                            modifier = Modifier.padding(start = 76.dp)
                        )
                        }
                    }
                }
            }
            
            // Create Group Button
            if (isGroupMode && uiState.selectedUsers.size >= 2) {
                Button(
                    onClick = { viewModel.createGroup(groupName.ifBlank { null }) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple500)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = White
                        )
                    } else {
                        Text(
                            localizedString(R.string.new_chat_create_group, "new_chat.new_group_create_button"),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: UserPublic,
    isGroupMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
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
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = user.avatarUrl,
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
        
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = White
            )
            
            Text(
                text = getLanguageByCode(user.preferredLanguage)?.name ?: user.preferredLanguage,
                style = MaterialTheme.typography.bodySmall,
                color = Surface400
            )
        }
        
        // Selection indicator
        if (isGroupMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = Purple500
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Surface500
            )
        }
    }
}


