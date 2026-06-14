package com.sakuya.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sakuya.model.extentions.Gender

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: String,
    val nickname: String,
    val avatarUrl: String?,
    val signature: String?,
    val gender: Gender?,
    val phoneNumber: String?,
    val email: String?
)
