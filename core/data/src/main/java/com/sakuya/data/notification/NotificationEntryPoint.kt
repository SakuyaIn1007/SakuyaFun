package com.sakuya.data.notification

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 为 Application 和 Activity 启动边界暴露通知基础设施，避免在框架类上执行 Kotlin 字段注入。 */
@EntryPoint @InstallIn(SingletonComponent::class)
interface NotificationEntryPoint {
    fun pushRegistrationManager(): PushRegistrationManager
    fun notificationRepository(): NotificationRepository
}
