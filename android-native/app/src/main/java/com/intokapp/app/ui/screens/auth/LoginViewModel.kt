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

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isNewUser: Boolean = false,
    val error: String? = null
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
}


