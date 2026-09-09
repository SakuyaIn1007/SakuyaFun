package com.sakuya.data.content

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** 在应用级 Retrofit 上创建唯一 ContentApiService，避免 Feature 重复声明同一后端协议。 */
@Module
@InstallIn(SingletonComponent::class)
object ContentNetworkModule {
    @Provides
    @Singleton
    fun provideContentApiService(retrofit: Retrofit): ContentApiService = retrofit.create(ContentApiService::class.java)
}
