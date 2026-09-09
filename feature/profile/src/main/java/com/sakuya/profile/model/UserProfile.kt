package com.sakuya.profile.model

import com.sakuya.model.extentions.Gender

data class UserProfile(
    val userId: String,
    var avatarUrl: String,
    var nickname: String,
    var signature: String? = "",
    var gender: Gender? = null,
    var birthday: String? = null,
    var regionCode: String? = "",
    var phoneNumber: String? = "",
    var email: String? = "",
    var pokeText: String? = "",
    var ringtoneName: String? = "",
    var privacySettings: PrivacySettings,
    val followingCount: Long = 0,
    val followerCount: Long = 0,
){
    companion object{
        fun empty() = UserProfile(
            userId = "0",
            avatarUrl = "",
            nickname = "新用户",
            privacySettings = PrivacySettings()
        )
    }
}

data class PrivacySettings(
    var canBeAddedByStrangers: Boolean = true,
    var showProfileToStrangers: Boolean = true,
    var muteMessagesFromUnknown: Boolean = false
)
