package com.sakuya.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.data.notification.*
import com.sakuya.model.notification.AppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NotificationViewModel.kt
 * 职责说明：管理统一通知列表、刷新状态、已读动作与一次性错误。
 * 执行流程：订阅 Room -> 首次和前台推送触发远端同步 -> 点击先持久化已读再交给导航层分流。
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(private val repository:NotificationRepository):ViewModel(){
    val notifications=repository.notifications.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val unreadCount=repository.unreadCount.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),0)
    val hasMore=repository.hasMore
    private val _refreshing=MutableStateFlow(false);val refreshing=_refreshing.asStateFlow()
    private val _effect=MutableSharedFlow<String>();val effect=_effect.asSharedFlow()
    init { refresh();viewModelScope.launch{NotificationRefreshEvents.flow.collect{refresh()}} }
    fun refresh(){viewModelScope.launch{_refreshing.value=true;repository.refresh().onFailure{_effect.emit(it.message?:"通知刷新失败")};_refreshing.value=false}}
    fun open(item:AppNotification,onReady:(AppNotification)->Unit){viewModelScope.launch{if(item.readAt==null)repository.markRead(item.id);onReady(item)}}
    fun markAllRead(){viewModelScope.launch{repository.markAllRead().onFailure{_effect.emit(it.message?:"全部已读失败")}}}
    fun loadMore(){viewModelScope.launch{repository.loadMore().onFailure{_effect.emit(it.message?:"加载更多通知失败")}}}
}
