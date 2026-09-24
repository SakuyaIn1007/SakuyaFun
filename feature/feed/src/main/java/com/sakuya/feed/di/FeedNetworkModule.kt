package com.sakuya.feed.di

import com.sakuya.feed.data.remote.FeedApiService
import com.sakuya.feed.data.repository.FeedRepository
import com.sakuya.feed.data.repository.RemoteFeedRepository
import com.google.gson.Gson
import com.sakuya.data.feed.AuthorFeedSource
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
    // 三个绑定链式复用同一个 RemoteFeedRepository 实例。
    // 若 FeedRepository 与 AuthorFeedSource 各自 new 一个，会得到两份持有 FeedCacheDao 的实例，
    // 缓存写入路径将出现两个来源。
    @Provides @Singleton fun provideRemoteFeedRepository(
        api: FeedApiService,
        cacheDao: FeedCacheDao,
        gson: Gson
    ): RemoteFeedRepository = RemoteFeedRepository(api, cacheDao, gson)

    @Provides @Singleton fun provideFeedRepository(repo: RemoteFeedRepository): FeedRepository = repo

    @Provides @Singleton fun provideAuthorFeedSource(repo: RemoteFeedRepository): AuthorFeedSource = repo
}
