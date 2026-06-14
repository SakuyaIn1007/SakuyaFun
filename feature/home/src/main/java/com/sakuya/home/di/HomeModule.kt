package com.sakuya.home.di

import com.sakuya.home.data.datasource.HomeDataSource
import com.sakuya.home.data.datasource.MockHomeDataSource
import com.sakuya.home.data.datasource.MockRankingDataSource
import com.sakuya.home.data.datasource.RankingDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    @Singleton
    abstract fun bindHomeDataSource(
        mockHomeDataSource: MockHomeDataSource
    ): HomeDataSource

    @Binds
    @Singleton
    abstract fun bindRankingDataSource(
        mockRankingDataSource: MockRankingDataSource
    ): RankingDataSource
}
