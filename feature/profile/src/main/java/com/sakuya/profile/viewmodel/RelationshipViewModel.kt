package com.sakuya.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.profile.data.repository.RelationshipRepository
import com.sakuya.profile.model.RelationshipListType
import com.sakuya.profile.model.RelationshipUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * RelationshipViewModel.kt
 * 职责说明：管理关注/粉丝分页列表和 Follow/Unfollow 操作状态。
 * 执行流程：列表类型或分页 Action -> Repository -> UiState；单个用户操作以 operatingUserId 防止重复提交。
 */
data class RelationshipUiState(
    val type: RelationshipListType = RelationshipListType.FOLLOWING,
    val users: List<RelationshipUser> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val operatingUserId: String? = null,
)

sealed interface RelationshipAction {
    data class Load(val type: RelationshipListType) : RelationshipAction
    data object LoadMore : RelationshipAction
    data class ToggleFollow(val user: RelationshipUser) : RelationshipAction
}

sealed interface RelationshipEffect { data class ShowError(val message: String) : RelationshipEffect }

@HiltViewModel
class RelationshipViewModel @Inject constructor(private val repository: RelationshipRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RelationshipUiState())
    val uiState = _uiState.asStateFlow()
    private val _effect = MutableSharedFlow<RelationshipEffect>()
    val effect = _effect.asSharedFlow()
    private var nextPage = 0

    fun onAction(action: RelationshipAction) {
        when (action) {
            is RelationshipAction.Load -> load(action.type, page = 0, refresh = true)
            RelationshipAction.LoadMore -> if (_uiState.value.canLoadMore && !_uiState.value.isLoadingMore) {
                load(_uiState.value.type, nextPage, refresh = false)
            }
            is RelationshipAction.ToggleFollow -> toggleFollow(action.user)
        }
    }

    /** 首次加载替换旧列表，加载更多只追加新页，保证切换关注/粉丝时不会残留上一类数据。 */
    private fun load(type: RelationshipListType, page: Int, refresh: Boolean) = viewModelScope.launch {
        _uiState.value = if (refresh) RelationshipUiState(type = type, isLoading = true) else _uiState.value.copy(isLoadingMore = true)
        repository.getUsers(type, page, PAGE_SIZE).onSuccess { result ->
            nextPage = result.nextPage ?: page
            _uiState.value = _uiState.value.copy(
                users = if (refresh) result.users else _uiState.value.users + result.users,
                isLoading = false,
                isLoadingMore = false,
                canLoadMore = result.nextPage != null,
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false)
            _effect.emit(RelationshipEffect.ShowError(error.message ?: "加载关系列表失败"))
        }
    }

    private fun toggleFollow(user: RelationshipUser) = viewModelScope.launch {
        if (_uiState.value.operatingUserId != null) return@launch
        _uiState.value = _uiState.value.copy(operatingUserId = user.userId)
        repository.setFollowing(user.userId, !user.isFollowing).onSuccess { updatedUser ->
            _uiState.value = _uiState.value.copy(users = _uiState.value.users.map { current ->
                if (current.userId == updatedUser.userId) updatedUser else current
            })
        }.onFailure { error -> _effect.emit(RelationshipEffect.ShowError(error.message ?: "更新关注状态失败")) }
        _uiState.value = _uiState.value.copy(operatingUserId = null)
    }

    private companion object { const val PAGE_SIZE = 20 }
}
