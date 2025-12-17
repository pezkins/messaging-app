package com.intokapp.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intokapp.app.data.models.Attachment
import com.intokapp.app.data.models.Message
import com.intokapp.app.data.models.MessageStatus
import com.intokapp.app.data.models.MessageType
import com.intokapp.app.data.models.ReplyTo
import com.intokapp.app.data.models.UserPublic

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["conversationId", "createdAt"])
    ]
)
@TypeConverters(MessageConverters::class)
data class CachedMessage(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderJson: String?,
    val type: String?,
    val originalContent: String,
    val originalLanguage: String?,
    val translatedContent: String?,
    val targetLanguage: String?,
    val status: String?,
    val createdAt: String,
    val reactionsJson: String?,
    val attachmentJson: String?,
    val readByJson: String?,
    val readAt: String?,
    val replyToJson: String?,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Extension function to convert CachedMessage to Message domain model
 */
fun CachedMessage.toMessage(): Message {
    val gson = Gson()
    
    val sender: UserPublic? = senderJson?.let {
        try {
            gson.fromJson(it, UserPublic::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    val messageType: MessageType? = type?.let {
        try {
            MessageType.valueOf(it)
        } catch (e: Exception) {
            MessageType.TEXT
        }
    }
    
    val messageStatus: MessageStatus? = status?.let {
        try {
            MessageStatus.valueOf(it)
        } catch (e: Exception) {
            null
        }
    }
    
    val reactions: Map<String, List<String>>? = reactionsJson?.let {
        try {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            gson.fromJson(it, type)
        } catch (e: Exception) {
            null
        }
    }
    
    val attachment: Attachment? = attachmentJson?.let {
        try {
            gson.fromJson(it, Attachment::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    val readBy: List<String>? = readByJson?.let {
        try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        } catch (e: Exception) {
            null
        }
    }
    
    val replyTo: ReplyTo? = replyToJson?.let {
        try {
            gson.fromJson(it, ReplyTo::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    return Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        sender = sender,
        type = messageType,
        originalContent = originalContent,
        originalLanguage = originalLanguage,
        translatedContent = translatedContent,
        targetLanguage = targetLanguage,
        status = messageStatus,
        createdAt = createdAt,
        reactions = reactions,
        attachment = attachment,
        readBy = readBy,
        readAt = readAt,
        replyTo = replyTo
    )
}

/**
 * Extension function to convert Message domain model to CachedMessage
 */
fun Message.toCachedMessage(): CachedMessage {
    val gson = Gson()
    return CachedMessage(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderJson = sender?.let { gson.toJson(it) },
        type = type?.name,
        originalContent = originalContent,
        originalLanguage = originalLanguage,
        translatedContent = translatedContent,
        targetLanguage = targetLanguage,
        status = status?.name,
        createdAt = createdAt,
        reactionsJson = reactions?.let { gson.toJson(it) },
        attachmentJson = attachment?.let { gson.toJson(it) },
        readByJson = readBy?.let { gson.toJson(it) },
        readAt = readAt,
        replyToJson = replyTo?.let { gson.toJson(it) }
    )
}

/**
 * Type converters for Room to handle complex types in messages
 */
class MessageConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromReactionsMap(reactions: Map<String, List<String>>?): String? {
        return reactions?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toReactionsMap(reactionsJson: String?): Map<String, List<String>>? {
        return reactionsJson?.let {
            try {
                val type = object : TypeToken<Map<String, List<String>>>() {}.type
                gson.fromJson(it, type)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    @TypeConverter
    fun fromAttachment(attachment: Attachment?): String? {
        return attachment?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toAttachment(attachmentJson: String?): Attachment? {
        return attachmentJson?.let {
            try {
                gson.fromJson(it, Attachment::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    @TypeConverter
    fun fromUserPublic(user: UserPublic?): String? {
        return user?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toUserPublic(userJson: String?): UserPublic? {
        return userJson?.let {
            try {
                gson.fromJson(it, UserPublic::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringList(listJson: String?): List<String>? {
        return listJson?.let {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(it, type)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    @TypeConverter
    fun fromReplyTo(replyTo: ReplyTo?): String? {
        return replyTo?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toReplyTo(replyToJson: String?): ReplyTo? {
        return replyToJson?.let {
            try {
                gson.fromJson(it, ReplyTo::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
