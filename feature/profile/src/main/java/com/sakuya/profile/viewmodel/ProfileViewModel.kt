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
import com.sakuya.data.notification.UpdateBadgeRepository
import com.sakuya.model.notification.NotificationUnreadCategory
import com.sakuya.model.notification.UpdateBadgeState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val updateBadges:UpdateBadgeRepository,
) : ViewModel() {
    val updateState=updateBadges.state.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),UpdateBadgeState())

    private val _userProfile = MutableStateFlow(UserProfile.Companion.empty())
    val userProfile: StateFlow<UserProfile> = _userProfile
    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadUserProfile()
        viewModelScope.launch{updateBadges.refresh()}
    }

    /** 我的动态成功展示后只清除互动分类，新粉丝和关注动态提示继续保留。 */
    fun markDynamicUpdatesRead(){viewModelScope.launch{updateBadges.markCategoryRead(NotificationUnreadCategory.FEED_INTERACTION)}}

    fun loadUserProfile() {
        viewModelScope.launch {
            userRepository.getUserProfile()
                .onSuccess { profile -> _userProfile.value = profile }
                .onFailure { error ->
                    _effect.emit(ProfileEffect.showToast(error.message ?: "个人资料加载失败"))
                }
        }
    }

    fun updateProfile(updatedProfile: UserProfile, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            userRepository.updateUserProfile(updatedProfile)
                .onSuccess { savedProfile ->
                    _userProfile.value = savedProfile
                    onSuccess()
                }
                .onFailure { error ->
                    _effect.emit(ProfileEffect.showToast(error.message ?: "个人资料保存失败"))
                }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.OnMeClick -> emitEffect(ProfileEffect.NavigateToMe)

            is ProfileAction.OnConversationClick -> emitEffect(ProfileEffect.NavigateToConversation)

            is ProfileAction.OnFriendsClick -> emitEffect(ProfileEffect.NavigateToFriends)

            is ProfileAction.OnBookshelfClick -> emitEffect(ProfileEffect.NavigateToBookshelf)

            is ProfileAction.OnReadingHistoryClick -> emitEffect(ProfileEffect.NavigateToReadingHistory)

            is ProfileAction.OnAlbumsClick -> emitEffect(ProfileEffect.NavigateToAlbums)

            is ProfileAction.OnSettingsClick -> emitEffect(ProfileEffect.NavigateToSettings)

            is ProfileAction.OnPrivacyClick -> emitEffect(ProfileEffect.NavigateToPrivacy)

            is ProfileAction.OnLogoutClick -> emitEffect(ProfileEffect.showToast("退出登录"))


            is ProfileAction.OnFavouritesClick -> emitEffect(ProfileEffect.NavigateToFavourites)

            is ProfileAction.OnCardsClick -> emitEffect(ProfileEffect.NavigateToCards)

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
    data object NavigateToBookshelf : ProfileEffect
    data object NavigateToReadingHistory : ProfileEffect
    data object NavigateToSettings : ProfileEffect
    data object NavigateToPrivacy : ProfileEffect
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
    data object OnBookshelfClick : ProfileAction
    data object OnReadingHistoryClick : ProfileAction
    data object OnSettingsClick : ProfileAction
    data object OnPrivacyClick : ProfileAction
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
    data object OnPokeClick : ProfileAction
    data object OnSignatureClick : ProfileAction
    data object OnRingtoneClick : ProfileAction
}
