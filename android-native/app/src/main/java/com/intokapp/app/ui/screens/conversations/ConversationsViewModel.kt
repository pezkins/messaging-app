package com.intokapp.app.ui.screens.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.Conversation
import com.intokapp.app.data.models.User
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import com.intokapp.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState
    
    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    _uiState.update { it.copy(user = state.user) }
                }
            }
        }
        
        viewModelScope.launch {
            chatRepository.conversations.collect { conversations ->
                _uiState.update { it.copy(conversations = conversations) }
            }
        }
        
        viewModelScope.launch {
            chatRepository.isLoadingConversations.collect { isLoading ->
                _uiState.update { it.copy(isLoading = isLoading) }
            }
        }
    }
    
    fun loadConversations() {
        viewModelScope.launch {
            chatRepository.loadConversations()
        }
    }
}


