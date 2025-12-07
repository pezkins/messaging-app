package com.intokapp.app.data.models

import com.google.gson.annotations.SerializedName

// ============================================
// User Models
// ============================================

data class User(
    val id: String,
    val email: String,
    val username: String,
    @SerializedName("preferredLanguage")
    val preferredLanguage: String,
    @SerializedName("preferredCountry")
    val preferredCountry: String? = null,
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class UserPublic(
    val id: String,
    val username: String,
    @SerializedName("preferredLanguage")
    val preferredLanguage: String,
    @SerializedName("avatarUrl")
    val avatarUrl: String? = null
)

// ============================================
// Auth Models
// ============================================

data class AuthResponse(
    val user: User,
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String,
    @SerializedName("isNewUser")
    val isNewUser: Boolean = false
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    @SerializedName("preferredLanguage")
    val preferredLanguage: String,
    @SerializedName("preferredCountry")
    val preferredCountry: String? = null
)

data class OAuthRequest(
    val provider: String,
    val providerId: String,
    val email: String,
    val name: String? = null,
    val avatarUrl: String? = null
)

// ============================================
// Conversation Models
// ============================================

data class Conversation(
    val id: String,
    val type: String, // "direct" or "group"
    val name: String? = null,
    val participants: List<UserPublic>,
    @SerializedName("lastMessage")
    val lastMessage: Message? = null,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)

data class CreateConversationRequest(
    @SerializedName("participantIds")
    val participantIds: List<String>,
    val type: String,
    val name: String? = null
)

// ============================================
// Message Models
// ============================================

enum class MessageStatus {
    @SerializedName("sending")
    SENDING,
    @SerializedName("sent")
    SENT,
    @SerializedName("delivered")
    DELIVERED,
    @SerializedName("seen")
    SEEN,
    @SerializedName("failed")
    FAILED
}

enum class MessageType {
    @SerializedName("text")
    TEXT,
    @SerializedName("voice")
    VOICE,
    @SerializedName("image")
    IMAGE,
    @SerializedName("file")
    FILE,
    @SerializedName("gif")
    GIF,
    @SerializedName("attachment")
    ATTACHMENT
}

data class Message(
    val id: String,
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("senderId")
    val senderId: String,
    val sender: UserPublic,
    val type: MessageType = MessageType.TEXT,
    @SerializedName("originalContent")
    val originalContent: String,
    @SerializedName("originalLanguage")
    val originalLanguage: String,
    @SerializedName("translatedContent")
    val translatedContent: String? = null,
    @SerializedName("targetLanguage")
    val targetLanguage: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    @SerializedName("createdAt")
    val createdAt: String,
    val reactions: Map<String, List<String>>? = null,
    val attachment: Attachment? = null
)

data class Attachment(
    val id: String,
    val key: String,
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("contentType")
    val contentType: String,
    @SerializedName("fileSize")
    val fileSize: Long,
    val category: String, // "image", "video", "document", "audio"
    val url: String? = null
)

// ============================================
// WebSocket Models
// ============================================

data class WebSocketMessage(
    val action: String,
    val data: Any? = null
)

data class SendMessagePayload(
    @SerializedName("conversationId")
    val conversationId: String,
    val content: String,
    val type: String = "TEXT",
    @SerializedName("tempId")
    val tempId: String? = null,
    val attachment: Attachment? = null
)

data class TypingPayload(
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("isTyping")
    val isTyping: Boolean
)

data class ReactionPayload(
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("messageId")
    val messageId: String,
    val timestamp: String,
    val emoji: String
)

// ============================================
// API Response Models
// ============================================

data class ConversationsResponse(
    val conversations: List<Conversation>
)

data class MessagesResponse(
    val messages: List<Message>,
    val hasMore: Boolean,
    val nextCursor: String?
)

data class UsersSearchResponse(
    val users: List<UserPublic>
)

