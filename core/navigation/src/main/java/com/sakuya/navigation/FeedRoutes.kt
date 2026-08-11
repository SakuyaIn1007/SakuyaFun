package com.sakuya.navigation

import android.net.Uri

const val FEED_ROUTE = "feed"
const val FEED_DETAIL_POST_ID_ARG = "postId"
const val FEED_DETAIL_ROUTE = "$FEED_ROUTE/{$FEED_DETAIL_POST_ID_ARG}"
const val FEED_COMPOSE_ROUTE = "feed_compose"
const val FEED_USER_ID_ARG = "userId"
const val FEED_AUTHOR_ROUTE = "$FEED_ROUTE/author/{$FEED_USER_ID_ARG}"

/**
 * 职责说明：集中构建动态详情页路由，避免各入口重复拼接路径。
 * 执行流程：调用方传入原始动态 ID -> 此处编码路径参数 -> NavController 匹配详情页导航目标。
 */
fun feedDetailRoute(postId: String) = "$FEED_ROUTE/${Uri.encode(postId)}"
fun feedAuthorRoute(userId: String) = "$FEED_ROUTE/author/$userId"
