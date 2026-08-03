package com.sakuya.library.di

import com.sakuya.library.data.remote.LibraryApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LibraryNetworkModule {
    @Provides
    @Singleton
    fun provideLibraryApiService(retrofit: Retrofit): LibraryApiService =
        retrofit.create(LibraryApiService::class.java)
}
