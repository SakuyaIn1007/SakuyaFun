package com.sakuya.feed.di

import com.sakuya.feed.data.remote.FeedApiService
import com.sakuya.feed.data.repository.FeedRepository
import com.sakuya.feed.data.repository.RemoteFeedRepository
import com.google.gson.Gson
import com.sakuya.data.local.dao.FeedCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * FeedNetworkModule.kt
 * 职责说明：将 Retrofit 创建的动态接口服务注入动态模块的 ViewModel。
 * 执行流程：Hilt 创建 PublicAuthorViewModel 时提供 FeedApiService，页面因此能读取真实他人主页。
 */
@Module
@InstallIn(SingletonComponent::class)
object FeedNetworkModule {
    @Provides
    @Singleton
    fun provideFeedApiService(retrofit: Retrofit): FeedApiService = retrofit.create(FeedApiService::class.java)
    @Provides @Singleton fun provideFeedRepository(api: FeedApiService, cacheDao: FeedCacheDao, gson: Gson): FeedRepository =
        RemoteFeedRepository(api, cacheDao, gson)
}
