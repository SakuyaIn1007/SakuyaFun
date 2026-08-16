package com.sakuya.search.data.wenku8

import com.sakuya.model.search.SearchContentType
import com.sakuya.model.search.SearchPage
import com.sakuya.model.search.SearchQuery
import com.sakuya.model.search.SearchResult
import com.sakuya.search.data.repository.SearchRepository
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wenku8Repository.kt
 * 职责说明：将网关 DTO 转为搜索与分章阅读所需的稳定领域数据，并规范化网络失败。
 * 执行流程：ViewModel 发起搜索或详情读取 -> Repository 检查 BaseResponse -> 转换模型或返回可展示错误。
 */
@Singleton
class Wenku8Repository @Inject constructor(private val api: Wenku8ApiService) : SearchRepository {
    override suspend fun search(query: SearchQuery, page: Int, pageSize: Int): Result<SearchPage> {
        if (query.filter.contentType == SearchContentType.DYNAMIC) return Result.success(SearchPage(emptyList(), null))
        return api.search(query.keyword.trim(), page).result { dto ->
            SearchPage(dto.items.map { SearchResult(it.id, it.title, "${it.author} · ${it.description}", SearchContentType.NOVEL, it.tags) }, dto.nextPage)
        }
    }
    override suspend fun getHistory() = Result.success(emptyList<String>())
    override suspend fun getHotKeywords() = Result.success(listOf("魔法禁书目录", "狼与香辛料", "刀剑神域"))
    override suspend fun saveHistory(keyword: String) = Result.success(Unit)
    override suspend fun clearHistory() = Result.success(Unit)
    suspend fun novel(id: String) = api.novel(id).result { it }
    suspend fun chapters(id: String) = api.chapters(id).result { it }
    suspend fun content(novelId: String, chapterId: String) = api.content(chapterId, novelId).result { it }
}

private fun <T, R> Response<com.sakuya.model.network.BaseResponse<T>>.result(mapper: (T) -> R): Result<R> =
    if (!isSuccessful) Result.failure(IllegalStateException("HTTP ${code()}"))
    else body()?.toResult()?.map(mapper) ?: Result.failure(IllegalStateException("服务器未返回数据"))
