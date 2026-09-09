package com.sakuya.model.notification

/**
 * AppNotification.kt
 * 职责说明：定义通知中心、系统推送与导航共同识别的受控通知类型。
 * 安全约束：未知服务端类型映射为 null 并只允许打开通知中心，不把任意字符串当作应用路由。
 */
enum class NotificationType { FEED_COMMENT, FEED_REPLY, FEED_LIKE, FEED_FAVORITE, NEW_FOLLOWER, FRIEND_REQUEST, FRIEND_ACCEPTED, CHAPTER_UPDATE }
enum class NotificationTargetType { FEED_POST, USER, FRIEND_REQUEST, BOOK_CHAPTER }

data class AppNotification(
    val id: String,
    val actorId: String,
    val type: NotificationType,
    val title: String,
    val content: String,
    val targetType: NotificationTargetType,
    val targetId: String,
    val targetUserId: String?,
    val createdAt: String,
    val readAt: String?,
)

data class NotificationPreferences(
    val pushEnabled: Boolean = true,
    val feedEnabled: Boolean = true,
    val friendEnabled: Boolean = true,
)

/** 客户端只允许按固定分类清除通知，避免 UI 传入任意服务端类型集合。 */
enum class NotificationUnreadCategory { FEED_INTERACTION, NEW_FOLLOWER, FRIEND_RELATION }

data class NotificationUnreadSummary(
    val total: Int = 0,
    val feedInteraction: Int = 0,
    val newFollowers: Int = 0,
    val friendRelation: Int = 0,
)

data class FollowingUnseenSummary(val postCount: Int = 0,val authorCount: Int = 0)

/**
 * UpdateBadgeState.kt
 * 职责说明：集中表达各一级入口是否显示无数字小红点，UI 不自行拼接业务类型。
 * 组合规则：消息=聊天或任一通知；我的=动态互动、新粉丝或关注用户新动态。
 */
data class UpdateBadgeState(
    val notification: NotificationUnreadSummary = NotificationUnreadSummary(),
    val following: FollowingUnseenSummary = FollowingUnseenSummary(),
    val conversationUnreadCount: Int = 0,
) {
    val showMessage: Boolean get() = conversationUnreadCount > 0 || notification.total > 0
    val showProfile: Boolean get() = notification.feedInteraction > 0 || notification.newFollowers > 0 || following.postCount > 0
    val showProfileDynamic: Boolean get() = notification.feedInteraction > 0
    val showFollowers: Boolean get() = notification.newFollowers > 0
    val showFollowing: Boolean get() = following.postCount > 0
    val showFriendRelation: Boolean get() = notification.friendRelation > 0
}
