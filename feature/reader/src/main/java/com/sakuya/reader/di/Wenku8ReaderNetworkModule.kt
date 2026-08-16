package com.sakuya.reader.di

import com.sakuya.reader.data.Wenku8ReaderApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object Wenku8ReaderNetworkModule { @Provides @Singleton fun api(retrofit: Retrofit): Wenku8ReaderApiService = retrofit.create(Wenku8ReaderApiService::class.java) }
