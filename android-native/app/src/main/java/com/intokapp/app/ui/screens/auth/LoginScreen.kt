package com.intokapp.app.ui.screens.auth

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.intokapp.app.data.repository.localizedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intokapp.app.R
import com.intokapp.app.data.constants.COUNTRIES
import com.intokapp.app.data.constants.LANGUAGES
import com.intokapp.app.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: (isNewUser: Boolean) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginScreen", "📥 Google Sign-In result received: resultCode=${result.resultCode}, hasData=${result.data != null}")
        // Always try to handle the result - error info is often in the intent data
        // even when resultCode is not RESULT_OK
        viewModel.handleGoogleSignInResult(result.data, result.resultCode)
    }
    
    // Track error for dialog
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Show error dialog when error occurs
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            errorMessage = it
            showErrorDialog = true
        }
    }
    
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess(uiState.isNewUser)
        }
    }
    
    // Error Dialog
    if (showErrorDialog && errorMessage.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text(localizedString(R.string.login_error_title, "errors.error_generic"), color = White) },
            text = { Text(errorMessage, color = Surface300) },
            containerColor = Surface900,
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text(localizedString(R.string.ok, "common.ok"), color = Purple500)
                }
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Surface950, Surface900)
                )
            )
    ) {
        when (uiState.emailAuthStep) {
            EmailAuthStep.INITIAL -> InitialLoginContent(
                uiState = uiState,
                onGoogleSignIn = {
                    val intent = viewModel.getGoogleSignInIntent()
                    googleSignInLauncher.launch(intent)
                },
                onEmailSignIn = { viewModel.startEmailAuth() }
            )
            EmailAuthStep.EMAIL_INPUT -> EmailInputContent(
                uiState = uiState,
                onBack = { viewModel.goBackToInitial() },
                onContinue = { email -> viewModel.checkEmail(email) }
            )
            EmailAuthStep.PASSWORD_INPUT -> PasswordInputContent(
                uiState = uiState,
                onBack = { viewModel.goBackToInitial() },
                onLogin = { password -> viewModel.loginWithEmail(password) }
            )
            EmailAuthStep.REGISTRATION -> RegistrationContent(
                uiState = uiState,
                onBack = { viewModel.goBackToInitial() },
                onRegister = { password, username, language, country ->
                    viewModel.registerWithEmail(password, username, language, country)
                }
            )
        }
    }
}

