package com.sakuya.friend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.friend.data.repository.FriendRepository
import com.sakuya.friend.data.remote.FriendRequestItem
import com.sakuya.model.friend.Friend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sakuya.data.notification.UpdateBadgeRepository
import com.sakuya.model.notification.NotificationUnreadCategory

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val repository: FriendRepository,
    private val updateBadges:UpdateBadgeRepository,
) : ViewModel() {

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends = _friends.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Friend>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequestItem>>(emptyList())
    val friendRequests = _friendRequests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _effect = MutableSharedFlow<FriendEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadFriends()
    }

    fun loadFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getFriends()
                .onSuccess { list ->
                    _friends.value = list
                    updateBadges.markCategoryRead(NotificationUnreadCategory.FRIEND_RELATION)
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "加载好友列表失败"))
                }
            _isLoading.value = false
        }
    }

    fun searchUsers(keyword: String) {
        viewModelScope.launch {
            if (keyword.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            _isLoading.value = true
            repository.searchUsers(keyword)
                .onSuccess { list ->
                    _searchResults.value = list
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "搜索失败"))
                }
            _isLoading.value = false
        }
    }

    fun addFriend(userId: String, message: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            repository.sendFriendRequest(userId, message)
                .onSuccess {
                    _effect.emit(FriendEffect.ShowToast("好友请求已发送"))
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "发送请求失败"))
                }
            _isLoading.value = false
        }
    }

    fun loadFriendRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getFriendRequests()
                .onSuccess { requests ->
                    _friendRequests.value = requests
                    updateBadges.markCategoryRead(NotificationUnreadCategory.FRIEND_RELATION)
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "加载好友请求失败"))
                }
            _isLoading.value = false
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(requestId)
                .onSuccess { friend ->
                    _friends.value = (_friends.value + friend).distinctBy { it.id }
                    _friendRequests.value = _friendRequests.value.filterNot { it.id == requestId }
                    _effect.emit(FriendEffect.ShowToast("已添加好友"))
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "接受请求失败"))
                }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectFriendRequest(requestId)
                .onSuccess {
                    _friendRequests.value = _friendRequests.value.filterNot { it.id == requestId }
                    _effect.emit(FriendEffect.ShowToast("已拒绝请求"))
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "拒绝请求失败"))
                }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            repository.removeFriend(friendId)
                .onSuccess {
                    _friends.value = _friends.value.filter { it.id != friendId }
                    _effect.emit(FriendEffect.ShowToast("已删除好友"))
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "删除失败"))
                }
        }
    }

    fun onAction(action: FriendAction) {
        when (action) {
            is FriendAction.OnFriendClick -> openConversation(action.friend)
            is FriendAction.OnAddFriendClick -> emitEffect(FriendEffect.NavigateToAddFriend)
        }
    }

    private fun openConversation(friend: Friend) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getOrCreateConversation(friend.id)
                .onSuccess { conversation ->
                    _effect.emit(FriendEffect.NavigateToChat(conversation.id, conversation.title))
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "创建会话失败"))
                }
            _isLoading.value = false
        }
    }

    private fun emitEffect(effect: FriendEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}

sealed interface FriendAction {
    data class OnFriendClick(val friend: Friend) : FriendAction
    data object OnAddFriendClick : FriendAction
}

sealed interface FriendEffect {
    data class NavigateToChat(val conversationId: String, val title: String) : FriendEffect
    data object NavigateToAddFriend : FriendEffect
    data class ShowError(val message: String) : FriendEffect
    data class ShowToast(val message: String) : FriendEffect
}
