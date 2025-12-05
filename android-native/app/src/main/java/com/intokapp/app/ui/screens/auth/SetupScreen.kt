package com.intokapp.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intokapp.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("en") }
    var selectedCountry by remember { mutableStateOf("US") }
    var isLoading by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface950)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Welcome to Intok! 👋",
            style = MaterialTheme.typography.headlineMedium,
            color = White
        )
        
        Text(
            text = "Let's set up your profile",
            style = MaterialTheme.typography.bodyLarge,
            color = Surface400,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Display Name
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple500,
                unfocusedBorderColor = Surface600,
                focusedLabelColor = Purple500,
                unfocusedLabelColor = Surface400,
                cursorColor = Purple500,
                focusedTextColor = White,
                unfocusedTextColor = White
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Language Dropdown (placeholder)
        Text(
            text = "Preferred Language",
            style = MaterialTheme.typography.labelLarge,
            color = Surface400,
            modifier = Modifier.align(Alignment.Start)
        )
        
        // TODO: Add proper language picker
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Surface800
        ) {
            Text(
                text = "🇺🇸 English",
                modifier = Modifier.padding(16.dp),
                color = White
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Country Dropdown (placeholder)
        Text(
            text = "Country",
            style = MaterialTheme.typography.labelLarge,
            color = Surface400,
            modifier = Modifier.align(Alignment.Start)
        )
        
        // TODO: Add proper country picker
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Surface800
        ) {
            Text(
                text = "🇺🇸 United States",
                modifier = Modifier.padding(16.dp),
                color = White
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Continue Button
        Button(
            onClick = {
                isLoading = true
                // TODO: Save profile settings
                onSetupComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple500
            ),
            enabled = displayName.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = White
                )
            } else {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

