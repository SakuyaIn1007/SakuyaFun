package com.sakuya.profile.effect

sealed class ProfileEffect {
    data object GoMe : ProfileEffect()
    data object GoConversation : ProfileEffect()
    data object GoFriends : ProfileEffect()
    data object GoSettings : ProfileEffect()
    data object GoWallet : ProfileEffect()
    data object GoFavourites : ProfileEffect()
    data object GoCards : ProfileEffect()
    data object GoAlbums : ProfileEffect()
    data class showToast(val message: String) : ProfileEffect()
}
