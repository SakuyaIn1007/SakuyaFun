package com.sakuya.notification

import com.sakuya.model.notification.NotificationType
import com.sakuya.navigation.*
import org.junit.Assert.assertEquals
import org.junit.Test

/** 锁定推送 Payload 只能进入预定义路由，缺失动态目标时必须回退通知中心。 */
class NotificationTargetRouteMapperTest {
    @Test fun mapsSupportedTargetsAndRejectsMissingFeedTarget(){
        assertEquals(feedAuthorRoute("user-1"),NotificationTargetRouteMapper.route(NotificationType.NEW_FOLLOWER,null,"user-1"))
        assertEquals(FRIEND_REQUESTS_ROUTE,NotificationTargetRouteMapper.route(NotificationType.FRIEND_REQUEST,"request-1","user-1"))
        assertEquals(FNOTICE_ROUTE,NotificationTargetRouteMapper.route(NotificationType.FEED_LIKE,"",null))
        assertEquals(
            "$WENKU8_READER_BASE_ROUTE?$READER_ARG_BOOK_ID=book-1&$READER_ARG_CHAPTER_ID=chapter-2&$READER_ARG_TITLE=%E7%AC%AC%E4%BA%8C%E7%AB%A0",
            NotificationTargetRouteMapper.route(NotificationType.CHAPTER_UPDATE,"book-1|chapter-2|第二章",null),
        )
    }
}
