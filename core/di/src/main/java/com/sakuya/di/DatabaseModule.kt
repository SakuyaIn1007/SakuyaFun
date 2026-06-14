package com.sakuya.di

import android.content.Context
import androidx.room.Room
import com.sakuya.data.local.AppDatabase
import com.sakuya.data.local.dao.ChatMessageDao
import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.data.local.dao.UserDao
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sakuya.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideConversationDao(
        database: AppDatabase
    ): ConversationDao = database.conversationDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(
        database: AppDatabase
    ): ChatMessageDao = database.chatMessageDao()

    @Provides
    @Singleton
    fun provideUserDao(
        database: AppDatabase
    ): UserDao = database.userDao()
}