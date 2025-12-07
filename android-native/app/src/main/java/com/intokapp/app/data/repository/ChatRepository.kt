package com.intokapp.app.data.repository

import android.util.Log
import com.intokapp.app.data.models.*
import com.intokapp.app.data.network.ApiService
import com.intokapp.app.data.network.CreateConversationRequest
import com.intokapp.app.data.network.WebSocketEvent
import com.intokapp.app.data.network.WebSocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val webSocketService: WebSocketService
) {
    private val TAG = "ChatRepository"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations
    
    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    private val _typingUsers = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val typingUsers: StateFlow<Map<String, Set<String>>> = _typingUsers
    
    private val _isLoadingConversations = MutableStateFlow(false)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations
    
    private val _isLoadingMessages = MutableStateFlow(false)
    val isLoadingMessages: StateFlow<Boolean> = _isLoadingMessages
    
    private val _hasMoreMessages = MutableStateFlow(false)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages
    
    private var nextCursor: String? = null
    
    init {
        // Subscribe to WebSocket events
        scope.launch {
            webSocketService.events.collect { event ->
                when (event) {
                    is WebSocketEvent.MessageReceived -> handleMessageReceived(event.message, event.tempId)
                    is WebSocketEvent.Typing -> handleTyping(event.conversationId, event.userId, event.isTyping)
                    is WebSocketEvent.Reaction -> handleReaction(event)
                    else -> {}
                }
            }
        }
    }
    
    private fun handleMessageReceived(message: Message, tempId: String?) {
        val currentMessages = _messages.value.toMutableList()
        
        if (tempId != null) {
            val index = currentMessages.indexOfFirst { it.id == tempId }
            if (index >= 0) {
                currentMessages[index] = message
            }
        } else if (_activeConversation.value?.id == message.conversationId) {
            if (!currentMessages.any { it.id == message.id }) {
                currentMessages.add(message)
            }
        }
        
        _messages.value = currentMessages
        
        // Update conversation's last message
        val currentConvs = _conversations.value.toMutableList()
        val convIndex = currentConvs.indexOfFirst { it.id == message.conversationId }
        if (convIndex >= 0) {
            val conv = currentConvs[convIndex]
            currentConvs[convIndex] = conv.copy(lastMessage = message, updatedAt = message.createdAt)
            currentConvs.sortByDescending { it.updatedAt }
            _conversations.value = currentConvs
        }
        
        Log.d(TAG, "📨 Message received in ${message.conversationId}")
    }
    
    private fun handleTyping(conversationId: String, userId: String, isTyping: Boolean) {
        val current = _typingUsers.value.toMutableMap()
        val users = current[conversationId]?.toMutableSet() ?: mutableSetOf()
        
        if (isTyping) {
            users.add(userId)
        } else {
            users.remove(userId)
        }
        
        current[conversationId] = users
        _typingUsers.value = current
    }
    
    private fun handleReaction(event: WebSocketEvent.Reaction) {
        val currentMessages = _messages.value.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == event.messageId }
        if (index >= 0) {
            val message = currentMessages[index]
            currentMessages[index] = message.copy(reactions = event.reactions)
            _messages.value = currentMessages
        }
    }
    
    suspend fun loadConversations() {
        if (_isLoadingConversations.value) return
        
        _isLoadingConversations.value = true
        Log.d(TAG, "📥 Loading conversations...")
        
        try {
            val response = apiService.getConversations()
            _conversations.value = response.conversations
            Log.d(TAG, "✅ Loaded ${response.conversations.size} conversations")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load conversations: ${e.message}")
        }
        
        _isLoadingConversations.value = false
    }
    
    suspend fun selectConversation(conversation: Conversation) {
        // Leave previous conversation
        _activeConversation.value?.let {
            webSocketService.leaveConversation(it.id)
        }
        
        // Join new conversation
        webSocketService.joinConversation(conversation.id)
        
        _activeConversation.value = conversation
        _messages.value = emptyList()
        _hasMoreMessages.value = false
        nextCursor = null
        
        _isLoadingMessages.value = true
        
        try {
            val response = apiService.getMessages(conversation.id)
            _messages.value = response.messages
            _hasMoreMessages.value = response.hasMore
            nextCursor = response.nextCursor
            Log.d(TAG, "✅ Loaded ${response.messages.size} messages")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load messages: ${e.message}")
        }
        
        _isLoadingMessages.value = false
    }
    
    fun clearActiveConversation() {
        _activeConversation.value?.let {
            webSocketService.leaveConversation(it.id)
        }
        _activeConversation.value = null
        _messages.value = emptyList()
        _hasMoreMessages.value = false
        nextCursor = null
    }
    
    suspend fun loadMoreMessages() {
        val conversation = _activeConversation.value ?: return
        if (!_hasMoreMessages.value || _isLoadingMessages.value || nextCursor == null) return
        
        _isLoadingMessages.value = true
        
        try {
            val response = apiService.getMessages(conversation.id, cursor = nextCursor)
            _messages.value = response.messages + _messages.value
            _hasMoreMessages.value = response.hasMore
            nextCursor = response.nextCursor
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load more messages: ${e.message}")
        }
        
        _isLoadingMessages.value = false
    }
    
    fun sendMessage(content: String, type: String = "TEXT", attachment: Map<String, Any>? = null) {
        val conversation = _activeConversation.value ?: return
        if (content.isBlank() && attachment == null) return
        
        val tempId = "temp-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        // Create optimistic message
        val optimisticMessage = Message(
            id = tempId,
            conversationId = conversation.id,
            senderId = "",
            sender = UserPublic("", "You", "en", null),
            type = MessageType.TEXT,
            originalContent = content,
            originalLanguage = "en",
            status = MessageStatus.SENDING,
            createdAt = dateFormat.format(Date())
        )
        
        _messages.value = _messages.value + optimisticMessage
        
        // Send via WebSocket
        webSocketService.sendMessage(
            conversationId = conversation.id,
            content = content,
            type = type,
            tempId = tempId,
            attachment = attachment
        )
    }
    
    suspend fun startConversation(user: UserPublic): Conversation? {
        // Check for existing direct conversation
        val existing = _conversations.value.find { conv ->
            conv.type == "direct" && conv.participants.any { it.id == user.id }
        }
        
        if (existing != null) {
            selectConversation(existing)
            return existing
        }
        
        // Create new conversation
        return try {
            val response = apiService.createConversation(
                CreateConversationRequest(
                    participantIds = listOf(user.id),
                    type = "direct"
                )
            )
            
            _conversations.value = listOf(response.conversation) + _conversations.value
            selectConversation(response.conversation)
            response.conversation
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start conversation: ${e.message}")
            null
        }
    }
    
    suspend fun startGroupConversation(users: List<UserPublic>, name: String? = null): Conversation? {
        return try {
            val response = apiService.createConversation(
                CreateConversationRequest(
                    participantIds = users.map { it.id },
                    type = "group",
                    name = name
                )
            )
            
            _conversations.value = listOf(response.conversation) + _conversations.value
            selectConversation(response.conversation)
            response.conversation
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start group: ${e.message}")
            null
        }
    }
    
    fun setTyping(isTyping: Boolean) {
        val conversation = _activeConversation.value ?: return
        webSocketService.sendTyping(conversation.id, isTyping)
    }
    
    fun sendReaction(messageId: String, messageTimestamp: String, emoji: String) {
        val conversation = _activeConversation.value ?: return
        webSocketService.sendReaction(conversation.id, messageId, messageTimestamp, emoji)
    }
    
    suspend fun searchUsers(query: String): List<UserPublic> {
        if (query.length < 2) return emptyList()
        
        return try {
            apiService.searchUsers(query).users
        } catch (e: Exception) {
            Log.e(TAG, "❌ User search failed: ${e.message}")
            emptyList()
        }
    }
}
