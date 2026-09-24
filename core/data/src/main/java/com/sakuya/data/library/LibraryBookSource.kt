package com.sakuya.data.library

import com.sakuya.data.catalog.CatalogBook
import com.sakuya.model.library.LibraryItem
import kotlinx.coroutines.flow.Flow

/**
 * 职责说明：书架能力的跨模块窄契约。bookdetail 只需要「这本书在不在书架、能否加入、
 * 触发一次同步」，不需要看到 LibraryRepository 的本地导入、多选删除、进度合并等实现细节。
 * 执行流程：bookshelf 的 LibraryRepository 实现本接口，由 LibraryModule 用 @Binds 绑定。
 * 错误处理：沿用实现的抛异常语义，调用方用 runCatching 包裹。
 */
interface LibraryBookSource {
    fun observeBooks(): Flow<List<LibraryItem>>
    suspend fun syncLibrary()
    suspend fun addCatalogBook(book: CatalogBook)
}
