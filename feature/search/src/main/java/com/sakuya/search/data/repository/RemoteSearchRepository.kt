package com.sakuya.search.data.repository

import com.sakuya.model.network.BaseResponse
import com.sakuya.model.search.SearchContentType
import com.sakuya.model.search.SearchPage
import com.sakuya.model.search.SearchQuery
import com.sakuya.model.search.SearchResult
import com.sakuya.search.data.remote.SearchApiService
import com.sakuya.search.data.remote.SearchPageDto
import com.sakuya.search.data.remote.SearchResultDto
import retrofit2.Response

/**
 * RemoteSearchRepository.kt
 * 职责说明：负责调用搜索 API、标准化网络错误，以及将 DTO 映射为领域搜索结果。
 * 执行流程：API Response -> BaseResponse 校验 -> SearchPage/SearchResult -> ViewModel UiState。
 */
class RemoteSearchRepository(private val apiService: SearchApiService) : SearchRepository {
    override suspend fun search(query: SearchQuery, page: Int, pageSize: Int): Result<SearchPage> =
        apiService.search(query.keyword, query.filter.contentType?.name?.lowercase(), page, pageSize)
            .toDomain { pageDto -> SearchPage(pageDto.items.map(SearchResultDto::toDomain), pageDto.nextPage) }

    /** 旧实现仅保留编译兼容；当前 Hilt 绑定的 ContentRepository 使用 DataStore 管理历史。 */
    override suspend fun getHistory(): Result<List<String>> = Result.success(emptyList())
    override suspend fun getHotKeywords(): Result<List<String>> = apiService.getHotKeywords().toDomain()

    /** 搜索历史由后端按用户维度维护时，可在 API 增加 POST/DELETE 后替换这两个实现。 */
    override suspend fun saveHistory(keyword: String): Result<Unit> = Result.success(Unit)
    override suspend fun clearHistory(): Result<Unit> = Result.success(Unit)
}

private fun SearchResultDto.toDomain() = SearchResult(
    id = id, title = title, summary = summary,
    type = runCatching { SearchContentType.valueOf(type.uppercase()) }.getOrDefault(SearchContentType.NOVEL),
    tags = tags,
)

private fun <T, R> Response<BaseResponse<T>>.toDomain(mapper: (T) -> R): Result<R> =
    if (!isSuccessful) Result.failure(IllegalStateException("HTTP ${code()}"))
    else body()?.toResult()?.map(mapper) ?: Result.failure(IllegalStateException("服务器未返回数据"))

private fun <T> Response<BaseResponse<T>>.toDomain(): Result<T> =
    if (!isSuccessful) Result.failure(IllegalStateException("HTTP ${code()}"))
    else body()?.toResult() ?: Result.failure(IllegalStateException("服务器未返回数据"))
