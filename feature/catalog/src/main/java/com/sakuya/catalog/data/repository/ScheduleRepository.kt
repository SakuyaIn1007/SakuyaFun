package com.sakuya.catalog.data.repository

import com.sakuya.catalog.model.ContentItem
import com.sakuya.catalog.model.NovelRelease
import com.sakuya.catalog.model.NovelReleaseDate
import com.sakuya.catalog.data.remote.CatalogApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ScheduleRepository.kt
 * 职责说明：集中维护已公布的轻小说更新资料，向时间表页提供完整且可排序的领域数据。
 * 执行流程：读取已知小说目录 -> 依照公布资料匹配小说 -> 返回全部有效更新；后续接入接口时只替换本类的数据来源。
 */
@Singleton
class ScheduleRepository @Inject constructor(
    private val apiService: CatalogApiService,
) {
    /**
     * 时间表仅显示后台人工维护的服务端资料；请求失败时返回失败，由 UI 显示重试，绝不回退到过期样例日期。
     */
    suspend fun getPublishedReleases(): Result<List<NovelRelease>> = try {
        val response = apiService.getPublishedReleases()
        val body = response.body()
        if (response.isSuccessful && body != null) body.toResult().map { releases -> releases.mapNotNull { it.toDomainOrNull() }.sortedBy(NovelRelease::releaseDate) }
        else Result.failure(IllegalStateException("时间表请求失败（HTTP ${response.code()}）"))
    } catch (error: Exception) { Result.failure(error) }
}

/** 服务端日期仅接受 yyyy-MM-dd，格式不合法的资料不会进入时间表。 */
private fun com.sakuya.catalog.data.remote.NovelReleaseDto.toDomainOrNull(): NovelRelease? {
    val parts = releaseDate.split("-")
    if (parts.size != 3) return null
    val date = runCatching { NovelReleaseDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt()) }.getOrNull() ?: return null
    return NovelRelease(novel, date, volumeName, isRecommended)
}
