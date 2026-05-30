package com.sakuya.profile

sealed class ProfileAction {

    data object SaveName : ProfileAction()
    data object OnMeClick : ProfileAction()
    data object OnConversationClick : ProfileAction()
    data object OnFriendsClick : ProfileAction()
    data object OnSettingsClick : ProfileAction()
    data object OnWalletClick : ProfileAction()
    data object OnCardsClick : ProfileAction()
    data object OnFavouritesClick : ProfileAction()
    data object OnLogoutClick : ProfileAction()
    data object OnAlbumsClick : ProfileAction()
    data object OnAvatarClick : ProfileAction()
    data object OnNameClick : ProfileAction()
    data object OnGenderClick : ProfileAction()
    data object OnRegionClick : ProfileAction()
    data object OnPhoneClick : ProfileAction()
    data object OnIdClick : ProfileAction()
    data object OnSignatureClick : ProfileAction()
    data object OnRingtoneClick : ProfileAction()
}
