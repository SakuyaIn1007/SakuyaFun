package com.sakuya.data.notification

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** 在共享网络栈上创建通知 API，确保 Token、超时和错误处理与其他模块一致。 */
@Module @InstallIn(SingletonComponent::class)
object NotificationDataModule {
    @Provides @Singleton fun provideNotificationApi(retrofit: Retrofit): NotificationApiService = retrofit.create(NotificationApiService::class.java)
    @Provides @Singleton fun provideUpdateBadgeApi(retrofit: Retrofit): UpdateBadgeApiService = retrofit.create(UpdateBadgeApiService::class.java)
}
