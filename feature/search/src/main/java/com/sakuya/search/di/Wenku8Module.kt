package com.sakuya.search.di

import com.sakuya.search.data.repository.SearchRepository
import com.sakuya.search.data.content.ContentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
abstract class Wenku8BindingModule { @Binds @Singleton abstract fun bindSearchRepository(repository: ContentRepository): SearchRepository }

/** 统一搜索使用全局 Retrofit，鉴权、Base URL 和错误拦截均与其他业务保持一致。 */
@Module @InstallIn(SingletonComponent::class)
object SearchNetworkModule {
    @Provides @Singleton
    fun provideSearchApiService(retrofit: Retrofit): com.sakuya.search.data.remote.SearchApiService =
        retrofit.create(com.sakuya.search.data.remote.SearchApiService::class.java)
}
