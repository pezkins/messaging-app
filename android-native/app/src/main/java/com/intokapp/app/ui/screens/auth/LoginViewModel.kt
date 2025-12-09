package com.intokapp.app.ui.screens.auth

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EmailAuthStep {
    INITIAL,        // Main login screen
    EMAIL_INPUT,    // Enter email
    PASSWORD_INPUT, // Existing user - enter password
    REGISTRATION    // New user - create password + profile
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isNewUser: Boolean = false,
    val error: String? = null,
    // Email auth state
    val emailAuthStep: EmailAuthStep = EmailAuthStep.INITIAL,
    val email: String = "",
    val emailExists: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState
    
    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                isLoggedIn = true, 
                                isNewUser = state.needsSetup
                            ) 
                        }
                    }
                    is AuthState.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = state.message) }
                    }
                    is AuthState.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun getGoogleSignInIntent(): Intent {
        Log.d("LoginViewModel", "🔘 Getting Google Sign-In Intent")
        _uiState.update { it.copy(isLoading = true, error = null) }
        return authRepository.getGoogleSignInIntent()
    }
    
    fun handleGoogleSignInResult(data: Intent?) {
        Log.d("LoginViewModel", "📥 Handling Google Sign-In result")
        viewModelScope.launch {
            try {
                val result = authRepository.handleGoogleSignInResult(data)
                Log.d("LoginViewModel", "📥 Got result: $result")
                result.onFailure { e ->
                    Log.e("LoginViewModel", "❌ Sign-in failed: ${e.message}", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "💥 Exception handling result: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            // Success handled by authState collector
        }
    }
    
    fun cancelLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }
    
    // Email Auth Methods
    fun startEmailAuth() {
        Log.d("LoginViewModel", "📧 Starting email auth flow")
        _uiState.update { it.copy(emailAuthStep = EmailAuthStep.EMAIL_INPUT, error = null) }
    }
    
    fun goBackToInitial() {
        _uiState.update { 
            it.copy(
                emailAuthStep = EmailAuthStep.INITIAL, 
                email = "", 
                error = null,
                isLoading = false
            ) 
        }
    }
    
    fun checkEmail(email: String) {
        Log.d("LoginViewModel", "📧 Checking email: $email")
        _uiState.update { it.copy(isLoading = true, error = null, email = email) }
        
        viewModelScope.launch {
            val result = authRepository.checkEmail(email)
            result.onSuccess { exists ->
                Log.d("LoginViewModel", "📧 Email exists: $exists")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        emailExists = exists,
                        emailAuthStep = if (exists) EmailAuthStep.PASSWORD_INPUT else EmailAuthStep.REGISTRATION
                    ) 
                }
            }.onFailure { e ->
                Log.e("LoginViewModel", "❌ Email check failed: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to check email") }
            }
        }
    }
    
    fun loginWithEmail(password: String) {
        val email = _uiState.value.email
        Log.d("LoginViewModel", "🔐 Logging in with email: $email")
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            val result = authRepository.loginWithEmail(email, password)
            result.onSuccess { isNewUser ->
                Log.d("LoginViewModel", "✅ Login successful")
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isNewUser = isNewUser) }
            }.onFailure { e ->
                Log.e("LoginViewModel", "❌ Login failed: ${e.message}")
                val errorMsg = if (e.message?.contains("401") == true) {
                    "Invalid password. Please try again."
                } else {
                    e.message ?: "Login failed"
                }
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
            }
        }
    }
    
    fun registerWithEmail(
        password: String,
        username: String,
        preferredLanguage: String,
        preferredCountry: String
    ) {
        val email = _uiState.value.email
        Log.d("LoginViewModel", "📝 Registering with email: $email")
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            val result = authRepository.registerWithEmail(
                email = email,
                password = password,
                username = username,
                preferredLanguage = preferredLanguage,
                preferredCountry = preferredCountry
            )
            result.onSuccess { isNewUser ->
                Log.d("LoginViewModel", "✅ Registration successful")
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isNewUser = false) }
            }.onFailure { e ->
                Log.e("LoginViewModel", "❌ Registration failed: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Registration failed") }
            }
        }
    }
}
