package com.sakuya.conversation.di

import com.google.gson.Gson
import com.sakuya.conversation.data.remote.ChatWebSocket
import com.sakuya.data.local.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatWebSocketModule {
    @Provides
    @Singleton
    fun provideChatWebSocket(
        okHttpClient: OkHttpClient,
        sessionManager: SessionManager,
        gson: Gson
    ): ChatWebSocket = ChatWebSocket(okHttpClient, sessionManager, gson)
}
