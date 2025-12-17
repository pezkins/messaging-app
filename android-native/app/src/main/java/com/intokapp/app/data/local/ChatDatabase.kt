package com.intokapp.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.intokapp.app.data.local.dao.ConversationDao
import com.intokapp.app.data.local.dao.MessageDao
import com.intokapp.app.data.local.entities.CachedConversation
import com.intokapp.app.data.local.entities.CachedMessage
import com.intokapp.app.data.local.entities.ConversationConverters
import com.intokapp.app.data.local.entities.MessageConverters

@Database(
    entities = [
        CachedConversation::class,
        CachedMessage::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ConversationConverters::class, MessageConverters::class)
abstract class ChatDatabase : RoomDatabase() {
    
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    
    companion object {
        private const val DATABASE_NAME = "intok_chat_cache.db"
        
        @Volatile
        private var INSTANCE: ChatDatabase? = null
        
        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // Clear DB on schema changes (cache only)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
