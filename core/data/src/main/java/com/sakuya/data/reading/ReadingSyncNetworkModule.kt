package com.sakuya.data.reading

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReadingSyncNetworkModule {
    @Provides
    @Singleton
    fun provideReadingSyncApi(retrofit: Retrofit): ReadingSyncApiService = retrofit.create(ReadingSyncApiService::class.java)
}
