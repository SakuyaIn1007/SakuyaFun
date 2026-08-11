package com.sakuya.friend.di

import com.sakuya.friend.data.remote.FriendApiService
import com.sakuya.friend.data.remote.GroupApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FriendNetworkModule {
    @Provides
    @Singleton
    fun provideFriendApiService(retrofit: Retrofit): FriendApiService {
        return retrofit.create(FriendApiService::class.java)
    }

    /** 群聊接口和好友接口共用应用级 Retrofit 配置，避免重复创建客户端。 */
    @Provides
    @Singleton
    fun provideGroupApiService(retrofit: Retrofit): GroupApiService {
        return retrofit.create(GroupApiService::class.java)
    }
}
