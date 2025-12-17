package com.intokapp.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intokapp.app.data.models.Conversation
import com.intokapp.app.data.models.Message
import com.intokapp.app.data.models.UserPublic

@Entity(tableName = "conversations")
@TypeConverters(ConversationConverters::class)
data class CachedConversation(
    @PrimaryKey
    val id: String,
    val type: String,
    val name: String?,
    val participantsJson: String,
    val lastMessageJson: String?,
    val unreadCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Extension function to convert CachedConversation to Conversation domain model
 */
fun CachedConversation.toConversation(): Conversation {
    val gson = Gson()
    val participantsType = object : TypeToken<List<UserPublic>>() {}.type
    val participants: List<UserPublic> = gson.fromJson(participantsJson, participantsType) ?: emptyList()
    val lastMessage: Message? = lastMessageJson?.let {
        try {
            gson.fromJson(it, Message::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    return Conversation(
        id = id,
        type = type,
        name = name,
        participants = participants,
        lastMessage = lastMessage,
        unreadCount = unreadCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Extension function to convert Conversation domain model to CachedConversation
 */
fun Conversation.toCachedConversation(): CachedConversation {
    val gson = Gson()
    return CachedConversation(
        id = id,
        type = type,
        name = name,
        participantsJson = gson.toJson(participants),
        lastMessageJson = lastMessage?.let { gson.toJson(it) },
        unreadCount = unreadCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Type converters for Room to handle complex types
 */
class ConversationConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromParticipantsList(participants: List<UserPublic>): String {
        return gson.toJson(participants)
    }
    
    @TypeConverter
    fun toParticipantsList(participantsJson: String): List<UserPublic> {
        val type = object : TypeToken<List<UserPublic>>() {}.type
        return gson.fromJson(participantsJson, type) ?: emptyList()
    }
    
    @TypeConverter
    fun fromMessage(message: Message?): String? {
        return message?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toMessage(messageJson: String?): Message? {
        return messageJson?.let {
            try {
                gson.fromJson(it, Message::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
