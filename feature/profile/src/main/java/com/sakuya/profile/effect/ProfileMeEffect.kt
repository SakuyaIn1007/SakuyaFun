package com.sakuya.profile.effect

sealed class ProfileMeEffect {
    data object GoAvatar : ProfileMeEffect()
    data object GoName : ProfileMeEffect()
    data object GoGender : ProfileMeEffect()
    data object GoRegion : ProfileMeEffect()
    data object GoPhone : ProfileMeEffect()
    data object GoId : ProfileMeEffect()
    data object GoSignature : ProfileMeEffect()
    data object GoRingtone : ProfileMeEffect()
}