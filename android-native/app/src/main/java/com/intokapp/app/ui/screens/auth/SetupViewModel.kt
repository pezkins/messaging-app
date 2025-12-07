package com.intokapp.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.constants.Country
import com.intokapp.app.data.constants.Language
import com.intokapp.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val displayName: String = "",
    val selectedLanguage: Language? = null,
    val selectedCountry: Country? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState
    
    fun setDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name, error = null) }
    }
    
    fun setLanguage(language: Language) {
        _uiState.update { it.copy(selectedLanguage = language, error = null) }
    }
    
    fun setCountry(country: Country) {
        _uiState.update { it.copy(selectedCountry = country, error = null) }
    }
    
    fun saveDisplayName(onSuccess: () -> Unit) {
        val name = _uiState.value.displayName
        if (name.length < 2) {
            _uiState.update { it.copy(error = "Name must be at least 2 characters") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.updateUsername(name)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
    
    fun saveLanguage(onSuccess: () -> Unit) {
        val language = _uiState.value.selectedLanguage ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.updateLanguage(language.code)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
    
    fun saveCountryAndComplete() {
        val country = _uiState.value.selectedCountry
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            if (country != null) {
                authRepository.updateCountry(country.code)
            }
            
            authRepository.completeSetup()
            _uiState.update { it.copy(isLoading = false, isComplete = true) }
        }
    }
    
    fun completeSetup() {
        viewModelScope.launch {
            authRepository.completeSetup()
            _uiState.update { it.copy(isComplete = true) }
        }
    }
}
