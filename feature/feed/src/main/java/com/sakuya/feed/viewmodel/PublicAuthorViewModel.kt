package com.sakuya.feed.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.feed.data.remote.FeedApiService
import com.sakuya.feed.data.remote.PublicProfileDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PublicAuthorViewModel.kt
 * 职责说明：加载他人公开主页，并处理主页底部的关注/取消关注操作。
 * 执行流程：页面进入时 load(userId) 请求 /profiles/{userId}；点击关注时请求服务端并仅回写 isFollowing。
 * 说明：请求失败保留已显示的数据，页面不会退回旧的静态“月见草”内容。
 */
data class PublicAuthorUiState(
    val profile: PublicProfileDto? = null,
    val isLoading: Boolean = true,
    val isOperatingFollow: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class PublicAuthorViewModel @Inject constructor(private val api: FeedApiService) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicAuthorUiState())
    val uiState = _uiState.asStateFlow()

    fun load(userId: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val response = runCatching { api.getPublicProfile(userId) }.getOrElse {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "主页加载失败")
            return@launch
        }
        val profile = response.body()?.data
        if (response.isSuccessful && response.body()?.code == 200 && profile != null) {
            _uiState.value = _uiState.value.copy(profile = profile, isLoading = false)
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = response.body()?.message ?: "主页加载失败")
        }
    }

    fun toggleFollow() {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isOperatingFollow) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperatingFollow = true, errorMessage = null)
            val response = runCatching {
                if (profile.isFollowing) api.unfollowUser(profile.userId) else api.followUser(profile.userId)
            }.getOrElse {
                _uiState.value = _uiState.value.copy(isOperatingFollow = false, errorMessage = "关注状态更新失败")
                return@launch
            }
            val following = response.body()?.data?.isFollowing
            if (response.isSuccessful && response.body()?.code == 200 && following != null) {
                _uiState.value = _uiState.value.copy(profile = profile.copy(isFollowing = following), isOperatingFollow = false)
            } else {
                _uiState.value = _uiState.value.copy(isOperatingFollow = false, errorMessage = response.body()?.message ?: "关注状态更新失败")
            }
        }
    }
}

/**
 * PublicRelationshipViewModel.kt
 * 职责说明：读取某个他人主页的关注或粉丝列表。
 * 执行流程：列表页按 type 请求对应 /profiles/{userId} 子路径；403 等业务错误直接保留给 UI 展示。
 */
data class PublicRelationshipUiState(val users: List<com.sakuya.feed.data.remote.RelationshipUserDto> = emptyList(), val isLoading: Boolean = true, val errorMessage: String? = null)

@HiltViewModel
class PublicRelationshipViewModel @Inject constructor(private val api: FeedApiService) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicRelationshipUiState())
    val uiState = _uiState.asStateFlow()
    fun load(userId: String, followers: Boolean) = viewModelScope.launch {
        _uiState.value = PublicRelationshipUiState()
        val response = runCatching { if (followers) api.getFollowerUsers(userId, 0, 20) else api.getFollowingUsers(userId, 0, 20) }.getOrElse {
            _uiState.value = PublicRelationshipUiState(isLoading = false, errorMessage = "列表加载失败"); return@launch
        }
        val body = response.body()
        val data = body?.data
        _uiState.value = if (response.isSuccessful && body?.code == 200 && data != null) PublicRelationshipUiState(users = data.users, isLoading = false)
        else PublicRelationshipUiState(isLoading = false, errorMessage = body?.message ?: "列表加载失败")
    }
}
