package com.intokapp.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intokapp.app.data.constants.COUNTRIES
import com.intokapp.app.data.constants.Country
import com.intokapp.app.data.constants.LANGUAGES
import com.intokapp.app.data.constants.Language
import com.intokapp.app.data.constants.Region
import com.intokapp.app.data.constants.getRegionsForCountry
import com.intokapp.app.data.constants.hasRegions
import com.intokapp.app.ui.theme.*

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onSetupComplete()
        }
    }
    
    // Calculate total steps (4 if country has regions, 3 otherwise)
    val totalSteps = if (viewModel.countryHasRegions()) 4 else 3
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Surface950, Surface900)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Indicator (dynamic based on whether country has regions)
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= currentStep) Purple500 else Surface700
                            )
                    )
                }
            }
            
            // Content
            when (currentStep) {
                0 -> DisplayNameStep(
                    displayName = uiState.displayName,
                    onDisplayNameChange = viewModel::setDisplayName,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onContinue = {
                        viewModel.saveDisplayName {
                            currentStep = 1
                        }
                    }
                )
                1 -> LanguageStep(
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelect = viewModel::setLanguage,
                    isLoading = uiState.isLoading,
                    onContinue = {
                        viewModel.saveLanguage {
                            currentStep = 2
                        }
                    }
                )
                2 -> CountryStep(
                    selectedCountry = uiState.selectedCountry,
                    onCountrySelect = viewModel::setCountry,
                    isLoading = uiState.isLoading,
                    countryHasRegions = viewModel.countryHasRegions(),
                    onSkip = viewModel::completeSetup,
                    onContinueToRegion = {
                        viewModel.saveCountry {
                            currentStep = 3
                        }
                    },
                    onFinish = viewModel::saveCountryAndComplete
                )
                3 -> RegionStep(
                    selectedCountry = uiState.selectedCountry,
                    selectedRegion = uiState.selectedRegion,
                    onRegionSelect = viewModel::setRegion,
                    isLoading = uiState.isLoading,
                    onSkip = viewModel::completeSetup,
                    onFinish = viewModel::saveRegionAndComplete
                )
            }
        }
    }
}

@Composable
private fun DisplayNameStep(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Purple500
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Choose a Display Name",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = White
        )
        
        Text(
            text = "This is how others will see you",
            style = MaterialTheme.typography.bodyMedium,
            color = Surface400,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            placeholder = { Text("Enter your name", color = Surface500) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = Purple500,
                unfocusedBorderColor = Surface700
            ),
            singleLine = true
        )
        
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = displayName.length >= 2 && !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple500,
                disabledContainerColor = Surface700
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = White
                )
            } else {
                Text("Continue")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun LanguageStep(
    selectedLanguage: Language?,
    onLanguageSelect: (Language) -> Unit,
    isLoading: Boolean,
    onContinue: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    
    val filteredLanguages = remember(searchText) {
        if (searchText.isEmpty()) LANGUAGES
        else LANGUAGES.filter {
            it.name.contains(searchText, ignoreCase = true) ||
            it.native.contains(searchText, ignoreCase = true)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Public,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Purple500
        )
        
        Text(
            text = "Select Your Language",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = White,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Text(
            text = "Messages will be translated to this language",
            style = MaterialTheme.typography.bodyMedium,
            color = Surface400,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search languages...", color = Surface500) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = Purple500,
                unfocusedBorderColor = Surface700
            ),
            singleLine = true
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredLanguages) { language ->
                LanguageItem(
                    language = language,
                    isSelected = selectedLanguage?.code == language.code,
                    onClick = { onLanguageSelect(language) }
                )
            }
        }
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedLanguage != null && !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple500,
                disabledContainerColor = Surface700
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = White
                )
            } else {
                Text("Continue")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Purple500.copy(alpha = 0.2f) else Surface800.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.name,
                    color = White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = language.native,
                    color = Surface400,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Purple500
                )
            }
        }
    }
}

@Composable
private fun CountryStep(
    selectedCountry: Country?,
    onCountrySelect: (Country) -> Unit,
    isLoading: Boolean,
    countryHasRegions: Boolean,
    onSkip: () -> Unit,
    onContinueToRegion: () -> Unit,
    onFinish: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    
    val filteredCountries = remember(searchText) {
        if (searchText.isEmpty()) COUNTRIES
        else COUNTRIES.filter { it.name.contains(searchText, ignoreCase = true) }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Purple500
        )
        
        Text(
            text = "Select Your Country",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = White,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Text(
            text = "Optional - helps connect you with nearby users",
            style = MaterialTheme.typography.bodyMedium,
            color = Surface400,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Search countries...", color = Surface500) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = Purple500,
                unfocusedBorderColor = Surface700
            ),
            singleLine = true
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCountries) { country ->
                CountryItem(
                    country = country,
                    isSelected = selectedCountry?.code == country.code,
                    onClick = { onCountrySelect(country) }
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = White)
            ) {
                Text("Skip")
            }
            
            Button(
                onClick = if (countryHasRegions) onContinueToRegion else onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple500)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = White
                    )
                } else {
                    if (countryHasRegions) {
                        Text("Continue")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    } else {
                        Text("Finish")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionStep(
    selectedCountry: Country?,
    selectedRegion: Region?,
    onRegionSelect: (Region) -> Unit,
    isLoading: Boolean,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    val regions = remember(selectedCountry) {
        selectedCountry?.let { getRegionsForCountry(it.code) } ?: emptyList()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Purple500
        )
        
        Text(
            text = "Select Your Region",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = White,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Text(
            text = "This helps with translation accuracy",
            style = MaterialTheme.typography.bodyMedium,
            color = Surface400,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(regions) { region ->
                RegionItem(
                    region = region,
                    isSelected = selectedRegion?.code == region.code,
                    onClick = { onRegionSelect(region) }
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = White)
            ) {
                Text("Skip")
            }
            
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple500)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = White
                    )
                } else {
                    Text("Finish")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun RegionItem(
    region: Region,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Purple500.copy(alpha = 0.2f) else Surface800.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = region.name,
                color = White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Purple500
                )
            }
        }
    }
}

@Composable
private fun CountryItem(
    country: Country,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Purple500.copy(alpha = 0.2f) else Surface800.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = country.flag,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Text(
                text = country.name,
                color = White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Purple500
                )
            }
        }
    }
}