@Composable
private fun InitialLoginContent(
    uiState: LoginUiState,
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Intok Logo",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = localizedString(R.string.app_name, "common.app_name"),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = White
        )
        
        Text(
            text = localizedString(R.string.login_subtitle, "auth.login_subtitle"),
            style = MaterialTheme.typography.bodyLarge,
            color = Surface400,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Google Sign In Button
        Button(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Surface900),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Purple500)
            } else {
                Text(text = localizedString(R.string.login_with_google, "auth.login_google_button"), style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Divider(modifier = Modifier.weight(1f), color = Surface700)
            Text(text = localizedString(R.string.login_or, "auth.login_or_continue_with"), color = Surface500, modifier = Modifier.padding(horizontal = 16.dp))
            Divider(modifier = Modifier.weight(1f), color = Surface700)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Email Sign In Button
        OutlinedButton(
            onClick = onEmailSignIn,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = White)
        ) {
            Text(text = localizedString(R.string.login_with_email, "auth.login_create_account"), style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = localizedString(R.string.login_terms, "auth.register_terms_prefix"),
            style = MaterialTheme.typography.bodySmall,
            color = Surface500,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmailInputContent(
    uiState: LoginUiState,
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        // Back button
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = localizedString(R.string.login_enter_email, "auth.login_email_placeholder"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = White
        )
        
        Text(
            text = localizedString(R.string.login_check_account, "auth.login_no_account"),
            style = MaterialTheme.typography.bodyLarge,
            color = Surface400,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(localizedString(R.string.login_email_label, "auth.login_email_placeholder")) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (email.isNotBlank() && email.contains("@")) {
                        onContinue(email)
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = Purple500,
                unfocusedBorderColor = Surface600,
                focusedLabelColor = Purple500,
                unfocusedLabelColor = Surface400,
                cursorColor = Purple500
            )
        )
        
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { onContinue(email) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple500),
            enabled = email.isNotBlank() && email.contains("@") && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = White)
            } else {
                Text(text = localizedString(R.string.setup_continue, "setup.setup_continue_button"), style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PasswordInputContent(
    uiState: LoginUiState,
    onBack: () -> Unit,
    onLogin: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = localizedString(R.string.login_welcome_back, "auth.login_title"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = White
        )
        
        Text(
            text = localizedString(R.string.login_enter_password_for, "auth.login_password_placeholder"),
            style = MaterialTheme.typography.bodyLarge,
            color = Surface400,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(localizedString(R.string.login_password_label, "auth.login_password_placeholder")) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (password.isNotBlank()) {
                        onLogin(password)
                    }
                }
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Surface400
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = Purple500,
                unfocusedBorderColor = Surface600,
                focusedLabelColor = Purple500,
                unfocusedLabelColor = Surface400,
                cursorColor = Purple500
            )
        )
        
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { onLogin(password) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple500),
            enabled = password.isNotBlank() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = White)
            } else {
                Text(text = localizedString(R.string.login_button, "auth.login_button"), style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RegistrationContent(
    uiState: LoginUiState,
    onBack: () -> Unit,
    onRegister: (password: String, username: String, language: String, country: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("en") }
    var selectedCountry by remember { mutableStateOf("US") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }
    
    val passwordsMatch = password == confirmPassword && password.isNotBlank()
    val isFormValid = password.length >= 6 && passwordsMatch && username.length >= 3
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = localizedString(R.string.register_title, "auth.register_title"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = White
        )
        
        Text(
            text = localizedString(R.string.register_setup_profile, "auth.register_subtitle"),
            style = MaterialTheme.typography.bodyLarge,
            color = Surface400,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Display Name
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(localizedString(R.string.register_username_label, "auth.register_username_placeholder")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text(localizedString(R.string.register_min_chars, "auth.auth_error_weak_password"), color = Surface500) },
                    colors = outlinedTextFieldColors()
                )
            }
            
            item {
                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(localizedString(R.string.login_password_label, "auth.login_password_placeholder")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    supportingText = { Text(localizedString(R.string.register_min_chars, "auth.auth_error_weak_password"), color = Surface500) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Surface400
                            )
                        }
                    },
                    colors = outlinedTextFieldColors()
                )
            }
            
            item {
                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(localizedString(R.string.register_confirm_password_label, "auth.register_confirm_password_placeholder")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = confirmPassword.isNotBlank() && !passwordsMatch,
                    supportingText = {
                        if (confirmPassword.isNotBlank() && !passwordsMatch) {
                            Text(localizedString(R.string.register_error_passwords_dont_match, "auth.auth_error_passwords_dont_match"), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = outlinedTextFieldColors()
                )
            }
            
            item {
                // Language Picker
                Text(
                    text = localizedString(R.string.settings_message_language, "settings.settings_message_language"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Surface400,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showLanguagePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Surface800
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val lang = LANGUAGES.find { it.code == selectedLanguage }
                        Text(
                            text = "${lang?.native ?: selectedLanguage} (${lang?.name ?: ""})",
                            color = White
                        )
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Surface400,
                            modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            item {
                // Country Picker
                Text(
                    text = localizedString(R.string.settings_country, "settings.settings_country"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Surface400,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showCountryPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Surface800
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val country = COUNTRIES.find { it.code == selectedCountry }
                        Text(
                            text = "${country?.flag ?: ""} ${country?.name ?: selectedCountry}",
                            color = White
                        )
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Surface400,
                            modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Button(
            onClick = { onRegister(password, username, selectedLanguage, selectedCountry) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple500),
            enabled = isFormValid && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = White)
            } else {
                Text(text = localizedString(R.string.register_button, "auth.register_button"), style = MaterialTheme.typography.titleMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Language Picker Dialog
    if (showLanguagePicker) {
        PickerDialog(
            title = localizedString(R.string.setup_language_title, "setup.setup_language_title"),
            items = LANGUAGES.map { Triple(it.code, "${it.native} (${it.name})", it.code == selectedLanguage) },
            onSelect = { selectedLanguage = it; showLanguagePicker = false },
            onDismiss = { showLanguagePicker = false }
        )
    }
    
    // Country Picker Dialog
    if (showCountryPicker) {
        PickerDialog(
            title = localizedString(R.string.setup_country_title, "setup.setup_country_title"),
            items = COUNTRIES.map { Triple(it.code, "${it.flag} ${it.name}", it.code == selectedCountry) },
            onSelect = { selectedCountry = it; showCountryPicker = false },
            onDismiss = { showCountryPicker = false }
        )
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White,
    unfocusedTextColor = White,
    focusedBorderColor = Purple500,
    unfocusedBorderColor = Surface600,
    focusedLabelColor = Purple500,
    unfocusedLabelColor = Surface400,
    cursorColor = Purple500
)

@Composable
private fun PickerDialog(
    title: String,
    items: List<Triple<String, String, Boolean>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = White) },
        containerColor = Surface900,
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(items) { (code, label, isSelected) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = White)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Purple500)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(localizedString(R.string.cancel, "common.cancel"), color = Purple500)
            }
        }
    )
}
