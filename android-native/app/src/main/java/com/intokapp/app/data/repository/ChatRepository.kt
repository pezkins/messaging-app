package com.intokapp.app.data.repository

import android.util.Log
import com.intokapp.app.data.models.*
import com.intokapp.app.data.network.ApiService
import com.intokapp.app.data.network.TokenManager
import com.intokapp.app.data.network.WebSocketEvent
import com.intokapp.app.data.network.WebSocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService,
    private val webSocketService: WebSocketService,
    private val tokenManager: TokenManager
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
    
    // Track message IDs we've already confirmed to prevent duplicates
    // Limit size to prevent memory leak during long sessions
    private val confirmedMessageIds = mutableSetOf<String>()
    private val MAX_CONFIRMED_IDS = 500
    
    // Track pending optimistic messages for fallback matching
    // Maps tempId to (senderId, content, timestamp) for matching when server doesn't echo tempId
    private data class PendingMessage(val senderId: String, val content: String, val timestamp: Long)
    private val pendingMessages = mutableMapOf<String, PendingMessage>()
    
    init {
        // Subscribe to WebSocket events - use collect with error handling
        scope.launch {
            Log.d(TAG, "🎧 Starting WebSocket event collector")
            try {
                webSocketService.events.collect { event ->
                    Log.d(TAG, "📥 Received WebSocket event: ${event::class.simpleName}")
                    when (event) {
                        is WebSocketEvent.MessageReceived -> {
                            Log.d(TAG, "📨 Processing message: ${event.message.id}, tempId: ${event.tempId}")
                            handleMessageReceived(event.message, event.tempId)
                        }
                        is WebSocketEvent.Typing -> handleTyping(event.conversationId, event.userId, event.isTyping)
                        is WebSocketEvent.Reaction -> handleReaction(event)
                        is WebSocketEvent.MessageDeleted -> handleMessageDeleted(event)
                        is WebSocketEvent.Connected -> Log.d(TAG, "🔌 WebSocket connected event received")
                        is WebSocketEvent.Disconnected -> Log.d(TAG, "🔌 WebSocket disconnected event received")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ WebSocket event collector error: ${e.message}", e)
            }
        }
    }
    
    private fun handleMessageReceived(message: Message, tempId: String?) {
        // Prevent memory leak: clear cache if it grows too large
        if (confirmedMessageIds.size > MAX_CONFIRMED_IDS) {
            Log.d(TAG, "🧹 Clearing confirmed IDs cache (exceeded $MAX_CONFIRMED_IDS)")
            confirmedMessageIds.clear()
        }
        
        val currentMessages = _messages.value.toMutableList()
        
        // Skip if we've already confirmed this message ID (prevents duplicates)
        if (confirmedMessageIds.contains(message.id)) {
            Log.d(TAG, "⏭️ Skipping duplicate message: ${message.id}")
            return
        }
        
        if (tempId != null) {
            // This is our own message being confirmed via tempId
            val index = currentMessages.indexOfFirst { it.id == tempId }
            if (index >= 0) {
                currentMessages[index] = message
                // Mark as confirmed so we skip the broadcast version
                confirmedMessageIds.add(message.id)
                pendingMessages.remove(tempId)
                Log.d(TAG, "✅ Confirmed message: $tempId → ${message.id}")
            } else {
                // Temp message not found - maybe race condition or already processed
                // Add the message anyway if it's for the active conversation
                if (_activeConversation.value?.id == message.conversationId) {
                    if (!currentMessages.any { it.id == message.id }) {
                        currentMessages.add(message)
                        confirmedMessageIds.add(message.id)
                        Log.d(TAG, "⚠️ Temp message not found, adding: $tempId → ${message.id}")
                    }
                }
                pendingMessages.remove(tempId)
            }
        } else {
            // Message without tempId - could be from another user or our message without tempId echo
            val activeConvId = _activeConversation.value?.id
            
            // Check if this might be our own message via fallback matching
            val matchedTempId = findMatchingPendingMessage(message)
            
            if (matchedTempId != null) {
                // This is our message - replace the optimistic one
                val index = currentMessages.indexOfFirst { it.id == matchedTempId }
                if (index >= 0) {
                    currentMessages[index] = message
                    confirmedMessageIds.add(message.id)
                    pendingMessages.remove(matchedTempId)
                    Log.d(TAG, "✅ Confirmed message via fallback: $matchedTempId → ${message.id}")
                } else {
                    // Temp message not found but we matched - add the message
                    if (activeConvId == message.conversationId) {
                        if (!currentMessages.any { it.id == message.id }) {
                            currentMessages.add(message)
                            confirmedMessageIds.add(message.id)
                            Log.d(TAG, "⚠️ Fallback temp not found, adding: $matchedTempId → ${message.id}")
                        }
                    }
                    pendingMessages.remove(matchedTempId)
                }
            } else if (activeConvId == message.conversationId) {
                // This is a message from someone else - add it if not already present
                if (!currentMessages.any { it.id == message.id }) {
                    currentMessages.add(message)
                    Log.d(TAG, "📩 New message from other user: ${message.id}")
                }
            }
        }
        
        _messages.value = currentMessages
        Log.d(TAG, "📊 Messages count: ${currentMessages.size}, Active conv: ${_activeConversation.value?.id}")
        
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
    
    private fun handleMessageDeleted(event: WebSocketEvent.MessageDeleted) {
        Log.d(TAG, "🗑️ Message deleted: ${event.messageId} in ${event.conversationId}")
        
        // Update message list to show deleted placeholder
        val currentMessages = _messages.value.toMutableList()
        val index = currentMessages.indexOfFirst { it.id == event.messageId }
        if (index >= 0) {
            val message = currentMessages[index]
            // Mark message as deleted - UI will show "This message was deleted"
            currentMessages[index] = message.copy(
                type = MessageType.TEXT,
                originalContent = "This message was deleted",
                translatedContent = "This message was deleted",
                attachment = null,
                reactions = emptyMap()
            )
            _messages.value = currentMessages
        }
    }
    
    /**
     * Fallback matching: find a pending optimistic message that matches the received message
     * by sender ID and content (for when server doesn't echo tempId)
     */
    private fun findMatchingPendingMessage(message: Message): String? {
        val now = System.currentTimeMillis()
        val maxAge = 30_000L // 30 seconds - messages older than this are stale
        
        for ((tempId, pending) in pendingMessages) {
            // Match by sender and content, within time window
            if (pending.senderId == message.senderId &&
                pending.content == message.originalContent &&
                now - pending.timestamp < maxAge) {
                return tempId
            }
        }
        
        // Clean up stale pending messages
        val staleIds = pendingMessages.filter { now - it.value.timestamp > maxAge }.keys
        staleIds.forEach { pendingMessages.remove(it) }
        
        return null
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
        
        // IMPORTANT: Set active conversation BEFORE joining WebSocket
        // This prevents race conditions where incoming messages are filtered out
        // because activeConversation wasn't set yet
        _activeConversation.value = conversation
        _messages.value = emptyList()
        _hasMoreMessages.value = false
        nextCursor = null
        
        // Join new conversation AFTER setting activeConversation
        webSocketService.joinConversation(conversation.id)
        
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
        confirmedMessageIds.clear()
        pendingMessages.clear()
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
    
    fun sendMessage(
        content: String, 
        type: String = "TEXT", 
        attachment: Map<String, Any>? = null, 
        translateDocument: Boolean? = null,
        replyToMessage: Message? = null
    ) {
        val conversation = _activeConversation.value ?: return
        if (content.isBlank() && attachment == null) return
        
        val tempId = "temp-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val now = Date()
        
        // Get current user for accurate optimistic message (matching iOS approach)
        val currentUser = runBlocking { tokenManager.getUser() }
        
        // Convert type string to MessageType enum
        val messageType = when (type.uppercase()) {
            "IMAGE" -> MessageType.IMAGE
            "GIF" -> MessageType.GIF
            "FILE" -> MessageType.FILE
            "VOICE" -> MessageType.VOICE
            "ATTACHMENT" -> MessageType.ATTACHMENT
            else -> MessageType.TEXT
        }
        
        // Create attachment object for optimistic message if provided
        val optimisticAttachment = attachment?.let {
            Attachment(
                id = it["id"] as? String ?: "",
                key = it["key"] as? String ?: "",
                fileName = it["fileName"] as? String ?: "",
                contentType = it["contentType"] as? String ?: "",
                fileSize = (it["fileSize"] as? Number)?.toLong() ?: 0L,
                category = it["category"] as? String ?: "",
                url = it["url"] as? String
            )
        }
        
        // Create ReplyTo object for optimistic message if replying
        val optimisticReplyTo = replyToMessage?.let {
            ReplyTo(
                messageId = it.id,
                content = it.originalContent.take(100),
                senderId = it.senderId,
                senderName = it.sender?.username ?: "Unknown",
                type = it.type?.name?.lowercase()
            )
        }
        
        // Create optimistic message with real user data
        val optimisticMessage = Message(
            id = tempId,
            conversationId = conversation.id,
            senderId = currentUser?.id ?: "pending",
            sender = UserPublic(
                id = currentUser?.id ?: "pending",
                username = currentUser?.username ?: "You",
                preferredLanguage = currentUser?.preferredLanguage ?: "en",
                avatarUrl = currentUser?.avatarUrl
            ),
            type = messageType,
            originalContent = content,
            originalLanguage = currentUser?.preferredLanguage ?: "en",
            status = MessageStatus.SENDING,
            createdAt = dateFormat.format(now),
            attachment = optimisticAttachment,
            replyTo = optimisticReplyTo
        )
        
        _messages.value = _messages.value + optimisticMessage
        
        // Track pending message for fallback matching
        pendingMessages[tempId] = PendingMessage(
            senderId = currentUser?.id ?: "pending",
            content = content,
            timestamp = now.time
        )
        
        // Create replyTo map for WebSocket
        val replyToMap = replyToMessage?.let {
            mapOf(
                "messageId" to it.id,
                "content" to it.originalContent.take(100),
                "senderId" to it.senderId,
                "senderName" to (it.sender?.username ?: "Unknown"),
                "type" to (it.type?.name?.lowercase() ?: "text")
            )
        }
        
        // Send via WebSocket
        webSocketService.sendMessage(
            conversationId = conversation.id,
            content = content,
            type = type,
            tempId = tempId,
            attachment = attachment,
            translateDocument = translateDocument,
            replyTo = replyToMap
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
    
    /**
     * Delete a message
     * @param messageId The ID of the message to delete
     * @param forEveryone If true, deletes for all participants; if false, only for current user
     */
    suspend fun deleteMessage(messageId: String, forEveryone: Boolean): Result<Unit> {
        val conversation = _activeConversation.value ?: return Result.failure(Exception("No active conversation"))
        
        Log.d(TAG, "🗑️ DELETE Message - conversationId: ${conversation.id}, messageId: $messageId, forEveryone: $forEveryone")
        
        return try {
            val response = apiService.deleteMessage(
                conversationId = conversation.id,
                messageId = messageId,
                forEveryone = forEveryone
            )
            Log.d(TAG, "🗑️ DELETE Message - Success: ${response.success}")
            
            if (response.success) {
                if (forEveryone) {
                    // For "delete for everyone", mark as deleted (will be broadcast via WebSocket)
                    val currentMessages = _messages.value.toMutableList()
                    val index = currentMessages.indexOfFirst { it.id == messageId }
                    if (index >= 0) {
                        val message = currentMessages[index]
                        currentMessages[index] = message.copy(
                            type = MessageType.TEXT,
                            originalContent = "This message was deleted",
                            translatedContent = "This message was deleted",
                            attachment = null,
                            reactions = emptyMap()
                        )
                        _messages.value = currentMessages
                    }
                } else {
                    // For "delete for me", remove from local list only
                    _messages.value = _messages.value.filter { it.id != messageId }
                }
                Log.d(TAG, "🗑️ Message deleted: $messageId (forEveryone: $forEveryone)")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete message"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete message: ${e.message}", e)
            // Log more details for HTTP errors
            if (e is retrofit2.HttpException) {
                Log.e(TAG, "❌ HTTP ${e.code()}: ${e.response()?.errorBody()?.string()}")
            }
            Result.failure(e)
        }
    }
    
    /**
     * Delete a conversation (removes from user's view only)
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            val response = apiService.deleteConversation(conversationId)
            
            if (response.success) {
                // Remove from local list
                _conversations.value = _conversations.value.filter { it.id != conversationId }
                
                // If this was the active conversation, clear it
                if (_activeConversation.value?.id == conversationId) {
                    clearActiveConversation()
                }
                
                Log.d(TAG, "🗑️ Conversation deleted: $conversationId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete conversation"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete conversation: ${e.message}")
            Result.failure(e)
        }
    }
}


