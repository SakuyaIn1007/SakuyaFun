package com.sakuya.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.data.notification.UpdateBadgeRepository
import com.sakuya.model.notification.UpdateBadgeState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** 首页只订阅统一提示仓库，关注流成功加载后的清除动作由 FeedTimelineViewModel 负责。 */
@HiltViewModel
class DashboardViewModel @Inject constructor(badges:UpdateBadgeRepository):ViewModel(){
    val updateState=badges.state.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),UpdateBadgeState())
}
