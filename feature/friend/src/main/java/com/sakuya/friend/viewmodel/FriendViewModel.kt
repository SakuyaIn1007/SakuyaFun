package com.sakuya.friend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.friend.data.repository.FriendRepository
import com.sakuya.friend.model.Friend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val repository: FriendRepository
) : ViewModel() {

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends = _friends.asStateFlow()

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
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "加载好友列表失败"))
                }
            _isLoading.value = false
        }
    }

    fun searchUsers(keyword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.searchUsers(keyword)
                .onSuccess { list ->
                    _friends.value = list
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "搜索失败"))
                }
            _isLoading.value = false
        }
    }

    fun addFriend(userId: String, message: String = "") {
        viewModelScope.launch {
            repository.sendFriendRequest(userId, message)
                .onSuccess {
                    _effect.emit(FriendEffect.ShowToast("好友请求已发送"))
                }
                .onFailure { error ->
                    _effect.emit(FriendEffect.ShowError(error.message ?: "发送请求失败"))
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
            is FriendAction.OnFriendClick -> emitEffect(
                FriendEffect.NavigateToChat(action.friend)
            )
            FriendAction.OnAddFriendClick -> emitEffect(FriendEffect.NavigateToAddFriend)
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
    data class NavigateToChat(val friend: Friend) : FriendEffect
    data object NavigateToAddFriend : FriendEffect
    data class ShowError(val message: String) : FriendEffect
    data class ShowToast(val message: String) : FriendEffect
}
