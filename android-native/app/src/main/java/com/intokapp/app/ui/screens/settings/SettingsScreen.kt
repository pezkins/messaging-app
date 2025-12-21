package com.intokapp.app.ui.screens.settings

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.intokapp.app.data.constants.*
import com.intokapp.app.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showEditName by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf("") }
    
    // Camera capture URI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.uploadProfilePicture(it) }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.uploadProfilePicture(cameraImageUri!!)
        }
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Create temp file and launch camera
            val photoFile = File.createTempFile("profile_", ".jpg", context.cacheDir)
            cameraImageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            cameraImageUri?.let { cameraLauncher.launch(it) }
        }
    }
    
    // Show error/success messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }
    
    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onLogout()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                    profilePicture = uiState.user?.profilePicture ?: uiState.user?.avatarUrl,
                    isUploading = uiState.isUploadingPhoto,
                    uploadProgress = uiState.uploadProgress,
                    onAvatarClick = { showImagePickerDialog = true },
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
                    
                    // Region (only show if country has regions)
                    uiState.user?.preferredCountry?.let { countryCode ->
                        if (hasRegions(countryCode)) {
                            Divider(color = Surface700)
                            
                            SettingsRow(
                                icon = Icons.Default.LocationOn,
                                iconColor = Accent500,
                                title = "Region",
                                value = uiState.user?.preferredRegion?.let { regionCode ->
                                    getRegionByCode(countryCode, regionCode)?.name
                                } ?: "Not set",
                                onClick = { showRegionPicker = true }
                            )
                        }
                    }
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
                            text = com.intokapp.app.BuildConfig.VERSION_NAME,
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
            
            // Loading overlay during upload
            if (uiState.isUploadingPhoto) {
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
                            text = "Uploading photo...",
                            color = White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    
    // Dialogs & Sheets
    if (showImagePickerDialog) {
        ImagePickerDialog(
            hasExistingPhoto = (uiState.user?.profilePicture ?: uiState.user?.avatarUrl) != null,
            onTakePhoto = {
                showImagePickerDialog = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onChooseFromGallery = {
                showImagePickerDialog = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemovePhoto = {
                showImagePickerDialog = false
                viewModel.deleteProfilePicture()
            },
            onDismiss = { showImagePickerDialog = false }
        )
    }
    
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
    
    if (showRegionPicker) {
        uiState.user?.preferredCountry?.let { countryCode ->
            RegionPickerDialog(
                countryCode = countryCode,
                onDismiss = { showRegionPicker = false },
                onSelect = { region ->
                    viewModel.updateRegion(region.code)
                    showRegionPicker = false
                }
            )
        }
    }
    
    if (showWhatsNew) {
        WhatsNewDialog(
            changelog = viewModel.whatsNewManager.changelog,
            onDismiss = { showWhatsNew = false }
        )
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
    profilePicture: String?,
    isUploading: Boolean,
    uploadProgress: Float,
    onAvatarClick: () -> Unit,
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
            // Avatar with camera overlay
            Box(
                modifier = Modifier.clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Purple500),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicture != null) {
                        AsyncImage(
                            model = profilePicture,
                            contentDescription = "Profile picture",
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
                    
                    // Loading overlay
                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Surface950.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = uploadProgress,
                                color = Purple500,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
                
                // Camera icon overlay - bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Surface700),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = White,
                        modifier = Modifier.size(16.dp)
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
private fun ImagePickerDialog(
    hasExistingPhoto: Boolean,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Profile Photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Take Photo
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTakePhoto),
                    shape = RoundedCornerShape(8.dp),
                    color = Surface700
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Purple500
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Take Photo", color = White)
                    }
                }
                
                // Choose from Gallery
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onChooseFromGallery),
                    shape = RoundedCornerShape(8.dp),
                    color = Surface700
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Purple500
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Choose from Gallery", color = White)
                    }
                }
                
                // Remove Photo (only show if there's an existing photo)
                if (hasExistingPhoto) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRemovePhoto),
                        shape = RoundedCornerShape(8.dp),
                        color = Red500.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Red500
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Remove Photo", color = Red500)
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
        },
        containerColor = Surface800
    )
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
private fun RegionPickerDialog(
    countryCode: String,
    onDismiss: () -> Unit,
    onSelect: (Region) -> Unit
) {
    val regions = remember(countryCode) { getRegionsForCountry(countryCode) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Region") },
        text = {
            if (regions.isEmpty()) {
                Text("No regions available for this country")
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp)
                ) {
                    items(regions) { region ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(region) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(region.name)
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
private fun WhatsNewDialog(
    changelog: List<com.intokapp.app.data.repository.ChangelogEntry>,
    onDismiss: () -> Unit
) {
    // Show first 5 entries
    val entries = changelog.take(5)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (entries.isNotEmpty()) {
                Column {
                    Text("Version ${entries.first().version}", fontWeight = FontWeight.Bold)
                    Text(
                        entries.first().title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text("What's New", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 400.dp)
            ) {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "v${entry.version} - ${entry.title}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Purple500
                        )
                    }
                    
                    entry.changes.forEach { change ->
                        Text(
                            text = change,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Purple500)
            }
        }
    )
}


// Add these color values to your Theme
val Green500 = androidx.compose.ui.graphics.Color(0xFF22C55E)
val Yellow500 = androidx.compose.ui.graphics.Color(0xFFEAB308)
val Blue500 = androidx.compose.ui.graphics.Color(0xFF3B82F6)
val Red500 = androidx.compose.ui.graphics.Color(0xFFEF4444)
