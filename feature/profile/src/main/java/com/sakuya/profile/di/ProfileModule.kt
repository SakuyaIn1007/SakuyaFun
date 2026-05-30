package com.sakuya.profile.di

import com.sakuya.profile.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//@Module
//@InstallIn(SingletonComponent::class)
//object ProfileModule{
//    @Provides
//    @Singleton
//    fun provideUserRepository(): UserRepository {
//        return UserRepository()
//    }
//}