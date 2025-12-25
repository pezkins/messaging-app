package com.intokapp.app.ui.screens.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.intokapp.app.data.repository.localizedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.intokapp.app.R
import com.intokapp.app.data.models.Conversation
import com.intokapp.app.data.models.UserPublic
import com.intokapp.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    conversationId: String,
    onBackClick: () -> Unit,
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val conversation = uiState.conversation
    val currentUserId = uiState.currentUserId
    
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    
    // Load conversation on first composition
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }
    
    // Update editedName when conversation loads
    LaunchedEffect(conversation?.name) {
        editedName = conversation?.name ?: ""
    }
    
    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadGroupPicture(conversationId, it) }
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    // Show loading if conversation not yet loaded
    if (conversation == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(localizedString(R.string.group_info_title, "group.group_info_title"), color = White) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, null, tint = White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface950)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Surface950)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Purple500)
            }
        }
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_info_title), color = White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, null, tint = White)
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Group Picture
                item {
                    GroupPictureSection(
                        pictureUrl = conversation.pictureUrl,
                        isUploading = uiState.isUploadingPhoto,
                        onClick = { photoPickerLauncher.launch("image/*") }
                    )
                }
                
                // Group Name
                item {
                    GroupNameSection(
                        name = conversation.name ?: "Group Chat",
                        onClick = { 
                            editedName = conversation.name ?: ""
                            showEditNameDialog = true 
                        }
                    )
                }
                
                // Participant Count
                item {
                    Text(
                        text = "${conversation.participants.size} participants",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Surface400
                    )
                }
                
                // Divider
                item {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Surface700
                    )
                }
                
                // Participants Header
                item {
                    Text(
                        text = "Participants",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Participants List
                items(conversation.participants) { participant ->
                    ParticipantRow(
                        participant = participant,
                        isCurrentUser = participant.id == currentUserId
                    )
                }
            }
            
            // Loading overlay
            if (uiState.isLoading || uiState.isUploadingPhoto) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Surface950.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Purple500)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.isUploadingPhoto) "Uploading photo..." else "Saving...",
                            color = White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    
    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Group Name") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateGroupName(conversationId, editedName)
                        showEditNameDialog = false
                    },
                    enabled = editedName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GroupPictureSection(
    pictureUrl: String?,
    isUploading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(top = 16.dp)
            .clickable(enabled = !isUploading, onClick = onClick),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Group picture or placeholder
        if (pictureUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(pictureUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Group picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Purple500),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        // Camera icon overlay
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Surface800),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change picture",
                    tint = White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun GroupNameSection(
    name: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit name",
            tint = Purple500,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ParticipantRow(
    participant: UserPublic,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        val avatarUrl = participant.profilePicture ?: participant.avatarUrl
        if (avatarUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Purple500),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.username.take(1).uppercase(),
                    color = White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Name and language
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = participant.username,
                    color = White,
                    fontWeight = FontWeight.Medium
                )
                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(You)",
                        color = Surface400,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = Surface400,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = getLanguageName(participant.preferredLanguage),
                    color = Surface400,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun getLanguageName(code: String): String {
    return java.util.Locale(code).displayLanguage
}
