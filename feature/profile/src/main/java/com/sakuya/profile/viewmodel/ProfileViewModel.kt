package com.sakuya.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.data.feed.AuthorFeedSource
import com.sakuya.model.feed.DynamicPost
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

/**
 * 个人页「我的动态」列表状态。
 * 说明：动态列表与资料分开持有，资料加载失败不应连带清空已经拿到的动态。
 */
data class ProfilePostsState(
    val posts: List<DynamicPost> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val updateBadges:UpdateBadgeRepository,
    private val feedRepository: AuthorFeedSource,
) : ViewModel() {
    val updateState=updateBadges.state.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),UpdateBadgeState())

    private val _userProfile = MutableStateFlow(UserProfile.Companion.empty())
    val userProfile: StateFlow<UserProfile> = _userProfile
    private val _postsState = MutableStateFlow(ProfilePostsState())
    val postsState: StateFlow<ProfilePostsState> = _postsState
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
                .onSuccess { profile ->
                    _userProfile.value = profile
                    // 动态列表以资料里的 userId 为查询键，因此必须等资料到手后再加载。
                    loadMyPosts(profile.userId)
                }
                .onFailure { error ->
                    _effect.emit(ProfileEffect.showToast(error.message ?: "个人资料加载失败"))
                }
        }
    }

    /**
     * 读取当前用户自己的动态，供个人页「动态」标签展示。
     * 执行流程：清空上次结果并进入加载态 -> 走动态模块的作者分页接口 -> 成功写入列表，失败保留错误供整页重试。
     * 说明：这里刻意复用 /feed/authors/{自己}，而不是另起接口；
     * 该接口只返回分页结果、不写首页 Room 流缓存，因此不会污染推荐流或关注流。
     */
    fun loadMyPosts(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _postsState.value = ProfilePostsState(isLoading = true)
            feedRepository.getAuthorFeed(userId, page = 0, pageSize = MY_POSTS_PAGE_SIZE)
                .onSuccess { result ->
                    _postsState.value = ProfilePostsState(posts = result.items)
                }
                .onFailure { error ->
                    _postsState.value = ProfilePostsState(errorMessage = error.message ?: "动态加载失败")
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

    private companion object { const val MY_POSTS_PAGE_SIZE = 20 }
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
