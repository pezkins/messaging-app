package com.intokapp.app.ui.screens.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.Conversation
import com.intokapp.app.data.models.User
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import com.intokapp.app.data.repository.ChangelogEntry
import com.intokapp.app.data.repository.ChatRepository
import com.intokapp.app.data.repository.WhatsNewManager
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
    val error: String? = null,
    val showWhatsNew: Boolean = false,
    val whatsNewEntries: List<ChangelogEntry> = emptyList(),
    // Delete conversation state
    val showDeleteDialog: Boolean = false,
    val conversationToDelete: Conversation? = null,
    val isDeletingConversation: Boolean = false
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val whatsNewManager: WhatsNewManager
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
        
        // Check if we should show What's New dialog
        checkWhatsNew()
    }
    
    private fun checkWhatsNew() {
        if (whatsNewManager.shouldShowWhatsNew()) {
            _uiState.update { it.copy(
                showWhatsNew = true,
                whatsNewEntries = whatsNewManager.getNewEntries()
            ) }
        }
    }
    
    fun dismissWhatsNew() {
        whatsNewManager.markAsSeen()
        _uiState.update { it.copy(showWhatsNew = false, whatsNewEntries = emptyList()) }
    }
    
    fun loadConversations() {
        viewModelScope.launch {
            chatRepository.loadConversations()
        }
    }
    
    // ============================================
    // Delete Conversations
    // ============================================
    
    fun showDeleteConversationDialog(conversation: Conversation) {
        _uiState.update { it.copy(showDeleteDialog = true, conversationToDelete = conversation) }
    }
    
    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false, conversationToDelete = null) }
    }
    
    fun deleteConversation() {
        val conversation = _uiState.value.conversationToDelete ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingConversation = true) }
            
            val result = chatRepository.deleteConversation(conversation.id)
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        isDeletingConversation = false,
                        showDeleteDialog = false,
                        conversationToDelete = null
                    ) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isDeletingConversation = false,
                        error = "Failed to delete conversation: ${error.message}"
                    ) }
                }
            )
        }
    }
}


