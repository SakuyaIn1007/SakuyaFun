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
    val birthday: String?,
    val regionCode: String?,
    val phoneNumber: String?,
    val email: String?,
    val pokeText: String?,
    val ringtoneName: String?,
    val canBeAddedByStrangers: Boolean = true,
    val showProfileToStrangers: Boolean = true,
    val muteMessagesFromUnknown: Boolean = false
)
