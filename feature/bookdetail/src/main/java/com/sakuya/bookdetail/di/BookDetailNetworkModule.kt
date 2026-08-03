package com.sakuya.bookdetail.di

import com.sakuya.bookdetail.data.remote.BookDetailApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BookDetailNetworkModule {
    @Provides
    @Singleton
    fun provideBookDetailApiService(retrofit: Retrofit): BookDetailApiService =
        retrofit.create(BookDetailApiService::class.java)
}
