package com.sakuya.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.profile.model.UserProfile
import com.sakuya.profile.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow(UserProfile.Companion.empty())
    val userProfile: StateFlow<UserProfile> = _userProfile
    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val profile = userRepository.getUserProfile()
            _userProfile.value = profile
        }
    }

    fun updateProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            userRepository.updateUserProfile(updatedProfile)
            _userProfile.value = updatedProfile
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
//
            ProfileAction.OnMeClick -> emitEffect(ProfileEffect.NavigateToMe)

            ProfileAction.OnConversationClick -> emitEffect(ProfileEffect.NavigateToConversation)

            ProfileAction.OnFriendsClick -> emitEffect(ProfileEffect.NavigateToFriends)
//
            ProfileAction.OnAlbumsClick -> emitEffect(ProfileEffect.NavigateToAlbums)
//
            ProfileAction.OnSettingsClick -> emitEffect(ProfileEffect.NavigateToSettings)

            ProfileAction.OnLogoutClick -> emitEffect(ProfileEffect.showToast("退出登录"))

            ProfileAction.OnWalletClick -> emitEffect(ProfileEffect.NavigateToWallet)

            ProfileAction.OnFavouritesClick -> emitEffect(ProfileEffect.NavigateToFavourites)

            ProfileAction.OnCardsClick -> emitEffect(ProfileEffect.NavigateToCards)

            else -> Unit
        }
    }
    private fun emitEffect(effect: ProfileEffect) {
        viewModelScope.launch {
        _effect.emit(effect)
        }
    }

}
sealed interface ProfileEffect {
    data object NavigateToMe : ProfileEffect
    data object NavigateToConversation : ProfileEffect
    data object NavigateToFriends : ProfileEffect
    data object NavigateToSettings : ProfileEffect
    data object NavigateToWallet : ProfileEffect
    data object NavigateToFavourites : ProfileEffect
    data object NavigateToCards : ProfileEffect
    data object NavigateToAlbums : ProfileEffect
    data class showToast(val message: String) : ProfileEffect

}
sealed interface ProfileAction {

    data object SaveName : ProfileAction
    data object OnMeClick : ProfileAction
    data object OnConversationClick : ProfileAction
    data object OnFriendsClick : ProfileAction
    data object OnSettingsClick : ProfileAction
    data object OnWalletClick : ProfileAction
    data object OnCardsClick : ProfileAction
    data object OnFavouritesClick : ProfileAction
    data object OnLogoutClick : ProfileAction
    data object OnAlbumsClick : ProfileAction
    data object OnAvatarClick : ProfileAction
    data object OnNameClick : ProfileAction
    data object OnGenderClick : ProfileAction
    data object OnRegionClick : ProfileAction
    data object OnPhoneClick : ProfileAction
    data object OnIdClick : ProfileAction
    data object OnSignatureClick : ProfileAction
    data object OnRingtoneClick : ProfileAction
}

