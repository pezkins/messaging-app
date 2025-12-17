package com.intokapp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.intokapp.app.data.local.entities.CachedConversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    /**
     * Get all conversations ordered by updatedAt (most recent first)
     */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    suspend fun getAll(): List<CachedConversation>
    
    /**
     * Get all conversations as a Flow for reactive updates
     */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<CachedConversation>>
    
    /**
     * Get a single conversation by ID
     */
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getById(conversationId: String): CachedConversation?
    
    /**
     * Insert a single conversation (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: CachedConversation)
    
    /**
     * Insert multiple conversations (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<CachedConversation>)
    
    /**
     * Update a single conversation
     */
    @Update
    suspend fun update(conversation: CachedConversation)
    
    /**
     * Delete a single conversation by ID
     */
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun delete(conversationId: String)
    
    /**
     * Delete all conversations
     */
    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
    
    /**
     * Update last message for a conversation
     */
    @Query("UPDATE conversations SET lastMessageJson = :lastMessageJson, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, lastMessageJson: String?, updatedAt: String)
    
    /**
     * Update unread count for a conversation
     */
    @Query("UPDATE conversations SET unreadCount = :unreadCount WHERE id = :conversationId")
    suspend fun updateUnreadCount(conversationId: String, unreadCount: Int)
    
    /**
     * Clear cache older than specified timestamp
     */
    @Query("DELETE FROM conversations WHERE cachedAt < :timestamp")
    suspend fun clearOldCache(timestamp: Long)
    
    /**
     * Replace all conversations in a single transaction
     */
    @Transaction
    suspend fun replaceAll(conversations: List<CachedConversation>) {
        deleteAll()
        insertAll(conversations)
    }
}
