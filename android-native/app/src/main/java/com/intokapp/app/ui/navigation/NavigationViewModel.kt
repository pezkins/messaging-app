package com.intokapp.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val authState: StateFlow<AuthState> = authRepository.authState
    
    fun initialize() {
        viewModelScope.launch {
            authRepository.initialize()
        }
    }
}


