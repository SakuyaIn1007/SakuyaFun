package com.sakuya.navigation

import android.net.Uri

const val CONVERSATION_ROUTE = "conversation"
const val CHAT_ROUTE = "chat"
const val CHAT_ID_ARG = "conversationId"

fun chatRoute(conversationId: String, title: String = "") =
    "$CHAT_ROUTE/${Uri.encode(conversationId)}?title=${Uri.encode(title)}"
