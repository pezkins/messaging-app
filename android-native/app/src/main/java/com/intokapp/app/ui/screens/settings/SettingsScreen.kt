package com.intokapp.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intokapp.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                    containerColor = Surface950,
                    titleContentColor = White
                )
            )
        },
        containerColor = Surface950
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Profile Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Surface900
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Purple600
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "U",
                                style = MaterialTheme.typography.headlineMedium,
                                color = White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "User Name",
                            style = MaterialTheme.typography.titleLarge,
                            color = White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "🇺🇸 English",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Surface400
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Settings Options
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.labelLarge,
                color = Surface400,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Surface900
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Edit Profile",
                        onClick = { /* TODO */ }
                    )
                    Divider(color = Surface800)
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "English",
                        onClick = { /* TODO */ }
                    )
                    Divider(color = Surface800)
                    SettingsItem(
                        icon = Icons.Default.LocationOn,
                        title = "Country",
                        subtitle = "United States",
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "About",
                style = MaterialTheme.typography.labelLarge,
                color = Surface400,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Surface900
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "What's New",
                        onClick = { /* TODO */ }
                    )
                    Divider(color = Surface800)
                    SettingsItem(
                        icon = Icons.Default.Shield,
                        title = "Privacy Policy",
                        onClick = { /* TODO */ }
                    )
                    Divider(color = Surface800)
                    SettingsItem(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Version
            Text(
                text = "Version 0.1.0",
                style = MaterialTheme.typography.bodySmall,
                color = Surface500,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Error
                )
            ) {
                Icon(
                    Icons.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Sign Out", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Surface900,
            titleContentColor = White,
            textContentColor = Surface300
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
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
            tint = Purple400,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Surface400
                )
            }
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Surface500
        )
    }
}

