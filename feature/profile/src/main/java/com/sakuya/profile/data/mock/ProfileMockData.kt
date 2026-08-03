package com.sakuya.profile.data.mock

import com.sakuya.model.extentions.Gender
import com.sakuya.profile.model.PrivacySettings
import com.sakuya.profile.model.UserProfile
import com.sakuya.profile.viewmodel.RegionItem

object ProfileMockData {
    val userProfile = UserProfile(
        userId = "sakuya_10001",
        avatarUrl = "https://i0.hdslb.com/bfs/archive/f225baea1791f6cc35addffab219515aee4639a7.jpg",
        nickname = "十六夜咲夜",
        signature = "时间会停下，事情还是要一件件做好。",
        gender = Gender.FEMALE,
        birthday = "2002-06-20",
        regionCode = "CN-ZJ-HZ",
        phoneNumber = "13800000000",
        email = "sakuya@example.com",
        pokeText = "的怀表轻轻响了一下",
        ringtoneName = "默认铃声",
        privacySettings = PrivacySettings(
            canBeAddedByStrangers = true,
            showProfileToStrangers = false,
            muteMessagesFromUnknown = true,
        )
    )

    val regions = listOf(
        RegionItem(code = "CN-BJ", name = "北京"),
        RegionItem(code = "CN-SH", name = "上海"),
        RegionItem(code = "CN-ZJ-HZ", name = "浙江 杭州"),
        RegionItem(code = "CN-GD-SZ", name = "广东 深圳"),
        RegionItem(code = "CN-SC-CD", name = "四川 成都"),
    )
}
