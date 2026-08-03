package com.sakuya.catalog.di

import com.sakuya.catalog.data.remote.CatalogApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CatalogNetworkModule {
    @Provides
    @Singleton
    fun provideCatalogApiService(retrofit: Retrofit): CatalogApiService =
        retrofit.create(CatalogApiService::class.java)
}
