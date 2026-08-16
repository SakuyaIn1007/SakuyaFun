package com.sakuya.profile.data.repository

import com.sakuya.model.network.BaseResponse
import com.sakuya.profile.data.remote.RelationshipApiService
import com.sakuya.profile.data.remote.RelationshipPageDto
import com.sakuya.profile.data.remote.RelationshipUserDto
import com.sakuya.profile.model.RelationshipListType
import com.sakuya.profile.model.RelationshipPage
import com.sakuya.profile.model.RelationshipUser
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RelationshipRepository.kt
 * 职责说明：集中处理关注/粉丝分页与关注关系修改，隔离 Retrofit 响应和 DTO。
 * 执行流程：RelationshipViewModel 请求列表或更新关系 -> 本类执行 API 与映射 -> 返回领域 Result。
 */
@Singleton
class RelationshipRepository @Inject constructor(private val apiService: RelationshipApiService) {
    suspend fun getUsers(type: RelationshipListType, page: Int, pageSize: Int): Result<RelationshipPage> =
        when (type) {
            RelationshipListType.FOLLOWING -> apiService.getFollowing(page, pageSize)
            RelationshipListType.FOLLOWERS -> apiService.getFollowers(page, pageSize)
        }.toDomain(RelationshipPageDto::toDomain)

    suspend fun setFollowing(userId: String, shouldFollow: Boolean): Result<RelationshipUser> =
        (if (shouldFollow) apiService.follow(userId) else apiService.unfollow(userId)).toDomain(RelationshipUserDto::toDomain)
}

private fun RelationshipPageDto.toDomain() = RelationshipPage(users.map(RelationshipUserDto::toDomain), nextPage)
private fun RelationshipUserDto.toDomain() = RelationshipUser(userId, name, initial, description, avatarColor, isFollowing)
private fun <T, R> Response<BaseResponse<T>>.toDomain(mapper: (T) -> R): Result<R> =
    if (!isSuccessful) Result.failure(IllegalStateException("HTTP ${code()}"))
    else body()?.toResult()?.map(mapper) ?: Result.failure(IllegalStateException("服务器未返回数据"))
