package com.intokapp.app.ui.screens.auth

import android.app.Activity
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
    
    fun signInWithGoogle(activity: Activity) {
        Log.d("LoginViewModel", "🔘 signInWithGoogle button clicked!")
        viewModelScope.launch {
            Log.d("LoginViewModel", "🔄 Starting coroutine for Google Sign-In")
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                Log.d("LoginViewModel", "📞 Calling authRepository.signInWithGoogle...")
                val result = authRepository.signInWithGoogle(activity)
                Log.d("LoginViewModel", "📥 Got result: $result")
                result.onFailure { e ->
                    Log.e("LoginViewModel", "❌ Sign-in failed: ${e.message}", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "💥 Exception in signInWithGoogle: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
            // Success handled by authState collector
        }
    }
}
