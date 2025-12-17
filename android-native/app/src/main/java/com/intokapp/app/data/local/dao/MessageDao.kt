package com.intokapp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.intokapp.app.data.local.entities.CachedMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    /**
     * Get all messages for a conversation ordered by createdAt (oldest first)
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getByConversationId(conversationId: String): List<CachedMessage>
    
    /**
     * Get messages for a conversation as a Flow for reactive updates
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getByConversationIdFlow(conversationId: String): Flow<List<CachedMessage>>
    
    /**
     * Get latest N messages for a conversation (for pagination)
     */
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLatest(conversationId: String, limit: Int): List<CachedMessage>
    
    /**
     * Get a single message by ID
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getById(messageId: String): CachedMessage?
    
    /**
     * Insert a single message (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: CachedMessage)
    
    /**
     * Insert multiple messages (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<CachedMessage>)
    
    /**
     * Update a single message
     */
    @Update
    suspend fun update(message: CachedMessage)
    
    /**
     * Delete a single message by ID
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun delete(messageId: String)
    
    /**
     * Delete all messages for a conversation
     */
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversationId(conversationId: String)
    
    /**
     * Delete all messages
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
    
    /**
     * Get message count for a conversation
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getCountByConversationId(conversationId: String): Int
    
    /**
     * Update message reactions
     */
    @Query("UPDATE messages SET reactionsJson = :reactionsJson WHERE id = :messageId")
    suspend fun updateReactions(messageId: String, reactionsJson: String?)
    
    /**
     * Mark message as deleted (set content to deleted placeholder)
     */
    @Query("UPDATE messages SET originalContent = :content, translatedContent = :content, type = 'TEXT', attachmentJson = NULL, reactionsJson = NULL WHERE id = :messageId")
    suspend fun markAsDeleted(messageId: String, content: String = "This message was deleted")
    
    /**
     * Keep only the latest N messages per conversation (cleanup)
     */
    @Query("""
        DELETE FROM messages WHERE id IN (
            SELECT id FROM messages 
            WHERE conversationId = :conversationId 
            ORDER BY createdAt DESC 
            LIMIT -1 OFFSET :keepCount
        )
    """)
    suspend fun keepLatestMessages(conversationId: String, keepCount: Int)
    
    /**
     * Clear cache older than specified timestamp
     */
    @Query("DELETE FROM messages WHERE cachedAt < :timestamp")
    suspend fun clearOldCache(timestamp: Long)
    
    /**
     * Replace all messages for a conversation in a single transaction
     */
    @Transaction
    suspend fun replaceAllForConversation(conversationId: String, messages: List<CachedMessage>) {
        deleteByConversationId(conversationId)
        insertAll(messages)
    }
    
    /**
     * Insert messages and cleanup old ones in a single transaction
     */
    @Transaction
    suspend fun insertAndCleanup(conversationId: String, messages: List<CachedMessage>, maxMessages: Int = 100) {
        insertAll(messages)
        keepLatestMessages(conversationId, maxMessages)
    }
}
