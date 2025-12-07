package com.intokapp.app.ui.screens.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.UserPublic
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import com.intokapp.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewChatUiState(
    val searchResults: List<UserPublic> = emptyList(),
    val selectedUsers: List<UserPublic> = emptyList(),
    val currentUserId: String? = null,
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val createdConversationId: String? = null,
    val error: String? = null
)

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NewChatUiState())
    val uiState: StateFlow<NewChatUiState> = _uiState
    
    private var searchJob: Job? = null
    
    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    _uiState.update { it.copy(currentUserId = state.user.id) }
                }
            }
        }
    }
    
    fun searchUsers(query: String) {
        searchJob?.cancel()
        
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(300) // Debounce
            
            val results = chatRepository.searchUsers(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }
    
    fun toggleUserSelection(user: UserPublic) {
        val current = _uiState.value.selectedUsers.toMutableList()
        
        if (current.any { it.id == user.id }) {
            current.removeAll { it.id == user.id }
        } else {
            current.add(user)
        }
        
        _uiState.update { it.copy(selectedUsers = current) }
    }
    
    fun startDirectChat(user: UserPublic) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val conversation = chatRepository.startConversation(user)
            
            if (conversation != null) {
                _uiState.update { it.copy(isLoading = false, createdConversationId = conversation.id) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to create conversation") }
            }
        }
    }
    
    fun createGroup(name: String?) {
        val users = _uiState.value.selectedUsers
        if (users.size < 2) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val conversation = chatRepository.startGroupConversation(users, name)
            
            if (conversation != null) {
                _uiState.update { it.copy(isLoading = false, createdConversationId = conversation.id) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to create group") }
            }
        }
    }
}
