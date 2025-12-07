package com.intokapp.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.Conversation
import com.intokapp.app.data.models.Message
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import com.intokapp.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val typingUsers: Set<String> = emptySet(),
    val currentUserId: String? = null,
    val displayName: String = "Chat",
    val isLoading: Boolean = false,
    val hasMoreMessages: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState
    
    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    _uiState.update { it.copy(currentUserId = state.user.id) }
                }
            }
        }
        
        viewModelScope.launch {
            chatRepository.messages.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        
        viewModelScope.launch {
            chatRepository.activeConversation.collect { conversation ->
                if (conversation != null) {
                    val displayName = if (conversation.type == "group") {
                        conversation.name ?: "Group Chat"
                    } else {
                        conversation.participants
                            .firstOrNull { it.id != _uiState.value.currentUserId }
                            ?.username ?: "Chat"
                    }
                    _uiState.update { it.copy(conversation = conversation, displayName = displayName) }
                }
            }
        }
        
        viewModelScope.launch {
            chatRepository.typingUsers.collect { typingMap ->
                val conversationId = _uiState.value.conversation?.id
                if (conversationId != null) {
                    _uiState.update { it.copy(typingUsers = typingMap[conversationId] ?: emptySet()) }
                }
            }
        }
        
        viewModelScope.launch {
            chatRepository.hasMoreMessages.collect { hasMore ->
                _uiState.update { it.copy(hasMoreMessages = hasMore) }
            }
        }
        
        viewModelScope.launch {
            chatRepository.isLoadingMessages.collect { isLoading ->
                _uiState.update { it.copy(isLoading = isLoading) }
            }
        }
    }
    
    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val conversations = chatRepository.conversations.value
            val conversation = conversations.find { it.id == conversationId }
            if (conversation != null) {
                chatRepository.selectConversation(conversation)
            }
        }
    }
    
    fun sendMessage(content: String) {
        chatRepository.sendMessage(content)
    }
    
    fun setTyping(isTyping: Boolean) {
        chatRepository.setTyping(isTyping)
    }
    
    fun loadMoreMessages() {
        viewModelScope.launch {
            chatRepository.loadMoreMessages()
        }
    }
    
    fun clearConversation() {
        chatRepository.clearActiveConversation()
    }
}


