package com.intokapp.app.data.local

import android.content.Context
import com.intokapp.app.data.local.dao.ConversationDao
import com.intokapp.app.data.local.dao.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase {
        return ChatDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideConversationDao(database: ChatDatabase): ConversationDao {
        return database.conversationDao()
    }
    
    @Provides
    @Singleton
    fun provideMessageDao(database: ChatDatabase): MessageDao {
        return database.messageDao()
    }
}
