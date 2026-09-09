package com.sakuya.sakuyainandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.data.notification.UpdateBadgeRepository
import com.sakuya.model.notification.UpdateBadgeState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MainScreenViewModel.kt
 * 职责说明：为应用级底栏和顶级导航提供统一更新提示状态。
 * 执行流程：创建及应用回到前台时请求摘要；Room 会话变化通过仓库 Flow 自动更新，不轮询 UI。
 */
@HiltViewModel
class MainScreenViewModel @Inject constructor(private val badges:UpdateBadgeRepository):ViewModel(){
    val state=badges.state.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),UpdateBadgeState())
    init{refresh()}
    fun refresh(){viewModelScope.launch{badges.refresh()}}
}
