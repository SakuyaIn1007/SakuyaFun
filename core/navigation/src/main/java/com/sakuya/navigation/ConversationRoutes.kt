package com.sakuya.navigation

import android.net.Uri

const val CONVERSATION_ROUTE = "conversation"
const val CHAT_ROUTE = "chat"
const val CHAT_ID_ARG = "conversationId"
const val CHAT_FOCUS_MESSAGE_ID_ARG = "focusMessageId"
const val CHAT_DETAIL_ROUTE = "chat_detail"
const val CHAT_SEARCH_ROUTE = "chat_search"
const val CHAT_TITLE_ARG = "title"
/** 注册目的地和 popUpTo 必须共用完整 pattern，避免基础路由与带参数目的地无法匹配。 */
const val CHAT_ROUTE_PATTERN = "$CHAT_ROUTE/{$CHAT_ID_ARG}?$CHAT_TITLE_ARG={$CHAT_TITLE_ARG}&$CHAT_FOCUS_MESSAGE_ID_ARG={$CHAT_FOCUS_MESSAGE_ID_ARG}"
const val CHAT_DETAIL_ROUTE_PATTERN = "$CHAT_DETAIL_ROUTE/{$CHAT_ID_ARG}"
const val CHAT_SEARCH_ROUTE_PATTERN = "$CHAT_SEARCH_ROUTE/{$CHAT_ID_ARG}?$CHAT_TITLE_ARG={$CHAT_TITLE_ARG}"

fun chatRoute(conversationId: String, title: String = "", focusMessageId: String = "") =
    "$CHAT_ROUTE/${Uri.encode(conversationId)}?$CHAT_TITLE_ARG=${Uri.encode(title)}&$CHAT_FOCUS_MESSAGE_ID_ARG=${Uri.encode(focusMessageId)}"

fun chatDetailRoute(conversationId: String) = "$CHAT_DETAIL_ROUTE/${Uri.encode(conversationId)}"
fun chatSearchRoute(conversationId: String, title: String = "") = "$CHAT_SEARCH_ROUTE/${Uri.encode(conversationId)}?$CHAT_TITLE_ARG=${Uri.encode(title)}"
