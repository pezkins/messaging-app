package com.intokapp.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.UserPublic
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import com.intokapp.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemoveParticipantsUiState(
    val conversationId: String = "",
    val participants: List<UserPublic> = emptyList(),
    val currentUserId: String? = null,
    val isLoadingParticipants: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class RemoveParticipantsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RemoveParticipantsUiState())
    val uiState: StateFlow<RemoveParticipantsUiState> = _uiState
    
    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    _uiState.update { it.copy(currentUserId = state.user.id) }
                }
            }
        }
    }
    
    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                conversationId = conversationId,
                isLoadingParticipants = true
            ) }
            
            val conversations = chatRepository.conversations.value
            val conversation = conversations.find { it.id == conversationId }
            
            if (conversation != null) {
                // Filter out current user from the list
                val currentUserId = _uiState.value.currentUserId
                val otherParticipants = conversation.participants.filter { 
                    it.id != currentUserId 
                }
                
                _uiState.update { it.copy(
                    participants = otherParticipants,
                    isLoadingParticipants = false
                ) }
            } else {
                _uiState.update { it.copy(
                    isLoadingParticipants = false,
                    error = "Conversation not found"
                ) }
            }
        }
    }
    
    fun removeParticipant(userId: String) {
        val state = _uiState.value
        if (state.conversationId.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = chatRepository.removeParticipant(state.conversationId, userId)
            
            result.fold(
                onSuccess = { updatedConversation ->
                    // Update local participants list
                    val currentUserId = _uiState.value.currentUserId
                    val remainingParticipants = updatedConversation.participants.filter { 
                        it.id != currentUserId 
                    }
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        participants = remainingParticipants,
                        successMessage = "Participant removed"
                    ) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Failed to remove participant: ${error.message}"
                    ) }
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
