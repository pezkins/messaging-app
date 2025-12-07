package com.intokapp.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.intokapp.app.data.constants.*
import com.intokapp.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showEditName by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onLogout()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = White) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Section
            ProfileSection(
                username = uiState.user?.username ?: "Unknown",
                email = uiState.user?.email ?: "",
                avatarUrl = uiState.user?.avatarUrl,
                onEditClick = {
                    editingName = uiState.user?.username ?: ""
                    showEditName = true
                }
            )
            
            // Preferences Section
            SettingsSection(title = "PREFERENCES") {
                SettingsRow(
                    icon = Icons.Default.Public,
                    iconColor = Purple500,
                    title = "Language",
                    value = getLanguageByCode(uiState.user?.preferredLanguage ?: "en")?.name ?: "English",
                    onClick = { showLanguagePicker = true }
                )
                
                Divider(color = Surface700)
                
                SettingsRow(
                    icon = Icons.Default.Map,
                    iconColor = Green500,
                    title = "Country",
                    value = uiState.user?.preferredCountry?.let { getCountryByCode(it)?.name } ?: "Not set",
                    onClick = { showCountryPicker = true }
                )
            }
            
            // About Section
            SettingsSection(title = "ABOUT") {
                SettingsRow(
                    icon = Icons.Default.AutoAwesome,
                    iconColor = Yellow500,
                    title = "What's New",
                    onClick = { showWhatsNew = true }
                )
                
                Divider(color = Surface700)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Blue500,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Text(
                        text = "Version",
                        color = White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )
                    
                    Text(
                        text = "0.1.1",
                        color = Surface400
                    )
                }
            }
            
            // Sign Out Button
            Button(
                onClick = { showSignOutConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Red500.copy(alpha = 0.1f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Red500
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", color = Red500, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    
    // Dialogs & Sheets
    if (showLanguagePicker) {
        LanguagePickerDialog(
            onDismiss = { showLanguagePicker = false },
            onSelect = { language ->
                viewModel.updateLanguage(language.code)
                showLanguagePicker = false
            }
        )
    }
    
    if (showCountryPicker) {
        CountryPickerDialog(
            onDismiss = { showCountryPicker = false },
            onSelect = { country ->
                viewModel.updateCountry(country.code)
                showCountryPicker = false
            }
        )
    }
    
    if (showWhatsNew) {
        WhatsNewDialog(onDismiss = { showWhatsNew = false })
    }
    
    if (showEditName) {
        AlertDialog(
            onDismissRequest = { showEditName = false },
            title = { Text("Edit Display Name") },
            text = {
                OutlinedTextField(
                    value = editingName,
                    onValueChange = { editingName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateUsername(editingName)
                        showEditName = false
                    },
                    enabled = editingName.length >= 2
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditName = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.signOut()
                        showSignOutConfirm = false
                    }
                ) {
                    Text("Sign Out", color = Red500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileSection(
    username: String,
    email: String,
    avatarUrl: String?,
    onEditClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Surface800.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Purple500),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = username.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Name with edit
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onEditClick)
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Purple500,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(16.dp)
                )
            }
            
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = Surface400,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Surface400,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Surface800.copy(alpha = 0.5f)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = title,
            color = White,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
        
        value?.let {
            Text(
                text = it,
                color = Surface400,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Surface400
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (Language) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchText) {
        if (searchText.isEmpty()) LANGUAGES
        else LANGUAGES.filter {
            it.name.contains(searchText, ignoreCase = true) ||
            it.native.contains(searchText, ignoreCase = true)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.height(300.dp)
                ) {
                    items(filteredLanguages) { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(language) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(language.name)
                                Text(
                                    text = language.native,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CountryPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (Country) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val filteredCountries = remember(searchText) {
        if (searchText.isEmpty()) COUNTRIES
        else COUNTRIES.filter { it.name.contains(searchText, ignoreCase = true) }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Country") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.height(300.dp)
                ) {
                    items(filteredCountries) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(country) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = country.flag,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(country.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text("Version 0.1.1", fontWeight = FontWeight.Bold)
                Text(
                    "Initial Release",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FeatureRow(icon = Icons.Default.Message, title = "Real-time Messaging", desc = "Send and receive messages instantly")
                FeatureRow(icon = Icons.Default.Public, title = "Auto Translation", desc = "Messages translated to your language")
                FeatureRow(icon = Icons.Default.Group, title = "Group Chats", desc = "Create group conversations")
                FeatureRow(icon = Icons.Default.EmojiEmotions, title = "Reactions", desc = "React to messages with emojis")
                FeatureRow(icon = Icons.Default.Photo, title = "Media Sharing", desc = "Share images and files")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Purple500,
            modifier = Modifier.size(24.dp)
        )
        
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Add these color values to your Theme
val Green500 = androidx.compose.ui.graphics.Color(0xFF22C55E)
val Yellow500 = androidx.compose.ui.graphics.Color(0xFFEAB308)
val Blue500 = androidx.compose.ui.graphics.Color(0xFF3B82F6)
val Red500 = androidx.compose.ui.graphics.Color(0xFFEF4444)
