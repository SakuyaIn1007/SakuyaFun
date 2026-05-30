package com.sakuya.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.profile.ProfileAction
import com.sakuya.profile.model.UserProfile
import com.sakuya.profile.data.repository.UserRepository
import com.sakuya.profile.effect.ProfileEffect
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
            ProfileAction.OnMeClick -> emitEffect(ProfileEffect.GoMe)

            ProfileAction.OnConversationClick -> emitEffect(ProfileEffect.GoConversation)

            ProfileAction.OnFriendsClick -> emitEffect(ProfileEffect.GoFriends)
//
            ProfileAction.OnAlbumsClick -> emitEffect(ProfileEffect.GoAlbums)
//
            ProfileAction.OnSettingsClick -> emitEffect(ProfileEffect.GoSettings)

            ProfileAction.OnLogoutClick -> emitEffect(ProfileEffect.showToast("退出登录"))

            ProfileAction.OnWalletClick -> emitEffect(ProfileEffect.GoWallet)

            ProfileAction.OnFavouritesClick -> emitEffect(ProfileEffect.GoFavourites)

            ProfileAction.OnCardsClick -> emitEffect(ProfileEffect.GoCards)
            else -> Unit
        }
    }
    private fun emitEffect(effect: ProfileEffect) {
        viewModelScope.launch {
        _effect.emit(effect)
        }
    }

}
