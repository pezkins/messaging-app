package com.intokapp.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.User
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isSignedOut: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState
    
    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        _uiState.update { it.copy(user = state.user) }
                    }
                    is AuthState.Unauthenticated -> {
                        _uiState.update { it.copy(isSignedOut = true) }
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun updateUsername(username: String) {
        if (username.length < 2) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.updateUsername(username)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.updateLanguage(languageCode)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun updateCountry(countryCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.updateCountry(countryCode)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}


