package com.sakuya.model.extentions

enum class Gender(
    val value: Int,
    val displayName: String
) {
    MALE(1, "男"),
    FEMALE(2, "女"),
    UNKNOWN(0, "未知")
}