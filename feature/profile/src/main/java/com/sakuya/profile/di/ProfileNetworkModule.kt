package com.sakuya.profile.di

import com.sakuya.profile.data.remote.ProfileApiService
import com.sakuya.profile.data.remote.RelationshipApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileNetworkModule {
    @Provides
    @Singleton
    fun provideProfileApiService(retrofit: Retrofit): ProfileApiService {
        return retrofit.create(ProfileApiService::class.java)
    }

    /** 关注关系与个人资料共用应用级 Retrofit 配置。 */
    @Provides
    @Singleton
    fun provideRelationshipApiService(retrofit: Retrofit): RelationshipApiService {
        return retrofit.create(RelationshipApiService::class.java)
    }
}
