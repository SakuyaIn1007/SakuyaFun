package com.sakuya.feed.viewmodel

import androidx.lifecycle.viewModelScope
import com.sakuya.common.mvi.BaseMviViewModel
import com.sakuya.data.network.NetworkMonitor
import com.sakuya.data.notification.UpdateBadgeRepository
import com.sakuya.feed.data.repository.FeedRepository
import com.sakuya.feed.data.repository.FollowingFeedRefreshNotifier
import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.feed.FeedStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * FeedTimelineViewModel.kt
 * 职责说明：管理动态时间线的分流、刷新、分页加载与错误事件。
 * 执行流程：UI Action -> Repository 请求 -> UiState 更新或 Effect 错误提示。
 * 架构说明：MVI 风格，基于 BaseMviViewModel；分流切换后先订阅该流缓存，再尝试远端同步。
 */
/**
 * 首页动态状态：posts 始终来自 Room；离线时 isShowingCachedContent 说明内容不是最新，
 * isOfflineWithoutCache 则要求 UI 显示无网络页而非空白列表。
 */
data class FeedTimelineUiState(
    val stream: FeedStream = FeedStream.RECOMMENDED,
    val posts: List<DynamicPost> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val isShowingCachedContent: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val isOfflineWithoutCache: Boolean = false,
)
sealed interface FeedTimelineAction { data class ChangeStream(val stream: FeedStream) : FeedTimelineAction; data object Refresh : FeedTimelineAction; data object LoadMore : FeedTimelineAction }
sealed interface FeedTimelineEffect { data class ShowError(val message: String) : FeedTimelineEffect }

/**
 * FeedTimelineViewModel.kt
 * 职责说明：统一管理首页推荐/关注分流、Room 缓存、远端分页刷新与网络可用状态。
 * 执行流程：切换分流后先订阅该流缓存，再尝试远端同步；网络变化只更新离线展示状态，
 * 恢复网络不自动请求；关注关系成功变更时，仅当前关注流自动重拉第 0 页。
 * 错误处理：远端失败不清空 Room 内容，有缓存时继续展示并发出错误 Effect，无缓存离线时显示离线状态。
 */
@HiltViewModel class FeedTimelineViewModel @Inject constructor(
    private val repository: FeedRepository,
    private val networkMonitor: NetworkMonitor,
    private val followingFeedRefreshNotifier: FollowingFeedRefreshNotifier,
    private val updateBadges: UpdateBadgeRepository,
) : BaseMviViewModel<FeedTimelineUiState, FeedTimelineAction, FeedTimelineEffect>(
    initialState = FeedTimelineUiState(isLoading = true),
) {
    private var nextPage = 0
    private var cacheObservationJob: Job? = null
    init {
        observeNetwork()
        observeFollowingRelationshipChanges()
        observeCache(FeedStream.RECOMMENDED)
        onAction(FeedTimelineAction.Refresh)
    }
    override fun onAction(action: FeedTimelineAction) {
        when (action) {
            is FeedTimelineAction.ChangeStream -> if (currentState.stream != action.stream) {
                updateState { FeedTimelineUiState(stream = action.stream, isLoading = true) }
                observeCache(action.stream)
                onAction(FeedTimelineAction.Refresh)
            }
            FeedTimelineAction.Refresh -> load(0, true)
            FeedTimelineAction.LoadMore -> if (currentState.canLoadMore && !currentState.isLoadingMore) {
                load(nextPage, false)
            }
        }
    }
    private fun load(page: Int, refresh: Boolean) = viewModelScope.launch {
        if (!networkMonitor.isNetworkAvailable.value) {
            updateState { state ->
                state.copy(
                    isLoading = false, isRefreshing = false, isLoadingMore = false,
                    isShowingCachedContent = state.posts.isNotEmpty(),
                    isOfflineWithoutCache = state.posts.isEmpty(),
                )
            }
            return@launch
        }
        updateState { state ->
            state.copy(
                isLoading = refresh && state.posts.isEmpty(),
                isRefreshing = refresh && state.posts.isNotEmpty(),
                isLoadingMore = !refresh,
            )
        }
        repository.getFeed(currentState.stream, page, PAGE_SIZE).onSuccess { result ->
            nextPage = result.nextPage ?: page
            updateState { state ->
                FeedTimelineUiState(
                    stream = state.stream,
                    posts = state.posts,
                    canLoadMore = result.nextPage != null,
                    isNetworkAvailable = true,
                )
            }
            // 只有关注流第一页成功同步后才推进游标；离线缓存或失败请求不会误清除红点。
            if (page == 0 && currentState.stream == FeedStream.FOLLOWING) updateBadges.markAllFollowingRead()
        }.onFailure { error ->
            updateState { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isShowingCachedContent = state.posts.isNotEmpty(),
                    isOfflineWithoutCache = state.posts.isEmpty() && !networkMonitor.isNetworkAvailable.value,
                )
            }
            sendEffect(FeedTimelineEffect.ShowError(error.message ?: "动态加载失败"))
        }
    }

    /** 分流切换时取消旧流收集，Room 后续写入会直接驱动当前页面的列表内容。 */
    private fun observeCache(stream: FeedStream) {
        cacheObservationJob?.cancel()
        cacheObservationJob = viewModelScope.launch {
            repository.observeCachedFeed(stream).collectLatest { posts ->
                if (currentState.stream != stream) return@collectLatest
                updateState { state ->
                    state.copy(
                        posts = posts,
                        isLoading = state.isLoading && posts.isEmpty(),
                        isShowingCachedContent = posts.isNotEmpty() && !state.isNetworkAvailable,
                        isOfflineWithoutCache = posts.isEmpty() && !state.isNetworkAvailable,
                    )
                }
            }
        }
    }

    private fun observeNetwork() = viewModelScope.launch {
        networkMonitor.isNetworkAvailable.collectLatest { available ->
            updateState { state ->
                state.copy(
                    isNetworkAvailable = available,
                    isShowingCachedContent = !available && state.posts.isNotEmpty(),
                    isOfflineWithoutCache = !available && state.posts.isEmpty(),
                )
            }
        }
    }

    /** 关系变化只会使关注流失效；推荐流仍保持原有请求节奏和排序结果。 */
    private fun observeFollowingRelationshipChanges() = viewModelScope.launch {
        followingFeedRefreshNotifier.relationshipChanges.collect {
            if (currentState.stream == FeedStream.FOLLOWING) {
                load(page = 0, refresh = true)
            }
        }
    }
    private companion object { const val PAGE_SIZE = 10 }
}
