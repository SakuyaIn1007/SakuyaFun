package com.sakuya.search.di

import com.sakuya.search.data.repository.SearchRepository
import com.sakuya.search.data.wenku8.Wenku8ApiService
import com.sakuya.search.data.wenku8.Wenku8Repository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
abstract class Wenku8BindingModule { @Binds @Singleton abstract fun bindSearchRepository(repository: Wenku8Repository): SearchRepository }
@Module @InstallIn(SingletonComponent::class)
object Wenku8NetworkModule { @Provides @Singleton fun provideWenku8Api(retrofit: Retrofit): Wenku8ApiService = retrofit.create(Wenku8ApiService::class.java) }
