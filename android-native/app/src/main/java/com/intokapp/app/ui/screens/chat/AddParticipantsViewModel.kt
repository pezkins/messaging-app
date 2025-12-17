package com.intokapp.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.UserPublic
import com.intokapp.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddParticipantsUiState(
    val conversationId: String = "",
    val existingParticipantIds: Set<String> = emptySet(),
    val searchResults: List<UserPublic> = emptyList(),
    val selectedUsers: List<UserPublic> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class AddParticipantsViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddParticipantsUiState())
    val uiState: StateFlow<AddParticipantsUiState> = _uiState
    
    private var searchJob: Job? = null
    
    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val conversations = chatRepository.conversations.value
            val conversation = conversations.find { it.id == conversationId }
            
            if (conversation != null) {
                _uiState.update { it.copy(
                    conversationId = conversationId,
                    existingParticipantIds = conversation.participants.map { p -> p.id }.toSet()
                ) }
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
            
            // Debounce
            delay(300)
            
            val results = chatRepository.searchUsers(query)
            
            // Filter out existing participants
            val filteredResults = results.filter { user ->
                !_uiState.value.existingParticipantIds.contains(user.id)
            }
            
            _uiState.update { it.copy(
                searchResults = results, // Show all, but mark existing ones as disabled
                isSearching = false
            ) }
        }
    }
    
    fun toggleUserSelection(user: UserPublic) {
        _uiState.update { state ->
            val currentSelected = state.selectedUsers.toMutableList()
            val existingIndex = currentSelected.indexOfFirst { it.id == user.id }
            
            if (existingIndex >= 0) {
                currentSelected.removeAt(existingIndex)
            } else {
                currentSelected.add(user)
            }
            
            state.copy(selectedUsers = currentSelected)
        }
    }
    
    fun addSelectedParticipants() {
        val state = _uiState.value
        if (state.selectedUsers.isEmpty() || state.conversationId.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val userIds = state.selectedUsers.map { it.id }
            val result = chatRepository.addParticipants(state.conversationId, userIds)
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, success = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to add participants: ${error.message}"
                    ) }
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
