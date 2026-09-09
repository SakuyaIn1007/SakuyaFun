package com.sakuya.model.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 验证一级入口组合规则，防止后续新增通知类型时把消息与我的红点错误绑定。 */
class UpdateBadgeStateTest {
    @Test fun messageCombinesConversationAndNotificationWhileProfileUsesOwnedUpdates(){
        val chat=UpdateBadgeState(conversationUnreadCount=1)
        assertTrue(chat.showMessage);assertFalse(chat.showProfile)

        val friend=UpdateBadgeState(notification=NotificationUnreadSummary(total=1,friendRelation=1))
        assertTrue(friend.showMessage);assertTrue(friend.showFriendRelation);assertFalse(friend.showProfile)

        val profile=UpdateBadgeState(notification=NotificationUnreadSummary(total=2,feedInteraction=1,newFollowers=1))
        assertTrue(profile.showMessage);assertTrue(profile.showProfile);assertTrue(profile.showProfileDynamic);assertTrue(profile.showFollowers)
    }

    @Test fun followedPostsLightProfileAndFollowingOnly(){
        val state=UpdateBadgeState(following=FollowingUnseenSummary(postCount=3,authorCount=2))
        assertTrue(state.showProfile);assertTrue(state.showFollowing);assertFalse(state.showMessage)
    }
}
