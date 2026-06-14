package com.sakuya.conversation.di

import com.sakuya.conversation.data.remote.ConversationApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConversationNetworkModule {
    @Provides
    @Singleton
    fun provideConversationApiService(retrofit: Retrofit): ConversationApiService {
        return retrofit.create(ConversationApiService::class.java)
    }
}
