package com.sakuya.data.notification

import com.sakuya.data.local.dao.NotificationDao
import com.sakuya.data.local.entity.NotificationEntity
import com.sakuya.model.notification.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationRepository.kt
 * 职责说明：协调通知 REST 与 Room 缓存，并统一处理已读和推送偏好。
 * 执行流程：页面先观察 Room -> refresh 拉取服务端 -> 校验枚举并写缓存 -> Flow 自动刷新 UI。
 */
@Singleton
class NotificationRepository @Inject constructor(private val api: NotificationApiService,private val dao: NotificationDao,private val badges:UpdateBadgeRepository) {
    val notifications: Flow<List<AppNotification>> = dao.observeAll().map { values -> values.mapNotNull(NotificationEntity::toDomain) }
    val unreadCount: Flow<Int> = combine(dao.observeUnreadCount(), badges.notificationUnreadCount) { local, server -> server ?: local }
    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()
    private var nextPage: Int? = null

    suspend fun refresh(): Result<Unit> = runCatching {
        val response=api.list(0,50);val body=requireNotNull(response.body()){"服务器返回为空"};check(response.isSuccessful&&body.isSuccess()){body.message}
        val page=requireNotNull(body.data);dao.upsertAll(page.items.mapNotNull(NotificationDto::toEntity));nextPage=page.nextPage;_hasMore.value=nextPage!=null
        badges.refreshNotifications()
    }
    suspend fun loadMore():Result<Unit> = runCatching { val pageIndex=nextPage?:return@runCatching;val response=api.list(pageIndex,50);val body=requireNotNull(response.body());check(response.isSuccessful&&body.isSuccess()){body.message};val page=requireNotNull(body.data);dao.upsertAll(page.items.mapNotNull(NotificationDto::toEntity));nextPage=page.nextPage;_hasMore.value=nextPage!=null }
    suspend fun markRead(id:String):Result<Unit> = runCatching { val r=api.markRead(id);check(r.isSuccessful&&r.body()?.isSuccess()==true){r.body()?.message?:"标记已读失败"};dao.markRead(id,Instant.now().toString());badges.refreshNotifications().getOrThrow() }
    suspend fun markAllRead():Result<Unit> = runCatching { val r=api.markAllRead();check(r.isSuccessful&&r.body()?.isSuccess()==true){r.body()?.message?:"全部已读失败"};dao.markAllRead(Instant.now().toString());badges.refreshNotifications().getOrThrow() }
    suspend fun getPreferences():Result<NotificationPreferences> = runCatching { val r=api.preferences();val b=requireNotNull(r.body());check(r.isSuccessful&&b.isSuccess()){b.message};requireNotNull(b.data).toDomain() }
    suspend fun updatePreferences(value:NotificationPreferences):Result<NotificationPreferences> = runCatching { val r=api.updatePreferences(value.toDto());val b=requireNotNull(r.body());check(r.isSuccessful&&b.isSuccess()){b.message};requireNotNull(b.data).toDomain() }
}

private fun NotificationDto.toEntity(): NotificationEntity? { NotificationType.entries.firstOrNull{it.name==type}?:return null;NotificationTargetType.entries.firstOrNull{it.name==targetType}?:return null;return NotificationEntity(id,actorId,type,title,content,targetType,targetId,targetUserId,createdAt,readAt) }
private fun NotificationEntity.toDomain(): AppNotification? {
    val resolvedType = NotificationType.entries.firstOrNull { it.name == type } ?: return null
    val resolvedTarget = NotificationTargetType.entries.firstOrNull { it.name == targetType } ?: return null
    return AppNotification(id, actorId, resolvedType, title, content, resolvedTarget, targetId, targetUserId, createdAt, readAt)
}
private fun PreferenceDto.toDomain()=NotificationPreferences(pushEnabled,feedEnabled,friendEnabled)
private fun NotificationPreferences.toDto()=PreferenceDto(pushEnabled,feedEnabled,friendEnabled)
