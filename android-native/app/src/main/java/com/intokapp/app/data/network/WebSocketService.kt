package com.intokapp.app.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intokapp.app.data.models.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

// MARK: - Event Types
sealed class WebSocketEvent {
    data class MessageReceived(val message: Message, val tempId: String?) : WebSocketEvent()
    data class Typing(val conversationId: String, val userId: String, val isTyping: Boolean) : WebSocketEvent()
    data class Reaction(
        val conversationId: String,
        val messageId: String,
        val messageTimestamp: String,
        val reactions: Map<String, List<String>>,
        val userId: String,
        val emoji: String
    ) : WebSocketEvent()
    data class MessageDeleted(
        val conversationId: String,
        val messageId: String,
        val deletedBy: String,
        val deletedAt: String
    ) : WebSocketEvent()
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
}

@Singleton
class WebSocketService @Inject constructor(
    private val wsUrl: String,
    private val tokenManager: TokenManager
) {
    private val TAG = "WebSocketService"
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 1000L
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    // Buffer events to prevent loss due to timing between emit and collect
    private val _events = MutableSharedFlow<WebSocketEvent>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<WebSocketEvent> = _events
    
    private val client = OkHttpClient.Builder()
        .build()
    
    fun connect() {
        val token = tokenManager.getAccessToken() ?: run {
            Log.w(TAG, "No token available for WebSocket connection")
            return
        }
        
        if (webSocket != null) {
            Log.d(TAG, "WebSocket already connected or connecting")
            return
        }
        
        val url = "$wsUrl?token=$token"
        Log.d(TAG, "Connecting to WebSocket: $wsUrl")
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "🔌 WebSocket connected")
                reconnectAttempts = 0
                _isConnected.value = true
                scope.launch {
                    _events.emit(WebSocketEvent.Connected)
                }
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "🔌 WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "🔌 WebSocket closed: $code $reason")
                _isConnected.value = false
                this@WebSocketService.webSocket = null
                scope.launch {
                    _events.emit(WebSocketEvent.Disconnected)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "🔌 WebSocket error: ${t.message}")
                _isConnected.value = false
                this@WebSocketService.webSocket = null
                scope.launch {
                    _events.emit(WebSocketEvent.Disconnected)
                }
                handleReconnect()
            }
        })
    }
    
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting WebSocket")
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        reconnectAttempts = 0
        _isConnected.value = false
    }
    
    private fun handleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.w(TAG, "🔌 Max reconnect attempts reached")
            return
        }
        
        reconnectAttempts++
        val delay = baseReconnectDelay * 2.0.pow(reconnectAttempts - 1).toLong()
        
        Log.d(TAG, "🔌 Reconnecting in ${delay}ms (attempt $reconnectAttempts)")
        
        scope.launch {
            delay(delay)
            connect()
        }
    }
    
    private fun handleMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val action = json.get("action")?.asString ?: return
            
            Log.d(TAG, "🔌 Received: $action")
            
            when (action) {
                "message:receive" -> {
                    val message = gson.fromJson(json.get("message"), Message::class.java)
                    val tempId = json.get("tempId")?.asString
                    scope.launch {
                        _events.emit(WebSocketEvent.MessageReceived(message, tempId))
                    }
                }
                
                "message:typing" -> {
                    val conversationId = json.get("conversationId")?.asString ?: return
                    val userId = json.get("userId")?.asString ?: return
                    val isTyping = json.get("isTyping")?.asBoolean ?: false
                    scope.launch {
                        _events.emit(WebSocketEvent.Typing(conversationId, userId, isTyping))
                    }
                }
                
                "message:reaction" -> {
                    val conversationId = json.get("conversationId")?.asString ?: return
                    val messageId = json.get("messageId")?.asString ?: return
                    val messageTimestamp = json.get("messageTimestamp")?.asString ?: return
                    val userId = json.get("userId")?.asString ?: return
                    val emoji = json.get("emoji")?.asString ?: return
                    val reactionsJson = json.getAsJsonObject("reactions")
                    val reactions = mutableMapOf<String, List<String>>()
                    reactionsJson?.entrySet()?.forEach { entry ->
                        reactions[entry.key] = entry.value.asJsonArray.map { it.asString }
                    }
                    scope.launch {
                        _events.emit(WebSocketEvent.Reaction(conversationId, messageId, messageTimestamp, reactions, userId, emoji))
                    }
                }
                
                "message:deleted" -> {
                    val conversationId = json.get("conversationId")?.asString ?: return
                    val messageId = json.get("messageId")?.asString ?: return
                    val deletedBy = json.get("deletedBy")?.asString ?: return
                    val deletedAt = json.get("deletedAt")?.asString ?: return
                    scope.launch {
                        _events.emit(WebSocketEvent.MessageDeleted(conversationId, messageId, deletedBy, deletedAt))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔌 Failed to parse message: ${e.message}")
        }
    }
    
    // MARK: - Send Methods
    private fun send(action: String, data: Map<String, Any>) {
        val ws = webSocket ?: run {
            Log.w(TAG, "🔌 Cannot send - not connected")
            return
        }
        
        val payload = mapOf(
            "action" to action,
            "data" to data
        )
        
        val json = gson.toJson(payload)
        ws.send(json)
        Log.d(TAG, "🔌 Sent: $action")
    }
    
    fun sendMessage(
        conversationId: String,
        content: String,
        type: String = "TEXT",
        tempId: String? = null,
        attachment: Map<String, Any>? = null,
        translateDocument: Boolean? = null,
        replyTo: Map<String, Any>? = null
    ) {
        val data = mutableMapOf<String, Any>(
            "conversationId" to conversationId,
            "content" to content,
            "type" to type.lowercase()
        )
        tempId?.let { data["tempId"] = it }
        attachment?.let { data["attachment"] = it }
        // Only include for documents
        translateDocument?.let { data["translateDocument"] = it }
        // Include reply reference if replying to a message
        replyTo?.let { data["replyTo"] = it }
        
        send("message:send", data)
    }
    
    fun sendTyping(conversationId: String, isTyping: Boolean) {
        send("message:typing", mapOf(
            "conversationId" to conversationId,
            "isTyping" to isTyping
        ))
    }
    
    fun markAsRead(conversationId: String, messageId: String) {
        send("message:read", mapOf(
            "conversationId" to conversationId,
            "messageId" to messageId
        ))
    }
    
    fun joinConversation(conversationId: String) {
        send("conversation:join", mapOf("conversationId" to conversationId))
    }
    
    fun leaveConversation(conversationId: String) {
        send("conversation:leave", mapOf("conversationId" to conversationId))
    }
    
    fun sendReaction(conversationId: String, messageId: String, messageTimestamp: String, emoji: String) {
        send("message:reaction", mapOf(
            "conversationId" to conversationId,
            "messageId" to messageId,
            "messageTimestamp" to messageTimestamp,
            "emoji" to emoji
        ))
    }
}


