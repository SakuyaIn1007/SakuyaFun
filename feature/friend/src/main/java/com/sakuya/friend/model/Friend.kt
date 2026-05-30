package com.sakuya.friend.model

data class Friend(
    val id: String,
    val name: String,
    val status: String,
    val avatarText: String,
    val isOnline: Boolean = false
)
