package com.sakuya.catalog.di

import com.sakuya.catalog.data.datasource.CatalogDataSource
import com.sakuya.catalog.data.datasource.RemoteCatalogDataSource
import com.sakuya.catalog.data.datasource.RemoteRankingDataSource
import com.sakuya.catalog.data.datasource.RankingDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogModule {

    @Binds
    @Singleton
    abstract fun bindCatalogDataSource(
        remoteCatalogDataSource: RemoteCatalogDataSource
    ): CatalogDataSource

    @Binds
    @Singleton
    abstract fun bindRankingDataSource(
        remoteRankingDataSource: RemoteRankingDataSource
    ): RankingDataSource
}
