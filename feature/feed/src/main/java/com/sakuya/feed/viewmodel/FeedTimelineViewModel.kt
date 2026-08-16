package com.sakuya.feed.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakuya.feed.data.repository.FeedRepository
import com.sakuya.data.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.sakuya.model.feed.DynamicPost
import com.sakuya.model.feed.FeedStream
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job

/**
 * FeedTimelineViewModel.kt
 * 职责说明：管理动态时间线的分流、刷新、分页加载与错误事件。
 * 执行流程：UI Action -> Repository 请求 -> UiState 更新或 Effect 错误提示。
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
 * 职责说明：统一管理首页分流、Room 缓存、远端刷新与网络可用状态。
 * 执行流程：切换分流后先订阅该流缓存，再尝试远端同步；网络变化只更新离线展示状态，
 * 恢复网络不自动请求，避免页面后台反复刷新。
 */
@HiltViewModel class FeedTimelineViewModel @Inject constructor(
    private val repository: FeedRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedTimelineUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()
    private val _effect = MutableSharedFlow<FeedTimelineEffect>()
    val effect = _effect.asSharedFlow()
    private var nextPage = 0
    private var cacheObservationJob: Job? = null
    init {
        observeNetwork()
        observeCache(FeedStream.RECOMMENDED)
        onAction(FeedTimelineAction.Refresh)
    }
    fun onAction(action: FeedTimelineAction) {
        when (action) {
            is FeedTimelineAction.ChangeStream -> if (_uiState.value.stream != action.stream) {
                _uiState.value = FeedTimelineUiState(stream = action.stream, isLoading = true)
                observeCache(action.stream)
                onAction(FeedTimelineAction.Refresh)
            }
            FeedTimelineAction.Refresh -> load(0, true)
            FeedTimelineAction.LoadMore -> if (_uiState.value.canLoadMore && !_uiState.value.isLoadingMore) {
                load(nextPage, false)
            }
        }
    }
    private fun load(page: Int, refresh: Boolean) = viewModelScope.launch {
        if (!networkMonitor.isNetworkAvailable.value) {
            _uiState.value = _uiState.value.copy(
                isLoading = false, isRefreshing = false, isLoadingMore = false,
                isShowingCachedContent = _uiState.value.posts.isNotEmpty(),
                isOfflineWithoutCache = _uiState.value.posts.isEmpty(),
            )
            return@launch
        }
        _uiState.value = _uiState.value.copy(isLoading = refresh && _uiState.value.posts.isEmpty(), isRefreshing = refresh && _uiState.value.posts.isNotEmpty(), isLoadingMore = !refresh)
        repository.getFeed(_uiState.value.stream, page, PAGE_SIZE).onSuccess { result ->
            nextPage = result.nextPage ?: page
            _uiState.value = FeedTimelineUiState(
                stream = _uiState.value.stream,
                posts = _uiState.value.posts,
                canLoadMore = result.nextPage != null,
                isNetworkAvailable = true,
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isShowingCachedContent = _uiState.value.posts.isNotEmpty(),
                isOfflineWithoutCache = _uiState.value.posts.isEmpty() && !networkMonitor.isNetworkAvailable.value,
            )
            _effect.emit(FeedTimelineEffect.ShowError(error.message ?: "动态加载失败"))
        }
    }

    /** 分流切换时取消旧流收集，Room 后续写入会直接驱动当前页面的列表内容。 */
    private fun observeCache(stream: FeedStream) {
        cacheObservationJob?.cancel()
        cacheObservationJob = viewModelScope.launch {
            repository.observeCachedFeed(stream).collectLatest { posts ->
                if (_uiState.value.stream != stream) return@collectLatest
                _uiState.value = _uiState.value.copy(
                    posts = posts,
                    isLoading = _uiState.value.isLoading && posts.isEmpty(),
                    isShowingCachedContent = posts.isNotEmpty() && !_uiState.value.isNetworkAvailable,
                    isOfflineWithoutCache = posts.isEmpty() && !_uiState.value.isNetworkAvailable,
                )
            }
        }
    }

    private fun observeNetwork() = viewModelScope.launch {
        networkMonitor.isNetworkAvailable.collectLatest { available ->
            _uiState.value = _uiState.value.copy(
                isNetworkAvailable = available,
                isShowingCachedContent = !available && _uiState.value.posts.isNotEmpty(),
                isOfflineWithoutCache = !available && _uiState.value.posts.isEmpty(),
            )
        }
    }
    private companion object { const val PAGE_SIZE = 10 }
}
