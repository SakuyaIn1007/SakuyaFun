package com.sakuya.di

import com.sakuya.data.local.TokenStorage
import com.sakuya.data.local.impl.TokenStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindTokenStorage(
        tokenStorageImpl: TokenStorageImpl
    ): TokenStorage
}