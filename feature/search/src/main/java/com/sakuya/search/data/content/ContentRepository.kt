package com.sakuya.search.data.content

import com.sakuya.data.content.ContentApiService
import com.sakuya.model.search.SearchPage
import com.sakuya.model.search.SearchQuery
import com.sakuya.model.search.SearchResult
import com.sakuya.search.data.local.SearchHistoryStorage
import com.sakuya.search.data.remote.SearchApiService
import com.sakuya.search.data.remote.SearchResultDto
import com.sakuya.search.data.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ContentRepository.kt
 * 职责说明：统一协调小说/动态/用户远程搜索、DataStore 历史与小说详情读取。
 * 执行流程：SearchViewModel -> SearchApiService 获取统一结果 -> SearchResult/UiState；
 * 小说详情仍经 ContentApiService 读取，不改变既有阅读链路。
 */
@Singleton
class ContentRepository @Inject constructor(
    private val contentApi: ContentApiService,
    private val searchApi: SearchApiService,
    private val historyStorage: SearchHistoryStorage,
) : SearchRepository {
    override suspend fun search(query: SearchQuery, page: Int, pageSize: Int): Result<SearchPage> = safeCall {
        searchApi.search(query.keyword.trim(), query.filter.contentType?.name?.lowercase(), page, pageSize).result { dto ->
            SearchPage(dto.items.map(SearchResultDto::toDomain), dto.nextPage)
        }
    }

    override suspend fun getHistory() = safeCall { Result.success(historyStorage.getHistory()) }
    override suspend fun getHotKeywords() = safeCall { searchApi.getHotKeywords().result { it } }
    override suspend fun saveHistory(keyword: String) = safeCall {
        historyStorage.save(keyword)
        Result.success(Unit)
    }
    override suspend fun clearHistory() = safeCall {
        historyStorage.clear()
        Result.success(Unit)
    }
    suspend fun novel(bookId: String) = safeCall { contentApi.novel(bookId).result { it } }
    suspend fun chapters(bookId: String) = safeCall { contentApi.chapters(bookId).result { it } }
}

/** I/O 失败进入 Result 供 ViewModel 渲染重试；协程取消继续向上传播，防止旧筛选请求回写页面。 */
private suspend fun <T> safeCall(block: suspend () -> Result<T>): Result<T> = try {
    block()
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    Result.failure(error)
}

private fun SearchResultDto.toDomain() = SearchResult(
    id = id,
    title = title,
    summary = summary,
    type = runCatching { com.sakuya.model.search.SearchContentType.valueOf(type.uppercase()) }
        .getOrDefault(com.sakuya.model.search.SearchContentType.NOVEL),
    tags = tags,
)

private fun <T, R> Response<com.sakuya.model.network.BaseResponse<T>>.result(mapper: (T) -> R): Result<R> =
    if (!isSuccessful) Result.failure(IllegalStateException(body()?.message ?: "HTTP ${code()}"))
    else body()?.toResult()?.map(mapper) ?: Result.failure(IllegalStateException("服务器未返回数据"))
