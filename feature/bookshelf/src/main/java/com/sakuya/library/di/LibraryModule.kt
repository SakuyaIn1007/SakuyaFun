package com.sakuya.library.di

import com.sakuya.data.library.LibraryBookSource
import com.sakuya.library.data.repository.LibraryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 职责说明：把 LibraryRepository 绑定到 core:data 中的窄契约 LibraryBookSource，
 * 供 bookdetail 等书架子树之外的模块注入。
 * 执行流程：Hilt 需要 LibraryBookSource 时解析到 @Singleton 的 LibraryRepository。
 * 说明：必须装在 SingletonComponent —— LibraryRepository 是 @Singleton，@Singleton 绑定
 * 无法安装在 ViewModelComponent 中。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {
    @Binds
    @Singleton
    abstract fun bindLibraryBookSource(impl: LibraryRepository): LibraryBookSource
}
