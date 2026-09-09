package com.sakuya.notification

import com.sakuya.model.notification.NotificationType
import com.sakuya.navigation.*

/** 把受控通知枚举与目标 ID 映射为现有应用路由；缺少目标时安全回退通知中心。 */
object NotificationTargetRouteMapper {
    fun route(type:NotificationType,targetId:String?,targetUserId:String?):String=when(type){
        NotificationType.FEED_COMMENT,NotificationType.FEED_REPLY,NotificationType.FEED_LIKE,NotificationType.FEED_FAVORITE->targetId?.takeIf{it.isNotBlank()}?.let(::feedDetailRoute)?:FNOTICE_ROUTE
        NotificationType.NEW_FOLLOWER,NotificationType.FRIEND_ACCEPTED->targetUserId?.takeIf{it.isNotBlank()}?.let(::feedAuthorRoute)?:FNOTICE_ROUTE
        NotificationType.FRIEND_REQUEST->FRIEND_REQUESTS_ROUTE
        NotificationType.CHAPTER_UPDATE->targetId?.split('|',limit=3)?.takeIf{it.size==3}?.let{parts->"$WENKU8_READER_BASE_ROUTE?$READER_ARG_BOOK_ID=${parts[0].routeEncode()}&$READER_ARG_CHAPTER_ID=${parts[1].routeEncode()}&$READER_ARG_TITLE=${parts[2].routeEncode()}"}?:FNOTICE_ROUTE
    }
}

private fun String.routeEncode():String=java.net.URLEncoder.encode(this,Charsets.UTF_8.name()).replace("+","%20")
