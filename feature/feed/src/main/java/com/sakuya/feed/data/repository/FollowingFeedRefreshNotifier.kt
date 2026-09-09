package com.sakuya.feed.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * FollowingFeedRefreshNotifier.kt
 * 职责说明：在 feed 功能内广播“关注关系已成功变更”，解除主页操作与时间线刷新之间的直接依赖。
 * 数据流：关注/取消关注接口成功 -> emitRelationshipChanged -> 关注流 ViewModel 收到事件并重新请求第 0 页。
 * 错误处理：失败的关注操作不得发出事件；缓冲区仅合并短时间内尚未消费的刷新提示，不保存用户数据。
 */
@Singleton
class FollowingFeedRefreshNotifier @Inject constructor() {
    private val _relationshipChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val relationshipChanges: SharedFlow<Unit> = _relationshipChanges.asSharedFlow()

    fun emitRelationshipChanged() {
        _relationshipChanges.tryEmit(Unit)
    }
}
