package com.sakuya.conversation.navigation

const val CONVERSATION_ROUTE = "conversation"
const val CHAT_ROUTE = "chat"
const val CHAT_ID_ARG = "conversationId"
const val CHAT_ROUTE_PATTERN = "$CHAT_ROUTE/{$CHAT_ID_ARG}"

fun chatRoute(conversationId: String) = "$CHAT_ROUTE/$conversationId"
