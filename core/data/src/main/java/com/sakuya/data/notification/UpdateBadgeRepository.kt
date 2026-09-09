package com.sakuya.data.notification

import com.sakuya.data.local.dao.ConversationDao
import com.sakuya.data.local.dao.NotificationDao
import com.sakuya.model.notification.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*

/** 仅保存内存态，不依赖 Retrofit；SessionManager 可安全调用 reset 而不会形成网络依赖环。 */
@Singleton
class UpdateBadgeStateStore @Inject constructor(){
    internal val notificationSummary=MutableStateFlow<NotificationUnreadSummary?>(null)
    internal val followingSummary=MutableStateFlow(FollowingUnseenSummary())
    fun reset(){notificationSummary.value=null;followingSummary.value=FollowingUnseenSummary()}
}

/**
 * UpdateBadgeRepository.kt
 * 职责说明：组合服务端通知分类、关注动态游标和 Room 会话未读数，为全局入口提供单一状态源。
 * 执行流程：登录/前台/页面刷新触发远端同步 -> StateFlow 更新 -> 底部导航与各功能入口同时重组。
 * 失败处理：同步失败保留上一次可信状态；已读接口失败不清本地红点，避免跨设备状态分叉。
 */
@Singleton
class UpdateBadgeRepository @Inject constructor(
    private val notificationApi: NotificationApiService,
    private val updateApi: UpdateBadgeApiService,
    private val notificationDao: NotificationDao,
    conversationDao: ConversationDao,
    private val store:UpdateBadgeStateStore,
) {
    val notificationUnreadCount:Flow<Int?> = store.notificationSummary.map { it?.total }

    val state: Flow<UpdateBadgeState> = combine(
        store.notificationSummary,
        store.followingSummary,
        conversationDao.observeTotalUnreadCount(),
    ) { notification, following, conversationUnread ->
        UpdateBadgeState(notification?:NotificationUnreadSummary(),following,conversationUnread)
    }

    suspend fun refresh(): Result<Unit> = runCatching {
        val notifications=refreshNotifications()
        val following=refreshFollowing()
        notifications.getOrThrow()
        following.getOrThrow()
    }

    suspend fun refreshNotifications(): Result<Unit> = runCatching {
        val response=notificationApi.unreadSummary();val body=requireNotNull(response.body()){ "服务器未返回通知摘要" }
        check(response.isSuccessful&&body.isSuccess()){body.message}
        val value=requireNotNull(body.data)
        store.notificationSummary.value=NotificationUnreadSummary(value.total,value.feedInteraction,value.newFollowers,value.friendRelation)
    }

    suspend fun refreshFollowing(): Result<Unit> = runCatching {
        val response=updateApi.followingSummary();val body=requireNotNull(response.body()){ "服务器未返回关注动态摘要" }
        check(response.isSuccessful&&body.isSuccess()){body.message}
        val value=requireNotNull(body.data)
        store.followingSummary.value=FollowingUnseenSummary(value.postCount,value.authorCount)
    }

    suspend fun markCategoryRead(category: NotificationUnreadCategory): Result<Unit> = runCatching {
        val response=notificationApi.markCategoryRead(ReadCategoryRequest(category.name));val body=response.body()
        check(response.isSuccessful&&body?.isSuccess()==true){body?.message?:"标记分类已读失败"}
        notificationDao.markTypesRead(category.types().map { it.name },Instant.now().toString())
        refreshNotifications().getOrThrow()
    }

    suspend fun markAllFollowingRead(): Result<Unit> = runCatching {
        val response=updateApi.markFollowingRead();check(response.isSuccessful&&response.body()?.isSuccess()==true){response.body()?.message?:"标记关注动态已读失败"}
        store.followingSummary.value=FollowingUnseenSummary()
    }

    suspend fun markAuthorRead(authorId:String): Result<Unit> = runCatching {
        val response=updateApi.markAuthorRead(authorId);check(response.isSuccessful&&response.body()?.isSuccess()==true){response.body()?.message?:"标记作者动态已读失败"}
        refreshFollowing().getOrThrow()
    }

    fun reset(){store.reset()}
}

private fun NotificationUnreadCategory.types():Set<NotificationType> = when(this){
    NotificationUnreadCategory.FEED_INTERACTION -> setOf(NotificationType.FEED_COMMENT,NotificationType.FEED_REPLY,NotificationType.FEED_LIKE,NotificationType.FEED_FAVORITE)
    NotificationUnreadCategory.NEW_FOLLOWER -> setOf(NotificationType.NEW_FOLLOWER)
    NotificationUnreadCategory.FRIEND_RELATION -> setOf(NotificationType.FRIEND_REQUEST,NotificationType.FRIEND_ACCEPTED)
}
